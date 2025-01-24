package com.omgodse.notally.activities

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityNotallyBinding
import com.omgodse.notally.databinding.DialogProgressBinding
import com.omgodse.notally.databinding.DialogReminderBinding
import com.omgodse.notally.image.ImageError
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.recyclerview.adapter.AudioAdapter
import com.omgodse.notally.recyclerview.adapter.ErrorAdapter
import com.omgodse.notally.recyclerview.adapter.PreviewImageAdapter
import com.omgodse.notally.room.Audio
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Frequency
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.Reminder
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.NotallyModel
import com.omgodse.notally.widget.WidgetProvider
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

abstract class NotallyActivity(private val type: Type) : AppCompatActivity() {

    internal lateinit var binding: ActivityNotallyBinding
    internal val model: NotallyModel by viewModels()

    override fun finish() {
        lifecycleScope.launch {
            model.saveNote()
            WidgetProvider.sendBroadcast(application, model.id)
            super.finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("id", model.id)
        lifecycleScope.launch {
            model.saveNote()
            WidgetProvider.sendBroadcast(application, model.id)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.type = type
        initialiseBinding()
        setContentView(binding.root)

        lifecycleScope.launch {
            if (model.isFirstInstance) {
                val persistedId = savedInstanceState?.getLong("id")
                val selectedId = intent.getLongExtra(Constants.SelectedBaseNote, 0L)
                val id = persistedId ?: selectedId
                model.setState(id)

                if (model.isNewNote && intent.action == Intent.ACTION_SEND) {
                    handleSharedNote()
                }

                model.isFirstInstance = false
            }

            setupToolbar()
            setupListeners()
            setStateFromModel()

            configureUI()
            binding.ScrollView.visibility = View.VISIBLE
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_IMAGES -> {
                    val uri = data?.data
                    val clipData = data?.clipData
                    if (uri != null) {
                        val uris = arrayOf(uri)
                        model.addImages(uris)
                    } else if (clipData != null) {
                        val uris = Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
                        model.addImages(uris)
                    }
                }
                REQUEST_VIEW_IMAGES -> {
                    val list = data?.getParcelableArrayListExtra<Image>(ViewImage.DELETED_IMAGES)
                    if (!list.isNullOrEmpty()) {
                        model.deleteImages(list)
                    }
                }
                REQUEST_SELECT_LABELS -> {
                    val list = data?.getStringArrayListExtra(SelectLabels.SELECTED_LABELS)
                    if (list != null && list != model.labels) {
                        model.setLabels(list)
                        Operations.bindLabels(binding.LabelGroup, model.labels, model.textSize)
                    }
                }
                REQUEST_RECORD_AUDIO -> model.addAudio()
                REQUEST_PLAY_AUDIO -> {
                    val audio = data?.getParcelableExtra<Audio>(PlayAudio.AUDIO)
                    if (audio != null) {
                        model.deleteAudio(audio)
                    }
                }
            }
        }
        // Bug in Samsung: Even if permission was granted result code is RESULT_CANCELED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (requestCode == REQUEST_ALARM_PERMISSION) {
                val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (manager.canScheduleExactAlarms()) {
                    displayReminderDialog()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAlarmPermission()
                }
            }
            REQUEST_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recordAudio()
                } else handleRejection()
            }
        }
    }


    abstract fun configureUI()

    open fun setupListeners() {
        binding.EnterTitle.doAfterTextChanged { text ->
            model.title = requireNotNull(text).trim().toString()
        }
    }

    open fun setStateFromModel() {
        val formatter = DateFormat.getDateInstance(DateFormat.FULL)
        binding.DateCreated.text = formatter.format(model.timestamp)

        binding.EnterTitle.setText(model.title)
        Operations.bindLabels(binding.LabelGroup, model.labels, model.textSize)

        setColor()
    }


    private fun handleSharedNote() {
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val string = intent.getStringExtra(Intent.EXTRA_TEXT)
        val charSequence = intent.getCharSequenceExtra(Operations.extraCharSequence)
        val body = charSequence ?: string

        if (body != null) {
            model.body = Editable.Factory.getInstance().newEditable(body)
        }
        if (title != null) {
            model.title = title
        }
    }


    @RequiresApi(24)
    private fun checkAudioPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.please_grant_notally_audio)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.continue_) { _, _ ->
                        requestPermissions(arrayOf(permission), REQUEST_AUDIO_PERMISSION)
                    }
                    .show()
            } else requestPermissions(arrayOf(permission), REQUEST_AUDIO_PERMISSION)
        } else recordAudio()
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (manager.canScheduleExactAlarms()) {
                displayReminderDialog()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.please_grant_notally_alarm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.continue_) { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:$packageName")
                        startActivityForResult(intent, REQUEST_ALARM_PERMISSION)
                    }
                    .show()
            }
        } else displayReminderDialog()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.please_grant_notally_notification)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.continue_) { _, _ ->
                            requestPermissions(arrayOf(permission), REQUEST_NOTIFICATION_PERMISSION)
                        }
                        .show()
                } else requestPermissions(arrayOf(permission), REQUEST_NOTIFICATION_PERMISSION)
            } else checkAlarmPermission()
        } else checkAlarmPermission()
    }


    private fun recordAudio() {
        if (model.audioRoot != null) {
            val intent = Intent(this, RecordAudio::class.java)
            startActivityForResult(intent, REQUEST_RECORD_AUDIO)
        } else Toast.makeText(this, R.string.insert_an_sd_card_audio, Toast.LENGTH_LONG).show()
    }

    private fun handleRejection() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.to_record_audio)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            }
            .show()
    }

    private fun selectImages() {
        if (model.imageRoot != null) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, REQUEST_ADD_IMAGES)
        } else Toast.makeText(this, R.string.insert_an_sd_card_images, Toast.LENGTH_LONG).show()
    }


    private fun share() {
        val body = when (type) {
            Type.NOTE -> model.body
            Type.LIST -> Operations.getBody(model.items)
        }
        Operations.shareNote(this, model.title, body)
    }

    private fun label() {
        val intent = Intent(this, SelectLabels::class.java)
        intent.putStringArrayListExtra(SelectLabels.SELECTED_LABELS, model.labels)
        startActivityForResult(intent, REQUEST_SELECT_LABELS)
    }

    private fun delete() {
        model.folder = Folder.DELETED
        finish()
    }

    private fun restore() {
        model.folder = Folder.NOTES
        finish()
    }

    private fun archive() {
        model.folder = Folder.ARCHIVED
        finish()
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    model.deleteBaseNote()
                    super.finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pin(item: MenuItem) {
        model.pinned = !model.pinned
        bindPinned(item)
    }


    private fun setupImages() {
        val adapter = PreviewImageAdapter(model.imageRoot) { position ->
            val intent = Intent(this, ViewImage::class.java)
            intent.putExtra(ViewImage.POSITION, position)
            intent.putExtra(Constants.SelectedBaseNote, model.id)
            startActivityForResult(intent, REQUEST_VIEW_IMAGES)
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.ImagePreview.scrollToPosition(positionStart)
            }
        })

        binding.ImagePreview.setHasFixedSize(true)
        binding.ImagePreview.adapter = adapter
        binding.ImagePreview.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(binding.ImagePreview)

        model.images.observe(this) { list ->
            adapter.submitList(list)
            binding.ImagePreview.isVisible = list.isNotEmpty()
        }

        val dialogBinding = DialogProgressBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adding_images)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        model.addingImages.observe(this) { progress ->
            if (progress.inProgress) {
                dialog.show()
                dialogBinding.ProgressBar.max = progress.total
                dialogBinding.ProgressBar.setProgressCompat(progress.current, true)
                dialogBinding.Count.text = getString(R.string.count, progress.current, progress.total)
            } else dialog.dismiss()
        }

        model.eventBus.observe(this) { event ->
            event.handle { errors -> displayImageErrors(errors) }
        }
    }

    private fun displayImageErrors(errors: List<ImageError>) {
        val recyclerView = RecyclerView(this)
        recyclerView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        recyclerView.adapter = ErrorAdapter(errors)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recyclerView.scrollIndicators = View.SCROLL_INDICATOR_TOP or View.SCROLL_INDICATOR_BOTTOM
        }

        val title = resources.getQuantityString(R.plurals.cant_add_images, errors.size, errors.size)
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(recyclerView)
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun setupAudios() {
        val adapter = AudioAdapter { position: Int ->
            if (position != -1) {
                val audio = model.audios.value[position]
                val intent = Intent(this, PlayAudio::class.java)
                intent.putExtra(PlayAudio.AUDIO, audio)
                startActivityForResult(intent, REQUEST_PLAY_AUDIO)
            }
        }
        binding.AudioRecyclerView.adapter = adapter

        model.audios.observe(this) { list ->
            adapter.submitList(list)
            binding.AudioHeader.isVisible = list.isNotEmpty()
            binding.AudioRecyclerView.isVisible = list.isNotEmpty()
        }
    }


    private fun setupReminder() {
        val padding = (resources.displayMetrics.density * 16).toInt()

        model.reminder.observe(this) { reminder ->
            if (reminder != null) {
                val dateFormat = Operations.getReminderDateFormat(reminder.timestamp)
                val date = DateUtils.formatDateTime(binding.root.context, reminder.timestamp, dateFormat)
                binding.Reminder.text = when (reminder.frequency) {
                    Frequency.ONCE -> date
                    Frequency.DAILY -> getString(R.string.repeats_daily, date)
                    Frequency.MONTHLY -> getString(R.string.repeats_monthly, date)
                }
                binding.Reminder.visibility = View.VISIBLE
                binding.DateCreated.updatePadding(bottom = 0)
            } else {
                binding.Reminder.visibility = View.GONE
                binding.DateCreated.updatePadding(bottom = padding)
            }
        }

        binding.Reminder.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setItems(R.array.reminder) { _, which ->
                    when (which) {
                        0 -> displayReminderDialog()
                        1 -> model.deleteReminder()
                    }
                }
                .show()
        }
    }

    private fun displayReminderDialog() {
        val dialogBinding = DialogReminderBinding.inflate(layoutInflater)

        var selectedYear = -1
        var selectedMonth = -1
        var selectedDay = -1
        var selectedHour = -1
        var selectedMinute = -1

        val dateFormatter = DateFormat.getDateInstance(DateFormat.FULL)
        val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)

        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(selectedYear, selectedMonth, selectedDay)
            dialogBinding.Date.text = dateFormatter.format(calendar.timeInMillis)
        }
        val timeListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            dialogBinding.Time.text = timeFormatter.format(calendar.timeInMillis)
        }
        dialogBinding.Date.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val dialog = DatePickerDialog(this, dateListener, year, month, day)
            dialog.datePicker.minDate = calendar.timeInMillis
            dialog.show()
        }
        dialogBinding.Time.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, timeListener, hour, minute, false).show()
        }

        var selectedFrequency = 0
        val labels = resources.getStringArray(R.array.frequencies)
        dialogBinding.Frequency.text = labels[selectedFrequency]
        dialogBinding.Frequency.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_frequency)
                .setSingleChoiceItems(labels, selectedFrequency) { dialog, which ->
                    selectedFrequency = which
                    dialogBinding.Frequency.text = labels[selectedFrequency]
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.set_reminder)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                if (selectedYear != -1 && selectedHour != -1) {
                    val frequency = Frequency.entries[selectedFrequency]
                    val calendar = Calendar.getInstance()
                    calendar.clear()
                    calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                    val reminder = Reminder(calendar.timeInMillis, frequency)
                    model.setReminder(reminder)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun setColor() {
        val color = Operations.extractColor(model.color, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color
        }
        binding.root.setBackgroundColor(color)
        binding.RecyclerView.setBackgroundColor(color)
        binding.Toolbar.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun setupToolbar() {
        binding.Toolbar.setNavigationOnClickListener { finish() }

        val menu = binding.Toolbar.menu
        val pin = menu.add(R.string.pin, R.drawable.pin) { item -> pin(item) }
        bindPinned(pin)

        menu.add(R.string.share, R.drawable.share) { share() }
        menu.add(R.string.labels, R.drawable.label) { label() }
        menu.add(R.string.add_images, R.drawable.add_images) { selectImages() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            menu.add(R.string.record_audio, R.drawable.record_audio) { checkAudioPermission() }
        }

        menu.add(R.string.set_reminder, R.drawable.reminder) { checkNotificationPermission() }

        when (model.folder) {
            Folder.NOTES -> {
                menu.add(R.string.delete, R.drawable.delete) { delete() }
                menu.add(R.string.archive, R.drawable.archive) { archive() }
            }
            Folder.DELETED -> {
                menu.add(R.string.restore, R.drawable.restore) { restore() }
                menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever() }
            }
            Folder.ARCHIVED -> {
                menu.add(R.string.delete, R.drawable.delete) { delete() }
                menu.add(R.string.unarchive, R.drawable.unarchive) { restore() }
            }
        }
    }

    private fun initialiseBinding() {
        binding = ActivityNotallyBinding.inflate(layoutInflater)
        when (type) {
            Type.NOTE -> {
                binding.AddItem.visibility = View.GONE
                binding.RecyclerView.visibility = View.GONE
            }
            Type.LIST -> {
                binding.EnterBody.visibility = View.GONE
            }
        }

        val title = TextSize.getEditTitleSize(model.textSize)
        val date = TextSize.getDisplayBodySize(model.textSize)
        val body = TextSize.getEditBodySize(model.textSize)

        binding.EnterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, title)
        binding.DateCreated.setTextSize(TypedValue.COMPLEX_UNIT_SP, date)
        binding.Reminder.setTextSize(TypedValue.COMPLEX_UNIT_SP, date)
        binding.EnterBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        setupImages()
        setupAudios()
        setupReminder()

        binding.root.isSaveFromParentEnabled = false
    }

    private fun bindPinned(item: MenuItem) {
        val icon: Int
        val title: Int
        if (model.pinned) {
            icon = R.drawable.unpin
            title = R.string.unpin
        } else {
            icon = R.drawable.pin
            title = R.string.pin
        }
        item.setTitle(title)
        item.setIcon(icon)
    }

    companion object {
        private const val REQUEST_ADD_IMAGES = 30
        private const val REQUEST_VIEW_IMAGES = 31
        private const val REQUEST_NOTIFICATION_PERMISSION = 32
        private const val REQUEST_SELECT_LABELS = 33
        private const val REQUEST_RECORD_AUDIO = 34
        private const val REQUEST_PLAY_AUDIO = 35
        private const val REQUEST_AUDIO_PERMISSION = 36
        private const val REQUEST_ALARM_PERMISSION = 37
    }
}
package com.omgodse.notally.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PostPDFGenerator
import android.transition.TransitionManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.core.widget.doAfterTextChanged
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialFade
import com.omgodse.notally.MenuDialog
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMainBinding
import com.omgodse.notally.databinding.DialogColorBinding
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.adapter.ColorAdapter
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var configuration: AppBarConfiguration

    private val model: BaseNoteModel by viewModels()

    override fun onBackPressed() {
        if (model.actionMode.enabled.value) {
            model.actionMode.close(true)
        } else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.Toolbar)
        setupFAB()
        setupMenu()
        setupActionMode()
        setupNavigation()
        setupSearch()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXPORT_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                model.writeCurrentFileToUri(uri)
            }
        }
    }


    private fun setupFAB() {
        binding.TakeNote.setOnClickListener {
            val intent = Intent(this, TakeNote::class.java)
            startActivity(intent)
        }
        binding.MakeList.setOnClickListener {
            val intent = Intent(this, MakeList::class.java)
            startActivity(intent)
        }
    }

    private fun setupMenu() {
        val menu = binding.NavigationView.menu
        menu.add(0, R.id.Notes, 0, R.string.notes).setCheckable(true).setIcon(R.drawable.home)
        menu.add(1, R.id.Labels, 0, R.string.labels).setCheckable(true).setIcon(R.drawable.label)
        menu.add(2, R.id.Deleted, 0, R.string.deleted).setCheckable(true).setIcon(R.drawable.delete)
        menu.add(2, R.id.Archived, 0, R.string.archived).setCheckable(true).setIcon(R.drawable.archive)
        menu.add(3, R.id.Settings, 0, R.string.settings).setCheckable(true).setIcon(R.drawable.settings)
    }


    private fun setupActionMode() {
        binding.ActionMode.setNavigationOnClickListener { model.actionMode.close(true) }

        val transition = MaterialFade()
        transition.secondaryAnimatorProvider = null

        transition.excludeTarget(binding.NavHostFragment, true)
        transition.excludeChildren(binding.NavHostFragment, true)
        transition.excludeTarget(binding.TakeNote, true)
        transition.excludeTarget(binding.MakeList, true)
        transition.excludeTarget(binding.NavigationView, true)

        model.actionMode.enabled.observe(this) { enabled ->
            TransitionManager.beginDelayedTransition(binding.RelativeLayout, transition)
            if (enabled) {
                binding.Toolbar.visibility = View.GONE
                binding.ActionMode.visibility = View.VISIBLE
                binding.DrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                binding.Toolbar.visibility = View.VISIBLE
                binding.ActionMode.visibility = View.GONE
                binding.DrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED)
            }
        }

        val menu = binding.ActionMode.menu
        val pinned = menu.add(R.string.pin, R.drawable.pin) {}
        val share = menu.add(R.string.share, R.drawable.share) { share() }
        val labels = menu.add(R.string.labels, R.drawable.label) { label() }

        val export = createExportMenu(menu)

        val changeColor = menu.add(R.string.change_color, R.drawable.change_color) { changeColor() }
        val copy = menu.add(R.string.make_a_copy, R.drawable.copy) { model.copyBaseNote() }
        val delete = menu.add(R.string.delete, R.drawable.delete) { model.moveBaseNotes(Folder.DELETED) }
        val archive = menu.add(R.string.archive, R.drawable.archive) { model.moveBaseNotes(Folder.ARCHIVED) }
        val restore = menu.add(R.string.restore, R.drawable.restore) { model.moveBaseNotes(Folder.NOTES) }
        val unarchive = menu.add(R.string.unarchive, R.drawable.unarchive) { model.moveBaseNotes(Folder.NOTES) }
        val deleteForever = menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever() }

        model.actionMode.count.observe(this) { count ->
            if (count == 0) {
                menu.forEach { item -> item.setVisible(false) }
            } else {
                binding.ActionMode.title = count.toString()

                val baseNote = model.actionMode.getFirstNote()
                if (count == 1) {
                    if (baseNote.pinned) {
                        pinned.setTitle(R.string.unpin)
                        pinned.setIcon(R.drawable.unpin)
                    } else {
                        pinned.setTitle(R.string.pin)
                        pinned.setIcon(R.drawable.pin)
                    }
                    pinned.onClick { model.pinBaseNote(!baseNote.pinned) }
                }

                pinned.setVisible(count == 1)
                share.setVisible(count == 1)
                labels.setVisible(count == 1)
                export.setVisible(count == 1)
                changeColor.setVisible(true)
                copy.setVisible(true)

                val folder = baseNote.folder
                delete.setVisible(folder == Folder.NOTES || folder == Folder.ARCHIVED)
                archive.setVisible(folder == Folder.NOTES)
                restore.setVisible(folder == Folder.DELETED)
                unarchive.setVisible(folder == Folder.ARCHIVED)
                deleteForever.setVisible(folder == Folder.DELETED)
            }
        }
    }

    private fun createExportMenu(menu: Menu): MenuItem {
        val export = menu.addSubMenu(R.string.export)
        export.setIcon(R.drawable.export)
        export.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        export.add("PDF").onClick { exportToPDF() }
        export.add("TXT").onClick { exportToTXT() }
        export.add("JSON").onClick { exportToJSON() }
        export.add("HTML").onClick { exportToHTML() }

        return export.item
    }

    fun MenuItem.onClick(function: () -> Unit) {
        setOnMenuItemClickListener {
            function()
            return@setOnMenuItemClickListener false
        }
    }


    private fun share() {
        val baseNote = model.actionMode.getFirstNote()
        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body.applySpans(baseNote.spans)
            Type.LIST -> Operations.getBody(baseNote.items)
        }
        Operations.shareNote(this, baseNote.title, body)
    }

    private fun changeColor() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.change_color)
            .create()

        val colorAdapter = ColorAdapter(object : ItemListener {
            override fun onClick(position: Int) {
                dialog.dismiss()
                val color = Color.entries[position]
                model.colorBaseNote(color)
            }

            override fun onLongClick(position: Int) {}
        })

        val dialogBinding = DialogColorBinding.inflate(layoutInflater)
        dialogBinding.RecyclerView.adapter = colorAdapter

        dialog.setView(dialogBinding.root)
        dialog.show()
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_selected_notes)
            .setPositiveButton(R.string.delete) { _, _ -> model.deleteBaseNotes() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun label() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val labels = model.getAllLabels()
            if (labels.isNotEmpty()) {
                displaySelectLabelsDialog(labels, baseNote)
            } else {
                model.actionMode.close(true)
                navigateWithAnimation(R.id.Labels)
            }
        }
    }

    private fun displaySelectLabelsDialog(labels: Array<String>, baseNote: BaseNote) {
        val checkedPositions = BooleanArray(labels.size) { index -> baseNote.labels.contains(labels[index]) }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.labels)
            .setNegativeButton(R.string.cancel, null)
            .setMultiChoiceItems(labels, checkedPositions) { _, which, isChecked -> checkedPositions[which] = isChecked }
            .setPositiveButton(R.string.save) { _, _ ->
                val new = ArrayList<String>()
                checkedPositions.forEachIndexed { index, checked ->
                    if (checked) {
                        val label = labels[index]
                        new.add(label)
                    }
                }
                model.updateBaseNoteLabels(new, baseNote.id)
            }
            .show()
    }


    private fun exportToPDF() {
        val baseNote = model.actionMode.getFirstNote()
        model.getPDFFile(baseNote, object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: CharSequence?) {
                Toast.makeText(this@MainActivity, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportToTXT() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getTXTFile(baseNote)
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun exportToJSON() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getJSONFile(baseNote)
            showFileOptionsDialog(file, "application/json")
        }
    }

    private fun exportToHTML() {
        val baseNote = model.actionMode.getFirstNote()
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote)
            showFileOptionsDialog(file, "text/html")
        }
    }


    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

        MenuDialog(this)
            .add(R.string.share) { shareFile(uri, mimeType) }
            .add(R.string.view_file) { viewFile(uri, mimeType) }
            .add(R.string.save_to_device) { saveFileToDevice(file, mimeType) }
            .show()
    }

    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val chooser = Intent.createChooser(intent, getString(R.string.view_note))
        startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        val chooser = Intent.createChooser(intent, null)
        startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)

        model.currentFile = file
        startActivityForResult(intent, REQUEST_EXPORT_FILE)
    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.NavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        configuration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, configuration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerClosed(drawerView: View) {
                if (fragmentIdToLoad != null && navController.currentDestination?.id != fragmentIdToLoad) {
                    navigateWithAnimation(requireNotNull(fragmentIdToLoad))
                }
            }
        })

        navController.addOnDestinationChangedListener { _, destination, _ ->
            fragmentIdToLoad = destination.id
            binding.NavigationView.setCheckedItem(destination.id)
            handleDestinationChange(destination)
        }
    }

    private fun handleDestinationChange(destination: NavDestination) {
        if (destination.id == R.id.Notes) {
            binding.TakeNote.show()
            binding.MakeList.show()
        } else {
            binding.TakeNote.hide()
            binding.MakeList.hide()
        }

        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        if (destination.id == R.id.Search) {
            binding.EnterSearchKeyword.visibility = View.VISIBLE
            binding.EnterSearchKeyword.requestFocus()
            inputManager.showSoftInput(binding.EnterSearchKeyword, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.EnterSearchKeyword.visibility = View.GONE
            inputManager.hideSoftInputFromWindow(binding.EnterSearchKeyword.windowToken, 0)
        }
    }

    private fun navigateWithAnimation(id: Int) {
        val options = navOptions {
            launchSingleTop = true
            anim {
                exit = androidx.navigation.ui.R.anim.nav_default_exit_anim
                enter = androidx.navigation.ui.R.anim.nav_default_enter_anim
                popExit = androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
                popEnter = androidx.navigation.ui.R.anim.nav_default_pop_enter_anim
            }
            popUpTo(navController.graph.startDestination) { inclusive = false }
        }
        navController.navigate(id, null, options)
    }


    private fun setupSearch() {
        binding.EnterSearchKeyword.setText(model.keyword)
        binding.EnterSearchKeyword.doAfterTextChanged { text ->
            model.keyword = requireNotNull(text).trim().toString()
        }
    }

    companion object {
        private const val REQUEST_EXPORT_FILE = 10
    }
}
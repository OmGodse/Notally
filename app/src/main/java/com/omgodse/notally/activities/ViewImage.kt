package com.omgodse.notally.activities

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityViewImageBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.recyclerview.adapter.ImageAdapter
import com.omgodse.notally.room.Converters
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.NotallyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ViewImage : AppCompatActivity() {

    private var currentImage: Image? = null
    private lateinit var deletedImages: ArrayList<Image>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityViewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedList = savedInstanceState?.getParcelableArrayList<Image>(DELETED_IMAGES)
        deletedImages = savedList ?: ArrayList()

        val result = Intent()
        result.putExtra(DELETED_IMAGES, deletedImages)
        setResult(RESULT_OK, result)

        val savedImage = savedInstanceState?.getParcelable<Image>(CURRENT_IMAGE)
        if (savedImage != null) {
            currentImage = savedImage
        }

        binding.RecyclerView.setHasFixedSize(true)
        binding.RecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        PagerSnapHelper().attachToRecyclerView(binding.RecyclerView)

        val initial = intent.getIntExtra(POSITION, 0)
        binding.RecyclerView.scrollToPosition(initial)

        lifecycleScope.launch {
            val database = NotallyDatabase.getDatabase(application)
            val id = intent.getLongExtra(Constants.SelectedBaseNote, 0)

            val json = withContext(Dispatchers.IO) { database.getBaseNoteDao().getImages(id) }
            val original = Converters.jsonToImages(json)
            val images = ArrayList<Image>(original.size)
            original.filterNotTo(images) { image -> deletedImages.contains(image) }

            val mediaRoot = IO.getExternalImagesDirectory(application)
            val adapter = ImageAdapter(mediaRoot, images)
            binding.RecyclerView.adapter = adapter
            setupToolbar(binding, adapter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CURRENT_IMAGE, currentImage)
        outState.putParcelableArrayList(DELETED_IMAGES, deletedImages)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EXPORT_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                writeImageToUri(uri)
            }
        }
    }


    private fun setupToolbar(binding: ActivityViewImageBinding, adapter: ImageAdapter) {
        binding.Toolbar.setNavigationOnClickListener { finish() }

        val layoutManager = binding.RecyclerView.layoutManager as LinearLayoutManager
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                val position = layoutManager.findFirstVisibleItemPosition()
                binding.Toolbar.title = "${position + 1} / ${adapter.itemCount}"
            }
        })

        binding.RecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val position = layoutManager.findFirstVisibleItemPosition()
                binding.Toolbar.title = "${position + 1} / ${adapter.itemCount}"
            }
        })

        binding.Toolbar.menu.add(R.string.share, R.drawable.share) {
            val position = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (position != -1) {
                val image = adapter.items[position]
                share(image)
            }
        }
        binding.Toolbar.menu.add(R.string.save_to_device, R.drawable.save) {
            val position = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (position != -1) {
                val image = adapter.items[position]
                saveToDevice(image)
            }
        }
        binding.Toolbar.menu.add(R.string.delete, R.drawable.delete) {
            val position = layoutManager.findFirstCompletelyVisibleItemPosition()
            if (position != -1) {
                delete(position, adapter)
            }
        }
    }


    private fun share(image: Image) {
        val mediaRoot = IO.getExternalImagesDirectory(application)
        val file = if (mediaRoot != null) File(mediaRoot, image.name) else null
        if (file != null && file.exists()) {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = image.mimeType
            intent.putExtra(Intent.EXTRA_STREAM, uri)

            // Necessary for sharesheet to show a preview of the image
            // Check -> https://commonsware.com/blog/2021/01/07/action_send-share-sheet-clipdata.html
            intent.clipData = ClipData.newRawUri(null, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(intent, null)
            startActivity(chooser)
        }
    }


    private fun saveToDevice(image: Image) {
        val mediaRoot = IO.getExternalImagesDirectory(application)
        val file = if (mediaRoot != null) File(mediaRoot, image.name) else null
        if (file != null && file.exists()) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = image.mimeType
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, "Notally Image")

            currentImage = image
            startActivityForResult(intent, REQUEST_EXPORT_FILE)
        }
    }

    private fun writeImageToUri(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val mediaRoot = IO.getExternalImagesDirectory(application)
                val file = if (mediaRoot != null) File(mediaRoot, requireNotNull(currentImage).name) else null
                if (file != null && file.exists()) {
                    val output = contentResolver.openOutputStream(uri) as FileOutputStream
                    output.channel.truncate(0)
                    val input = FileInputStream(file)
                    input.copyTo(output)
                    input.close()
                    output.close()
                }
            }
            Toast.makeText(this@ViewImage, R.string.saved_to_device, Toast.LENGTH_LONG).show()
        }
    }


    private fun delete(position: Int, adapter: ImageAdapter) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_image_forever)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                val image = adapter.items.removeAt(position)
                deletedImages.add(image)
                adapter.notifyItemRemoved(position)
                if (adapter.items.isEmpty()) {
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val POSITION = "POSITION"
        const val CURRENT_IMAGE = "CURRENT_IMAGE"
        const val DELETED_IMAGES = "DELETED_IMAGES"
        private const val REQUEST_EXPORT_FILE = 40
    }
}
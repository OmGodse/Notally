package com.omgodse.notally.viewmodels

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.widget.Toast
import androidx.core.text.getSpans
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.AttachmentDeleteService
import com.omgodse.notally.Cache
import com.omgodse.notally.Progress
import com.omgodse.notally.R
import com.omgodse.notally.image.Event
import com.omgodse.notally.image.ImageError
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.preferences.BetterLiveData
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.room.Audio
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.NotallyDatabase
import com.omgodse.notally.room.SpanRepresentation
import com.omgodse.notally.room.Type
import com.omgodse.notally.widget.WidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID

class NotallyModel(private val app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val baseNoteDao = database.getBaseNoteDao()

    val textSize = Preferences.getInstance(app).textSize.value

    var isNewNote = true
    var isFirstInstance = true

    var type = Type.NOTE

    var id = 0L
    var folder = Folder.NOTES
    var color = Color.DEFAULT

    var title = String()
    var pinned = false
    var timestamp = System.currentTimeMillis()

    val labels = ArrayList<String>()

    var body: Editable = SpannableStringBuilder()

    val items = ArrayList<ListItem>()
    val images = BetterLiveData<List<Image>>(emptyList())
    val audios = BetterLiveData<List<Audio>>(emptyList())

    val addingImages = MutableLiveData<Progress>()
    val eventBus = MutableLiveData<Event<List<ImageError>>>()

    var imageRoot = IO.getExternalImagesDirectory(app)
    var audioRoot = IO.getExternalAudioDirectory(app)

    fun addAudio() {
        viewModelScope.launch {
            val audio = withContext(Dispatchers.IO) {
                /*
                Regenerate because the directory may have been deleted between the time of activity creation
                and audio recording
                */
                audioRoot = IO.getExternalAudioDirectory(app)
                requireNotNull(audioRoot) { "audioRoot is null" }

                /*
                If we have reached this point, an SD card (emulated or real) exists and audioRoot
                is not null. audioRoot.exists() can be false if the folder `Audio` has been deleted after
                the previous line, but audioRoot itself can't be null
                */
                val original = IO.getTempAudioFile(app)
                val name = "${UUID.randomUUID()}.m4a"
                val final = File(audioRoot, name)
                val input = FileInputStream(original)
                IO.copyStreamToFile(input, final)

                original.delete()

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(final.path)
                val duration = requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
                Audio(name, duration.toLong(), System.currentTimeMillis())
            }
            val copy = ArrayList(audios.value)
            copy.add(audio)
            audios.value = copy
            updateAudios()
        }
    }

    fun deleteAudio(audio: Audio) {
        viewModelScope.launch {
            val copy = ArrayList(audios.value)
            copy.remove(audio)
            audios.value = copy
            updateAudios()
            AttachmentDeleteService.start(app, arrayListOf(audio))
        }
    }


    fun addImages(uris: Array<Uri>) {
        val unknownName = app.getString(R.string.unknown_name)
        val unknownError = app.getString(R.string.unknown_error)
        val invalidImage = app.getString(R.string.invalid_image)
        val formatNotSupported = app.getString(R.string.image_format_not_supported)
        val errorWhileRenaming = app.getString(R.string.error_while_renaming_image)

        viewModelScope.launch {
            addingImages.value = Progress(true, 0, uris.size, false)

            val successes = ArrayList<Image>()
            val errors = ArrayList<ImageError>()

            uris.forEachIndexed { index, uri ->
                withContext(Dispatchers.IO) {
                    val document = requireNotNull(DocumentFile.fromSingleUri(app, uri))
                    val displayName = document.name ?: unknownName
                    try {
                        /*
                        Regenerate because the directory may have been deleted between the time of activity creation
                        and image addition
                         */
                        imageRoot = IO.getExternalImagesDirectory(app)
                        requireNotNull(imageRoot) { "externalRoot is null" }

                        /*
                        If we have reached this point, an SD card (emulated or real) exists and externalRoot
                        is not null. externalRoot.exists() can be false if the folder `Images` has been deleted after
                        the previous line, but externalRoot itself can't be null
                        */
                        val temp = File(imageRoot, "Temp")

                        val inputStream = requireNotNull(app.contentResolver.openInputStream(uri))
                        IO.copyStreamToFile(inputStream, temp)

                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(temp.path, options)
                        val mimeType = options.outMimeType

                        if (mimeType != null) {
                            val extension = getExtensionForMimeType(mimeType)
                            if (extension != null) {
                                val name = "${UUID.randomUUID()}.$extension"
                                if (IO.renameFile(temp, name)) {
                                    successes.add(Image(name, mimeType))
                                } else {
                                    // I don't expect this error to ever happen but just in case
                                    errors.add(ImageError(displayName, errorWhileRenaming))
                                }
                            } else errors.add(ImageError(displayName, formatNotSupported))
                        } else errors.add(ImageError(displayName, invalidImage))
                    } catch (exception: Exception) {
                        errors.add(ImageError(displayName, unknownError))
                        Operations.log(app, exception)
                    }
                }

                addingImages.value = Progress(true, index + 1, uris.size, false)
            }

            addingImages.value = Progress(false, 0, 0, false)

            if (successes.isNotEmpty()) {
                val copy = ArrayList(images.value)
                copy.addAll(successes)
                images.value = copy
                updateImages()
            }

            if (errors.isNotEmpty()) {
                eventBus.value = Event(errors)
            }
        }
    }

    fun deleteImages(list: ArrayList<Image>) {
        viewModelScope.launch {
            val copy = ArrayList(images.value)
            copy.removeAll(list)
            images.value = copy
            updateImages()
            AttachmentDeleteService.start(app, list)
        }
    }

    private fun getExtensionForMimeType(type: String): String? {
        return when (type) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            else -> null
        }
    }


    fun setLabels(list: List<String>) {
        labels.clear()
        labels.addAll(list)
    }


    suspend fun setState(id: Long) {
        if (id != 0L) {
            isNewNote = false

            val cachedNote = Cache.list.find { baseNote -> baseNote.id == id }
            val baseNote = cachedNote ?: withContext(Dispatchers.IO) { baseNoteDao.get(id) }

            if (baseNote != null) {
                this.id = id
                folder = baseNote.folder
                color = baseNote.color

                title = baseNote.title
                pinned = baseNote.pinned
                timestamp = baseNote.timestamp

                setLabels(baseNote.labels)

                body = baseNote.body.applySpans(baseNote.spans)

                items.clear()
                items.addAll(baseNote.items)

                images.value = baseNote.images
                audios.value = baseNote.audios
            } else {
                createBaseNote()
                Toast.makeText(app, R.string.cant_find_note, Toast.LENGTH_LONG).show()
            }
        } else createBaseNote()
    }

    private suspend fun createBaseNote() {
        id = withContext(Dispatchers.IO) { baseNoteDao.insert(getBaseNote()) }
    }


    suspend fun deleteBaseNote() {
        withContext(Dispatchers.IO) { baseNoteDao.delete(id) }
        WidgetProvider.sendBroadcast(app, longArrayOf(id))
        val attachments = ArrayList(images.value + audios.value)
        if (attachments.isNotEmpty()) {
            AttachmentDeleteService.start(app, attachments)
        }
    }

    suspend fun saveNote(): Long {
        return withContext(Dispatchers.IO) { baseNoteDao.insert(getBaseNote()) }
    }

    private suspend fun updateImages() {
        withContext(Dispatchers.IO) { baseNoteDao.updateImages(id, images.value) }
    }

    private suspend fun updateAudios() {
        withContext(Dispatchers.IO) { baseNoteDao.updateAudios(id, audios.value) }
    }


    private fun getBaseNote(): BaseNote {
        val spans = getFilteredSpans(body)
        val body = this.body.trimEnd().toString()
        val items = this.items.filter { item -> item.body.isNotEmpty() }
        return BaseNote(id, type, folder, color, title, pinned, timestamp, labels, body, spans, items, images.value, audios.value)
    }

    private fun getFilteredSpans(spanned: Spanned): ArrayList<SpanRepresentation> {
        val representations = LinkedHashSet<SpanRepresentation>()
        spanned.getSpans<CharacterStyle>().forEach { span ->
            val end = spanned.getSpanEnd(span)
            val start = spanned.getSpanStart(span)
            val representation = SpanRepresentation(false, false, false, false, false, start, end)

            when (span) {
                is StyleSpan -> {
                    representation.bold = span.style == Typeface.BOLD
                    representation.italic = span.style == Typeface.ITALIC
                }

                is URLSpan -> representation.link = true
                is TypefaceSpan -> representation.monospace = span.family == "monospace"
                is StrikethroughSpan -> representation.strikethrough = true
            }

            if (representation.isNotUseless()) {
                representations.add(representation)
            }
        }
        return getFilteredRepresentations(ArrayList(representations))
    }

    private fun getFilteredRepresentations(representations: ArrayList<SpanRepresentation>): ArrayList<SpanRepresentation> {
        representations.forEachIndexed { index, representation ->
            val match = representations.find { spanRepresentation ->
                spanRepresentation.isEqualInSize(representation)
            }
            if (match != null && representations.indexOf(match) != index) {
                if (match.bold) {
                    representation.bold = true
                }
                if (match.link) {
                    representation.link = true
                }
                if (match.italic) {
                    representation.italic = true
                }
                if (match.monospace) {
                    representation.monospace = true
                }
                if (match.strikethrough) {
                    representation.strikethrough = true
                }
                val copy = ArrayList(representations)
                copy[index] = representation
                copy.remove(match)
                return getFilteredRepresentations(copy)
            }
        }
        return representations
    }
}
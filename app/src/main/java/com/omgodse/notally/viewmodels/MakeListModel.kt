package com.omgodse.notally.viewmodels

import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.io.FileWriter

class MakeListModel : BaseModel() {

    val items = ArrayList<ListItem>()

    override fun saveNote() {
        val listItems = items.filter { item -> item.body.isNotBlank() }
        if (title.isEmpty() && listItems.isEmpty()) {
            return
        }

        file?.let {
            val fileWriter = FileWriter(it)
            val xmlWriter = XMLWriter(XMLTags.List)
            xmlWriter.startNote()
            xmlWriter.setDateCreated(timestamp.toString())
            xmlWriter.setTitle(title)
            xmlWriter.setListItems(listItems)
            xmlWriter.setLabels(labels.value ?: HashSet())
            xmlWriter.endNote()

            fileWriter.write(xmlWriter.getText())
            fileWriter.close()
        }
    }

    override fun setStateFromFile() {
        file?.let { file ->
            if (file.exists()){
                val xmlReader = XMLReader(file)
                title = xmlReader.getTitle()
                timestamp = xmlReader.getDateCreated().toLong()

                items.apply {
                    clear()
                    addAll(xmlReader.getListItems())
                }
                labels.value = xmlReader.getLabels()
            }
        }
    }
}
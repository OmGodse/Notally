package com.omgodse.notally.viewmodels

import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter

class MakeListModel : BaseModel() {

    val items = ArrayList<ListItem>()

    override fun saveNote() {
        val listItems = items.filter { item -> item.body.isNotBlank() }
        file?.let {
            val xmlWriter = XMLWriter(XMLTags.List, it)

            xmlWriter.start()
            xmlWriter.setTimestamp(timestamp.toString())
            xmlWriter.setTitle(title)
            xmlWriter.setListItems(listItems)
            xmlWriter.setLabels(labels.value ?: HashSet())

            xmlWriter.end()
        }
    }

    override fun setStateFromFile() {
        file?.let { file ->
            if (file.exists()){
                val xmlReader = XMLReader(file)
                title = xmlReader.getTitle()
                timestamp = xmlReader.getTimestamp().toLong()

                items.clear()
                items.addAll(xmlReader.getListItems())

                labels.value = xmlReader.getLabels()
            }
        }
    }
}
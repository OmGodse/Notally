package com.omgodse.notally.miscellaneous

data class Note (val isNote: Boolean,
                 val title: String,
                 val timestamp: String,
                 val body: String,
                 val items: ArrayList<ListItem>,
                 val spans: ArrayList<SpanRepresentation>,
                 val labels: HashSet<String>,
                 val filePath: String) {

    fun isEmpty(): Boolean {
        return if (isNote){
            title.isBlank() && body.isBlank()
        } else title.isBlank() && items.isEmpty()
    }
}
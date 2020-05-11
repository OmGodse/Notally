package com.omgodse.notally.miscellaneous

data class Note (val isNote: Boolean,
                 val title: String,
                 val timestamp: String,
                 val body: String,
                 val items: ArrayList<ListItem>,
                 val spans: ArrayList<SpanRepresentation>,
                 val labels: HashSet<String>,
                 val filePath: String) {

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            is Note -> other.hashCode() == hashCode()
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = isNote.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + items.hashCode()
        result = 31 * result + spans.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + filePath.hashCode()
        return result
    }
}
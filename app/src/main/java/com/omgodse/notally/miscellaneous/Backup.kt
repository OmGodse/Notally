package com.omgodse.notally.miscellaneous

data class Backup (val notes: ArrayList<Note>, val deletedNotes: ArrayList<Note>, val archivedNotes: ArrayList<Note>, val labels: HashSet<String>)
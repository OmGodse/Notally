package com.omgodse.notally.xml

import com.omgodse.notally.room.BaseNote

class Backup(
    val baseNotes: List<BaseNote>,
    val deletedNotes: List<BaseNote>,
    val archivedNotes: List<BaseNote>,
    val labels: HashSet<String>
)
package com.omgodse.notally.room.json

import org.json.JSONArray

class IterableJSONArray<T>(json: String) : JSONArray(json), Iterable<T> {

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var nextIndex = 0

            override fun hasNext(): Boolean {
                return nextIndex < length()
            }

            override fun next(): T {
                val element = get(nextIndex) as T
                nextIndex++
                return element
            }
        }
    }
}
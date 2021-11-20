package com.omgodse.notally.fragments

import com.omgodse.notally.R

class Search : NotallyFragment() {

    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults
}
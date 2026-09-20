package com.neverreader.util.android.view

import androidx.recyclerview.widget.DefaultItemAnimator

class InstantChangeItemAnimator : DefaultItemAnimator() {
    override fun getChangeDuration(): Long = 0L
}
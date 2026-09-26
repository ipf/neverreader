package com.neverreader.app.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.neverreader.sdk.util.AbsNeverReaderActivity

class OpenSourceLicensesActivity : AbsNeverReaderActivity() {

    override val accessType: ActivityAccessRestriction
        get() = ActivityAccessRestriction.ANY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, OpenSourceLicensesFragment())
                .commit()
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, OpenSourceLicensesActivity::class.java))
        }
    }
}

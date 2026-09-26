/*
 * Copyright (C) 2014 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neverreader.util.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Process Phoenix facilitates restarting your application process. This should only be used for
 * things like fundamental state changes in your debug builds (e.g., changing from staging to
 * production).
 *
 *
 * Trigger process recreation by calling [.triggerRebirth] with a [Context] instance.
 */
class PPActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IS_ENABLED) {
            val intent = intent.getParcelableExtra<Intent?>(KEY_RESTART_INTENT)
            startActivity(intent)
            finish()
            Runtime.getRuntime().exit(0) // Kill kill kill!
        } else {
            finish()
        }
    }

    companion object {
        // Renamed so we can have it in our manifest with clearly stating what it is.
        private val IS_ENABLED = false

        private const val KEY_RESTART_INTENT = "phoenix_restart_intent"

        /**
         * Call to restart the application process using the specified intent.
         *
         *
         * The behavior of the current process after invoking this method is undefined.
         */
        /**
         * Call to restart the application process using the [default][Intent.CATEGORY_DEFAULT]
         * activity as intent.
         *
         *
         * The behavior of the current process after invoking this method is undefined.
         */
        @JvmOverloads
        fun triggerRebirth(context: Context, nextIntent: Intent? = getRestartIntent(context)) {
            if (!IS_ENABLED) {
                return
            }

            val intent = Intent(context, PPActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // In case we are called with non-Activity context.
            intent.putExtra(KEY_RESTART_INTENT, nextIntent)
            context.startActivity(intent)
            if (context is Activity) {
                context.finish()
            }
            Runtime.getRuntime().exit(0) // Kill kill kill!
        }

        private fun getRestartIntent(context: Context): Intent? {
            val defaultIntent = Intent(Intent.ACTION_MAIN, null)
            defaultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            defaultIntent.addCategory(Intent.CATEGORY_DEFAULT)

            val packageName = context.packageName
            val packageManager = context.packageManager
            return packageManager.getLaunchIntentForPackage(packageName)
        }
    }
}

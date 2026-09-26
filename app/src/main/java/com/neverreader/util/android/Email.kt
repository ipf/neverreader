package com.neverreader.util.android

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.neverreader.app.R
import com.neverreader.util.java.ListUtils

object Email {
    /**
     * Will launch a SEND intent with the email info provided. If no app on the device
     * can handle the intent then it will display an alert dialog to let the user know.
     *
     * @param to
     * @param subject
     * @param message
     * @param context
     */
    fun startEmailIntent(
        to: String?,
        subject: String?,
        message: String?,
        context: Context,
        attachments: MutableList<Attachment>
    ) {
        val intent: Intent?
        if (attachments.isNotEmpty()) {
            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            val uris = ArrayList<Uri>(attachments.size)
            for (file in attachments) {
                file.uri?.let { uris.add(it) }
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        } else {
            intent = Intent(Intent.ACTION_SEND)
        }

        intent.setType("text/plain")

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf<String?>(to))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        if (message != null) {
            intent.putExtra(Intent.EXTRA_TEXT, message)
        }

        if (IntentUtils.isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent)
        } else {
            val msg =
                context.getString(R.string.dg_no_email_app_m) + "\n\n" + context.getString(R.string.dg_no_email_app_m_support)

            AlertDialog.Builder(context)
                .setTitle(R.string.dg_no_email_app_t)
                .setMessage(msg)
                .setPositiveButton(R.string.ac_ok, null)
                .show()
        }
    }

    class Attachment(val mimeType: String?, val uri: Uri?) {
        constructor(mimeType: String?, uri: String?) : this(mimeType, Uri.parse(uri))
    }
}

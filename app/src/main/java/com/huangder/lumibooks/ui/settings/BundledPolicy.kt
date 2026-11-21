package com.huangder.lumibooks.ui.settings

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import com.huangder.lumibooks.R

internal enum class BundledPolicy(
    val version: Int,
    val file: String,
    @StringRes val title: Int,
    val url: String
) {
    TERMS(6, "community-terms.html", R.string.terms_of_service, "https://github.com/wxyu71/Lumi_Books/blob/main/app/src/main/assets/html/community-terms.html"),
    PRIVACY(6, "community-privacy.html", R.string.privacy_policy, "https://github.com/wxyu71/Lumi_Books/blob/main/app/src/main/assets/html/community-privacy.html");

    fun open(context: Context, version: Int = this.version) {
        if (version == this.version) {
            context.startActivity(
                Intent(context, WebViewActivity::class.java)
                    .putExtra("title", context.getString(title))
                    .putExtra("file", file)
            )
        } else {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            try {
                context.startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(context, R.string.network_error, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

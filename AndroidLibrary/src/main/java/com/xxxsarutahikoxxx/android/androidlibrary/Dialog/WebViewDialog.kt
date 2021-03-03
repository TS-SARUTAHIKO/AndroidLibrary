package com.xxxsarutahikoxxx.android.androidlibrary.Dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.xxxsarutahikoxxx.android.androidlibrary.R


/**
 * webページをダイアログで表示するためのクラス
 *
 * [FragmentActivity.browseWithDialog(url : String)] を用いて表示すること
 * */
class WebViewDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val url = arguments?.getString("URL") ?: ""

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.web_view_dialog_layout, null)
        val webView = view.findViewById<WebView>(R.id.WebViewDialog_Content)
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)

        return AlertDialog.Builder(requireContext())
                .setView(view)
                .create()
    }
}

fun FragmentActivity.browseWithDialog(url : String){
    WebViewDialog().let {
        it.arguments = Bundle().apply { putString("URL", url) }

        it.show(this.supportFragmentManager, "")
    }
}
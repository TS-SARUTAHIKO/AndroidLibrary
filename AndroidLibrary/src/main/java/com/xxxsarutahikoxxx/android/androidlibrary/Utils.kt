package com.xxxsarutahikoxxx.android.androidlibrary

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.xxxsarutahikoxxx.android.androidlibrary.Dialog.browseWithDialog
import kotlin.concurrent.thread


/** トースト表示用のショートカット */
var View.toast : Any?
    get() = throw RuntimeException("入力専用")
    set(value) {
        thread {
            Handler(context.mainLooper).post {
                Toast
                    .makeText(context, "$value", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

/** トースト表示用のショートカット */
var Activity.toast : Any?
    get() = throw RuntimeException("入力専用")
    set(value) {
        thread {
            Handler(this.mainLooper).post {
                Toast
                    .makeText(this, "$value", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

/** パーミッションの取得関数 */
fun Activity.getPermissions(vararg permission : String) : Boolean {
    val request = permission.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

    if( request.isNotEmpty() ){
        ActivityCompat.requestPermissions(this, request.toTypedArray(), 0)
    }

    return request.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
}

/** Browse */
fun FragmentActivity.browse(url : String, dialog : Boolean = true){
    if( dialog ){
        browseWithDialog(url)
    }else{
        val uri : Uri = Uri.parse(url)
        val i : Intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(i);
    }
}
package com.xxxsarutahikoxxx.android.androidlibrary

import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import com.xxxsarutahikoxxx.android.recyclertreeviewadapter.asTree
import com.xxxsarutahikoxxx.android.recyclertreeviewadapter.create
import kotlinx.android.synthetic.main.content_main.*
import java.lang.RuntimeException


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }



        Playing_Button.apply {
            onPlay = { view, mode -> out = "Play : $mode" }
            onPause = { view, mode -> out = "Pause : $mode" }
        }

        //createTree()
    }
    fun createTree(){
        RecyclerAsTree.asTree {
            create("c1")
            create("c2"){
                create("c2-1")
                create("c2-2"){
                    create("c2-2-1"){
                        create("c2-2-1-------------1-------------2-------------3-------------4-------------5")
                    }
                }
            }
            create("c3"){
                create(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            }
            create("c4")
            create("c5")
            create("c6")
            create("c7")
            create("c8")
            create("c9")
            create("c10")
            create("c11")

            expandAll()
        }.apply {
            onSelected = {
                it?.content
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}


var out : Any?
    get() = throw RuntimeException("")
    set(value) { Log.d("標準出力", "$value") }

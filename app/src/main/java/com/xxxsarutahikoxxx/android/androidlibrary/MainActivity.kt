package com.xxxsarutahikoxxx.android.androidlibrary

import android.os.Bundle
import android.os.Environment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.xxxsarutahikoxxx.android.recyclertreeviewadapter.asTree
import com.xxxsarutahikoxxx.android.recyclertreeviewadapter.create
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        RecyclerAsTree.asTree {
            create("c1")
            create("c2"){
                create("c2-1")
                create("c2-2"){
                    create("c2-2-1"){
                        create("c2-2-1----------------------------------------------------------------------------")
                    }
                }
            }
            create("c3"){
                create(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            }

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
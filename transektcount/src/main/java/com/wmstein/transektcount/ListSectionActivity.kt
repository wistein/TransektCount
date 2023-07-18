package com.wmstein.transektcount

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource

/************************************************************************************
 * Shows the list of selectable sections which is put together by SectionListAdapter.
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Starts CountingActivity, EditSectionActivity and NewSectionActivity.
 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
class ListSectionActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    // preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    private var sectionDataSource: SectionDataSource? = null
    var sections: List<Section>? = null
    private var maxId = 0
    var list: ListView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_section)
        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }
        val list_view = findViewById<LinearLayout>(R.id.list_view)
        list_view.background = transektCount!!.background
        list = findViewById(android.R.id.list)
    }

    fun deleteSection(sct: Section?) {
        sectionDataSource!!.deleteSection(sct!!)
        showData()
        list!!.invalidate() //force list to draw
    }

    override fun onResume() {
        super.onResume()
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)
        sectionDataSource = SectionDataSource(this)
        sectionDataSource!!.open()
        showData()
    }

    override fun onPause() {
        super.onPause()
        sectionDataSource!!.close()
    }

    // show sections list
    private fun showData() {
        Runtime.getRuntime().gc() // garbage collection to free memory
        sections = sectionDataSource!!.getAllSections(prefs!!)
        maxId = sectionDataSource!!.maxId
        val adapter = SectionListAdapter(this, R.layout.listview_section_row, sections!!, maxId)
        val lv = findViewById<ListView>(android.R.id.list)
        lv.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.list_section, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.newSect) {
            startActivity(
                Intent(
                    this,
                    NewSectionActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val list_view = findViewById<LinearLayout>(R.id.list_view)
        list_view.background = null
        list_view.background = transektCount!!.setBackground()
        brightPref = prefs.getBoolean("pref_bright", true)
    }

    companion object {
        //private static final String TAG = "TransektCountListSectAct";
    }

}

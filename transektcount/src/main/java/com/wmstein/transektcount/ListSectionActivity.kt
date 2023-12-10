package com.wmstein.transektcount

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource

/************************************************************************************
 * Shows the list of selectable sections which is put together by ListSectionAdapter.
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Starts EditSectionActivity.
 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-11-28
 */
class ListSectionActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    // preferences
    private var prefs = TransektCountApplication.getPrefs()
    private var brightPref = false

    private var sectionDataSource: SectionDataSource? = null
    var sections: List<Section>? = null // list of all sections
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
        val listView = findViewById<LinearLayout>(R.id.list_view)
        listView.background = transektCount!!.background
        list = findViewById(android.R.id.list)
    }
    // end of onCreate

    override fun onResume() {
        super.onResume()

        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
        brightPref = prefs.getBoolean("pref_bright", true)

        sectionDataSource = SectionDataSource(this)
        sectionDataSource!!.open()
        sections = sectionDataSource!!.getAllSections(prefs!!)
        maxId = sectionDataSource!!.maxId

        showData()
    }
    // end of onResume

    override fun onPause() {
        super.onPause()

        sectionDataSource!!.close()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val listView = findViewById<LinearLayout>(R.id.list_view)
        listView.background = null
        listView.background = transektCount!!.setBackground()
        if (prefs != null) {
            brightPref = prefs.getBoolean("pref_bright", true)
        }
    }

    // delete section and respective gpx track as well as respective alerts
    fun deleteSection(sct: Section?) {
        sectionDataSource!!.deleteSection(sct!!)
        sections = sectionDataSource!!.getAllSections(prefs!!)
        maxId = sectionDataSource!!.maxId

        showData()
        list!!.invalidate() //force list to draw
    }

    // show sections list by ListSectionAdapter
    private fun showData() {
//        Runtime.getRuntime().gc() // garbage collection to free memory
        val adapter = ListSectionAdapter(
            this,
            R.layout.listview_section_row,
            sections!!,
            maxId
            )
        val lv = findViewById<ListView>(android.R.id.list)
        lv.adapter = adapter
    }

}

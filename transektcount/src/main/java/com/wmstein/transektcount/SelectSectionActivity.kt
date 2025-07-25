package com.wmstein.transektcount

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource

/************************************************************************************
 * Shows the list of selectable sections which is put together by SelectSectionAdapter.
 * Based on ListProjectActivity.java by milo on 05/05/2014.
 * Starts EditSectionListActivity.
 * Changes and additions for TransektCount by wmstein since 2016-02-16,
 * last edited in Java on 2023-07-07,
 * converted to Kotlin on 2023-07-17,
 * renamed from ListSectionActivity.kt on 2024-11-26,
 * last edited on 2025-06-20
 */
class SelectSectionActivity : AppCompatActivity() {
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

        if (MyDebug.DLOG) Log.d(TAG, "45 onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_list_section)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.list_view)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        brightPref = prefs.getBoolean("pref_bright", true)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        transektCount = application as TransektCountApplication

        val listView = findViewById<LinearLayout>(R.id.list_view)
        listView.background = transektCount!!.setBackgr()
        list = findViewById(android.R.id.list)

        sectionDataSource = SectionDataSource(this)

        // new onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@SelectSectionActivity)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.DLOG) Log.d(TAG, "74 onResume")

        sectionDataSource!!.open()
        sections = sectionDataSource!!.getAllSections(prefs)
        maxId = sectionDataSource!!.maxId

        showData()
    }
    // End of onResume()

    override fun onPause() {
        super.onPause()

        if (MyDebug.DLOG) Log.d(TAG, "87 onPause")

        sectionDataSource!!.close()
    }

    // delete section as well as respective alerts
    fun deleteSection(sct: Section?) {
        sectionDataSource!!.deleteSection(sct!!)
        sections = sectionDataSource!!.getAllSections(prefs)
        maxId = sectionDataSource!!.maxId

        showData()
        list!!.invalidate() //force list to draw
    }

    // show sections list by SelectSectionAdapter
    private fun showData() {
        val adapter = SelectSectionAdapter(
            this,
            R.layout.listview_section_row,
            sections!!,
            maxId
            )
        val lv = findViewById<ListView>(android.R.id.list)
        lv.adapter = adapter
    }

    companion object {
        private const val TAG = "SelSectAct"
    }

}

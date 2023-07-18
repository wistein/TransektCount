package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.wmstein.transektcount.database.Section
import com.wmstein.transektcount.database.SectionDataSource

/*********************************************************
 * Create a new empty transect section list (NewCount)
 * uses activity_new_section.xml
 * NewSectionActivity is called from ListSectionActivity.
 * Based on NewProjectActivity.java by milo on 05/05/2014,
 * changed by wmstein since 2016-02-16,
 * last edited in Java on 2023-06-25,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
class NewSectionActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var transektCount: TransektCountApplication? = null

    private var prefs = TransektCountApplication.getPrefs()

    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null
    var section: Section? = null
    private var newSection: Section? = null
    var layout: ViewGroup? = null
    private var newsectName: EditText? = null
    private var sectionDataSource: SectionDataSource? = null
    var sections: List<Section> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_new_section)
        transektCount = application as TransektCountApplication
        prefs = TransektCountApplication.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)

        val baseLayout = findViewById<ScrollView>(R.id.newsectScreen) //in activity_new_section.xml
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(baseLayout.resources, bMap)
        baseLayout.background = bg
        sectionDataSource = SectionDataSource(this)
        newsectName = findViewById(R.id.newsectName) //in activity_new_section.xml
        newsectName!!.setTextColor(Color.WHITE)
        newsectName!!.setHintTextColor(Color.argb(255, 170, 170, 170))
    }

    override fun onResume() {
        super.onResume()
        sectionDataSource!!.open()
        // Show the keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        sectionDataSource!!.close()
    }

    // Inflate the menu; this adds items to the action bar if it is present.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.new_section, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on 
        val id = item.itemId
        if (id == R.id.menuSaveExit) {
            saveSection(layout)
        }
        return super.onOptionsItemSelected(item)
    }

    // Save section with plausi-check for empty or duplicate section name
    @SuppressLint("ApplySharedPref")
    fun saveSection(view: View?) {
        // first, edit the section name
        val sect_name = newsectName!!.text.toString()
        sections = sectionDataSource!!.getAllSections(prefs!!)

        // check for empty section name
        if (sect_name.isEmpty()) {
            showSnackbarRed(getString(R.string.newName))
            return
        }

        // check if this is not a duplicate of an existing name
        if (compSectionNames(sect_name)) {
            showSnackbarRed(sect_name + " " + getString(R.string.isdouble))
            return
        }

        // check if section is contiguous
        var entries = -1
        var maxId = 0
        try {
            entries = sectionDataSource!!.numEntries
        } catch (e: Exception) {
            if (MyDebug.LOG) showSnackbarRed("getNumEntries failed")
        }
        try {
            maxId = sectionDataSource!!.maxId
        } catch (e: Exception) {
            if (MyDebug.LOG) showSnackbarRed("getMaxId failed")
        }
        if (entries != maxId) {
            showSnackbarRed(getString(R.string.notContiguous))
            if (MyDebug.LOG) showSnackbarRed("maxId: $maxId, entries: $entries")
            return
        }
        newSection = sectionDataSource!!.createSection(sect_name)
        sectionDataSource!!.saveSection(newSection!!)

        // Toast here, as snackbar doesn't show up
        Toast.makeText(this, getString(R.string.sectionSaved), Toast.LENGTH_SHORT).show()

        // Edit the new section.
        section = sectionDataSource!!.getSectionByName(sect_name)
        val section_id: Int = section!!.id

        // Store section_id into SharedPreferences.
        // That makes sure that the current selected section can be retrieved 
        // by EditSectionActivity
        val editor = prefs!!.edit()
        editor.putInt("section_id", section_id)
        editor.commit()
        if (MyDebug.LOG) Log.d(TAG, "sect_id = $section_id")
        val intent = Intent(this@NewSectionActivity, EditSectionActivity::class.java)
        intent.putExtra("section_id", section_id)
        startActivity(intent)
    }

    // Compare section names for duplicates and return TRUE when duplicate found
    // created by wmstein on 10.04.2016
    private fun compSectionNames(newname: String): Boolean {
        var isDblName = false
        var sname: String?
        val sectionList = sectionDataSource!!.allSectionNames

        // for all Sections
        val childcount = sectionList.size + 1
        for (i in 1 until childcount) {
            section = sectionDataSource!!.getSection(i)
            sname = section!!.name
            if (MyDebug.LOG) Log.d(TAG, "sname = $sname")
            if (newname == sname) {
                isDblName = true
                if (MyDebug.LOG) Log.d(TAG, "Double name = $sname")
                break
            }
        }
        return isDblName
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        val baseLayout = findViewById<ScrollView>(R.id.newsectScreen)
        baseLayout.background = null
        bMap = transektCount!!.decodeBitmap(
            R.drawable.kbackground,
            transektCount!!.width,
            transektCount!!.height
        )
        bg = BitmapDrawable(baseLayout.resources, bMap)
        baseLayout.background = bg
    }

    private fun showSnackbarRed(str: String) // bold red text
    {
        val view = findViewById<View>(R.id.newsectScreen)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setTextColor(Color.RED)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        sB.show()
    }

    companion object {
        private const val TAG = "TransektCountNewSectAct"
    }

}

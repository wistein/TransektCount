package com.wmstein.filechooser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.wmstein.transektcount.R
import java.io.File
import java.io.FileFilter
import java.text.DateFormat
import java.text.SimpleDateFormat

/********************************************************************************
 * AdvFileChooser lets you select files from user's Documents directory.
 * It will be called within WelcomeActivity and uses FileArrayAdapter and Option.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18,
 * last change in Java on 2022-04-30,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2024-06-20
 */
class AdvFileChooser : Activity() {
    private var currentDir: File? = null
    private var adapter: FileArrayAdapter? = null
    private var fileFilter: FileFilter? = null
    private var fileExtension: String = ""
    private var fileName: String? = null
    private var fName: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.list_view)

        val extras = intent.extras
        if (extras != null) {
            if (extras.getString("filterFileExtension") != null) {
                fileExtension = extras.getString("filterFileExtension")!!
                fileName = extras.getString("filterFileName")

                fileFilter = FileFilter { pathname: File ->
                    pathname.name.contains(".") &&
                            pathname.name.contains(fileName!!) &&
                            fileExtension.contains(
                                pathname.name.substring(
                                    pathname.name.lastIndexOf(".")
                                )
                            )
                }
            }
        }

        // set FileChooser Headline
        var fileHd = ""
        if (fileExtension.endsWith("db")) // headline for db-file
        {
            fileHd = getString(R.string.fileHeadlineDB)
        }
        else if (fileExtension.endsWith("exp")) // headline for gpx-file
        {
            fileHd = getString(R.string.fileHeadlineEXP)
        }
        val fileHead: TextView = findViewById(R.id.fileHead)
        fileHead.text = fileHd

        // currentDir = /storage/emulated/0/Documents/TransektCount/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            currentDir = Environment.getExternalStorageDirectory()
            if(fileName.equals("tourcount"))
                currentDir = File("$currentDir/Documents/TourCount")
            else
                currentDir = File("$currentDir/Documents/TransektCount")
        } else {
            currentDir = Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_DOCUMENTS)
            if(fileName.equals("tourcount"))
                currentDir = File("$currentDir/TourCount")
            else
                currentDir = File("$currentDir/TransektCount")
        }
        fill(currentDir!!)
    }
    // end of onCreate()

    // Disable Back-key in AdvFileChooser as return with no selected file produces
    //   NullPointerException of FileInputStream
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showSnackbar(getString(R.string.noBack))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // List only files in user's home directory
    private fun fill(f: File) {
        val dirs: Array<File>? = if (fileFilter != null) f.listFiles(fileFilter) else f.listFiles()
        this.title = getString(R.string.currentDir) + ": " + f.name
        val fls: MutableList<Option> = ArrayList() // list of files to choose from
        @SuppressLint("SimpleDateFormat") val dform: DateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm")
        try {
            assert(dirs != null)
            if (dirs != null) {
                for (ff in dirs) {
                    if (!ff.isHidden) {
                        fls.add(
                            Option(
                                ff.name, getString(R.string.fileSize) + ": "
                                        + ff.length() + " B,  " + getString(R.string.date) + ": "
                                        + dform.format(ff.lastModified()), ff.absolutePath, false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // do nothing
        }

        if (fls.isNotEmpty()) {
            fls.sort()
            val listView = findViewById<ListView>(R.id.lvFiles)
            adapter = FileArrayAdapter(listView.context, R.layout.file_view, fls)
            listView.adapter = adapter
            listView.onItemClickListener =
                OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                    val o = adapter!!.getItem(position)
                    if (!o.isBack) doSelect(o) else {
                        currentDir = File(o.path)
                        fill(currentDir!!)
                    }
                }
        }
        else {
            showSnackbarRed(getString(R.string.noFile))
            val intent = Intent()
            intent.putExtra("fileSelected", "")
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun doSelect(o: Option?) {
        // onFileClick(o);
        val fileSelected = File(o!!.path)
        val intent = Intent()
        intent.putExtra("fileSelected", fileSelected.absolutePath)
        setResult(RESULT_OK, intent)
        finish()
    }

    public override fun onStop() {
        super.onStop()
    }

    private fun showSnackbar(str: String) // green text
    {
        val view = findViewById<View>(R.id.lvFiles)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setTextColor(Color.GREEN)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        sB.show()
    }

    private fun showSnackbarRed(str: String) // red text
    {
        val view = findViewById<View>(R.id.lvFiles)
        val sB = Snackbar.make(view, str, Snackbar.LENGTH_LONG)
        sB.setTextColor(Color.RED)
        val tv = sB.view.findViewById<TextView>(R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        sB.show()
    }

    companion object {
        private const val TAG = "AdvFileChooser"
    }

}
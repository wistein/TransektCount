package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.wmstein.transektcount.LocationService.Companion.isNotBlank
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionIdGPS
import com.wmstein.transektcount.TransektCountApplication.Companion.sectionNameCurrent
import com.wmstein.transektcount.database.Section

/**************************************************************
 * SelectSectionAdapter is called from SelectSectionActivity.
 * It provides the lines of the sections list for selecting the
 * section to be used by the CountingActivity
 *
 * Based on ProjectListAdapter.java by milo on 2014-05-05.
 * Modified for TransektCount by wmstein since 2016-02-18
 * Last edited in Java on 2023-07-05,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2025-12-29
 */
internal class SelectSectionAdapter(
    private val context: Context,
    private val layoutResourceId: Int,
    private var sections: List<Section>, // list of all sections
    private val maxId: Int // highest section ID
) : ArrayAdapter<Section?>
    (context, layoutResourceId, sections) {
    private val mContext: Context = context
    private var sct: Section? = null

    // preferences
    private var prefs: SharedPreferences? = null
    private var buttonSoundPref: Boolean = false
    private var buttonSound: String = ""
    private var audioAttributionContext: Context? = null
    private var rToneP: MediaPlayer? = null
    private var buttonVibPref = false
    private val vibrator = mContext.getSystemService(Vibrator::class.java)
    private fun setPrefs() {
        buttonVibPref = prefs!!.getBoolean("pref_button_vib", false)
        buttonSoundPref = prefs!!.getBoolean("pref_button_sound", false)
        buttonSound = prefs!!.getString("button_sound", "")!!
    }

    private class SectionHolder {
        var txtTitle: Button? = null
        var txtRemark: TextView? = null
        var txtDate: TextView? = null
        var deleteSection: ImageButton? = null
    }

    // Constructor of entries for the sections list per position
    //   may need garbage collection in SelectSectionActivity as RAM may run short
    @SuppressLint("SetTextI18n", "ResourceAsColor")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        audioAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext.createAttributionContext("ringSound")
            else mContext
        var sectionsListRow = convertView
        val holder: SectionHolder
        prefs = TransektCountApplication.getPrefs()
        setPrefs()

        // Prepare button sounds
        if (buttonSoundPref) {
            val uriP = if (isNotBlank(buttonSound))
                buttonSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (rToneP == null) {
                rToneP = MediaPlayer.create(audioAttributionContext, uriP)
            }
        }

        val section = sections[position]
        val sectionId = section.id

        // if there is still no row for the current section list
        if (sectionsListRow == null) {
            val inflater = (mContext as Activity).layoutInflater
            sectionsListRow = inflater.inflate(layoutResourceId, parent, false)
            holder = SectionHolder()
            holder.txtTitle = sectionsListRow?.findViewById(R.id.txtTitle)
            holder.txtRemark = sectionsListRow?.findViewById(R.id.txtRemark)
            holder.txtDate = sectionsListRow?.findViewById(R.id.txtDate)
            holder.deleteSection = sectionsListRow?.findViewById(R.id.deleteSection)

            // make title and remark click sensitive
            holder.txtTitle!!.setOnClickListener(mOnTitleClickListener)
            holder.txtRemark!!.setOnClickListener(mOnTitleClickListener)

            // delete button only for maxId (there remains a problem with a large section list)
            if (sectionId == maxId && maxId != 1) {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_delete)
                holder.deleteSection!!.setOnClickListener(mOnDeleteClickListener)

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "120, getView, Id = $sectionId, maxId = $maxId")
            } else {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_nodelete)
                holder.deleteSection!!.setOnClickListener(mOnNoDeleteClickListener)

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.d(TAG, "126, getView, Id = $sectionId, not maxId = $maxId")
            }
            sectionsListRow?.tag = holder
        } else {
            holder = sectionsListRow.tag as SectionHolder
        }

        holder.txtTitle!!.tag = section
        holder.txtRemark!!.tag = section
        holder.txtDate!!.tag = section
        holder.deleteSection!!.tag = section

        // Color section name (button text) blue for GPS-determined section
        // Use GPS-sectionId from global variable sectionIdGPS to mark section in list
        if (sectionIdGPS > 0 && sectionId == sectionIdGPS) {
            val sName = section.name
            holder.txtTitle!!.text = HtmlCompat.fromHtml(
                "<font color='#4088FF'>$sName</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else
            holder.txtTitle!!.text = section.name // show section name without color

        // longDate contains Date as Long value
        // hexDate is LongDate as Hex-String
        val longDate = section.datNum()
        val hexDate = java.lang.Long.toHexString(longDate)

        // Provides Date of Section list, if any
        if (hexDate == "0") {
            holder.txtRemark!!.text = ""
            holder.txtDate!!.text = ""
        } else {
            // section.getDateTime fetches date and time as string from created_at
            holder.txtRemark!!.text = mContext.getString(R.string.hintDateTime) // context
            holder.txtDate!!.text = section.dateTime
        }
        return sectionsListRow!!
    }

    // Start counting by clicking on title
    private val mOnTitleClickListener = View.OnClickListener { v ->
        setPrefs()
        soundButton()
        buttonVib()


        sct = v.tag as Section
        sectionNameCurrent = sct!!.name.toString()
        val intent = Intent(context, CountingActivity::class.java)
        intent.putExtra("section_id", sct!!.id)
        mContext.startActivity(intent)
    }

    // Delete section by clicking on delete button
    private val mOnDeleteClickListener = View.OnClickListener { v ->
        sct = v.tag as Section
        val builder = AlertDialog.Builder(mContext)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(
            sct!!.name
                    + ": "
                    + mContext.getString(R.string.confirmDelete)
        ).setCancelable(false).setPositiveButton(
            R.string.deleteButton
        ) { _: DialogInterface?, _: Int ->
            // perform the deleting in SelectSectionActivity
            (mContext as SelectSectionActivity).deleteSection(sct)
        }.setNegativeButton(
            R.string.cancelButton
        ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    // Don't delete
    private val mOnNoDeleteClickListener = View.OnClickListener { v ->
        sct = v.tag as Section
        val builder = AlertDialog.Builder(mContext)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setMessage(
            sct!!.name
                    + ": "
                    + mContext.getString(R.string.informNodelete)
        ).setCancelable(false).setPositiveButton(
            R.string.ok_button
        ) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    private fun soundButton() {
        if (buttonSoundPref) {
            if (rToneP!!.isPlaying) {
                rToneP!!.stop()
                rToneP!!.release()
            }
            rToneP!!.start()
            Handler(Looper.getMainLooper()).postDelayed({
                if (rToneP!!.isPlaying) {
                    rToneP!!.stop()
                }
                rToneP!!.release()
                rToneP = null
            }, 500)
        }
    }

    private fun buttonVib() {
        if (buttonVibPref && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 31) { // S, Android 12
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            200,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
                vibrator.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "SelectSectAdapt"
    }

}


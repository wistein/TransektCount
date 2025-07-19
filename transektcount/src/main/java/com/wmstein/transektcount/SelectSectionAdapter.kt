package com.wmstein.transektcount

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.net.toUri
import com.wmstein.transektcount.database.Section

/***********************************************************
 * SelectSectionAdapter is called from SelectSectionActivity and
 * provides a line in the sections list and
 * starts CountingActivity for selected section.
 * Based on ProjectListAdapter.java by milo on 05/05/2014.
 * Modified for TransektCount by wmstein since 2016-02-18
 * Last edited in Java on 2023-07-05,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2025-05-05
 */
internal class SelectSectionAdapter(
    private val context: Context,
    private val layoutResourceId: Int,
    private val sections: List<Section>, // list of all sections
    private val maxId: Int, // highest section ID
) : ArrayAdapter<Section?>
    (context, layoutResourceId, sections) {
    private val mContext: Context = context
    private var sct: Section? = null

    // preferences
    private var prefs: SharedPreferences? = null
    private var buttonSoundPref = false
    private var buttonVibPref = false
    private var buttonSound: String? = null

    private fun setPrefs() {
        buttonSoundPref = prefs!!.getBoolean("pref_button_sound", false)
        buttonSound = prefs!!.getString("button_sound", null)
        buttonVibPref = prefs!!.getBoolean("pref_button_vib", false)
    }

    private class SectionHolder {
        var txtTitle: TextView? = null
        var txtRemark: TextView? = null
        var txtDate: TextView? = null
        var deleteSection: ImageButton? = null
    }

    private val vibrator = mContext.getSystemService(Vibrator::class.java)

    // Constructor of entries for the sections list per position
    //   may need garbage collection in SelectSectionActivity as RAM may run short
    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var sectionsListRow = convertView
        val holder: SectionHolder
        prefs = TransektCountApplication.getPrefs()
        setPrefs()

        val section = sections[position]
        val sectionId = section.id

        // if there is still no row for the current section list
        if (sectionsListRow == null) {
            val inflater = (context as Activity).layoutInflater
            sectionsListRow = inflater.inflate(layoutResourceId, parent, false)
            holder = SectionHolder()
            holder.txtTitle = sectionsListRow?.findViewById(R.id.txtTitle)
            holder.txtRemark = sectionsListRow?.findViewById(R.id.txtRemark)
            holder.txtDate = sectionsListRow?.findViewById(R.id.txtDate)
            holder.deleteSection = sectionsListRow?.findViewById(R.id.deleteSection)

            holder.txtTitle!!.setOnClickListener(mOnTitleClickListener)
            holder.txtRemark!!.setOnClickListener(mOnTitleClickListener)

            // set an active delete button only for maxId (problem with large section list!)
            if (sectionId == maxId) {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_delete)
                holder.deleteSection!!.setOnClickListener(mOnDeleteClickListener)
                if (MyDebug.DLOG)
                    Log.d(TAG, "97, getView, Id = $sectionId, maxId = $maxId")
            } else {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_nodelete)
                holder.deleteSection!!.setOnClickListener(mOnNoDeleteClickListener)
                if (MyDebug.DLOG)
                    Log.d(TAG, "102, getView, Id = $sectionId, not maxId = $maxId")
            }
            sectionsListRow?.tag = holder
        } else {
            holder = sectionsListRow.tag as SectionHolder
        }
        holder.txtTitle!!.tag = section
        holder.txtRemark!!.tag = section
        holder.txtDate!!.tag = section
        holder.deleteSection!!.tag = section
        holder.txtTitle!!.text = section.name

        // LongDate contains Date as Long value
        // HexDate is LongDate as Hex-String
        val longDate = section.DatNum()
        val hexDate = java.lang.Long.toHexString(longDate)

        // Provides Date of Section list, if any
        if (hexDate == "0") {
            holder.txtRemark!!.text = ""
            holder.txtDate!!.text = ""
        } else {
            // section.getDateTime fetches date and time as string from created_at
            holder.txtRemark!!.text = context.getString(R.string.hintDateTime)
            holder.txtDate!!.text = section.dateTime
        }
        return sectionsListRow!!
    }

    // Start counting by clicking on title
    private val mOnTitleClickListener = View.OnClickListener { v ->
        setPrefs()
        soundButtonSound()

        buttonVib()
        sct = v.tag as Section
        val intent = Intent(getContext(), CountingActivity::class.java)
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
            R.string.cancel
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
            R.string.nodeleteButton
        ) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
    }

    // button sound
    private fun soundButtonSound() {
        if (buttonSoundPref) {
            try {
                val notification: Uri = if (isNotBlank(buttonSound) && buttonSound != null) {
                    buttonSound!!.toUri()
                } else
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    r.stop()
                }, 400)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buttonVib() {
        if (buttonVibPref && vibrator.hasVibrator()) {
            if (SDK_INT >= 31) { // S, Android 12
                if (MyDebug.DLOG) Log.d(TAG, "202, Vibrator >= SDK 31")

                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                if (SDK_INT >= 26) {
                    if (MyDebug.DLOG) Log.d(TAG, "207, Vibrator >= SDK 26")

                    vibrator.vibrate(VibrationEffect.createOneShot(200,
                            VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    if (MyDebug.DLOG) Log.d(TAG, "212 Vibrator < SDK 26")
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
                vibrator.cancel()
            }
        }
    }

    companion object {
        private const val TAG = "SelectSectAdapt"

        /**
         * Checks if a CharSequence is not empty (""), not null and not whitespace only.
         *
         * isNotBlank(null)      = false
         * isNotBlank("")        = false
         * isNotBlank(" ")       = false
         * isNotBlank("bob")     = true
         * isNotBlank("  bob  ") = true
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is not empty and not null and not whitespace
         */
        private fun isNotBlank(cs: CharSequence?): Boolean {
            return !isBlank(cs)
        }

        private fun isBlank(cs: CharSequence?): Boolean {
            val strLen: Int? = cs?.length
            if (cs == null || strLen == 0)
                return true

            for (i in 0 until strLen!!) {
                if (!Character.isWhitespace(cs[i]))
                    return false
            }
            return true
        }
    }

}


package com.wmstein.transektcount

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.wmstein.transektcount.database.Section

/*********************************************************
 * SectionListAdapter is called from ListSectionActivity and
 * provides a line in the sections list.
 * Based on ProjectListAdapter.java by milo on 05/05/2014.
 * Adopted with additions for TransektCount by wmstein since 2016-02-18
 * Last edited in Java on 2023-07-05,
 * converted to Kotlin on 2023-07-17,
 * last edited on 2023-07-17
 */
internal class SectionListAdapter(
    private val context: Context,
    private val layoutResourceId: Int,
    private val sections: List<Section>,
    private val maxId: Int,
) : ArrayAdapter<Section?>(
    context, layoutResourceId, sections
), OnSharedPreferenceChangeListener {
    private val mContext: Context = context
    private var sct: Section? = null

    // preferences
    private var buttonSoundPref = false
    private var buttonVibPref = false
    private var buttonSound: String? = null
    private var prefs: SharedPreferences? = null

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private fun setPrefs() {
        buttonSoundPref = prefs!!.getBoolean("pref_button_sound", false)
        buttonSound = prefs!!.getString("button_sound", null)
        buttonVibPref = prefs!!.getBoolean("pref_button_vib", false)
    }

    private class SectionHolder {
        var txtTitle: TextView? = null
        var txtRemark: TextView? = null
        var txtDate: TextView? = null
        var editSection: ImageButton? = null
        var deleteSection: ImageButton? = null
    }

    // Constructor of an entry for the sections list position
    //   needs garbage collection in ListSectionActivity as RAM may run short
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var sectionsListRow = convertView
        val holder: SectionHolder
        prefs = TransektCountApplication.getPrefs()
        prefs!!.registerOnSharedPreferenceChangeListener(this)
        setPrefs()
        val section = sections[position]
        val sectionId = section.id

        // if there is still no list row for the current section
        if (sectionsListRow == null) {
            val inflater = (context as Activity).layoutInflater
            sectionsListRow = inflater.inflate(layoutResourceId, parent, false)
            holder = SectionHolder()
            holder.txtTitle = sectionsListRow.findViewById(R.id.txtTitle)
            holder.txtRemark = sectionsListRow.findViewById(R.id.txtRemark)
            holder.txtDate = sectionsListRow.findViewById(R.id.txtDate)
            holder.editSection = sectionsListRow.findViewById(R.id.editSection)
            holder.deleteSection = sectionsListRow.findViewById(R.id.deleteSection)
            holder.txtTitle!!.setOnClickListener(mOnTitleClickListener)
            holder.txtRemark!!.setOnClickListener(mOnTitleClickListener)
            holder.editSection!!.setOnClickListener(mOnEditClickListener)

            // only for maxId set an active delete button
            if (sectionId == maxId) {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_delete)
                holder.deleteSection!!.setOnClickListener(mOnDeleteClickListener)
                if (MyDebug.LOG) {
                    Log.e(TAG, "Id = $sectionId, maxId = $maxId")
                }
            } else {
                holder.deleteSection!!.setImageResource(R.drawable.ic_menu_nodelete)
                holder.deleteSection!!.setOnClickListener(mOnNodeleteClickListener)
                if (MyDebug.LOG) {
                    Log.e(TAG, "Id = $sectionId, not maxId = $maxId")
                }
            }
            sectionsListRow.tag = holder
        } else {
            holder = sectionsListRow.tag as SectionHolder
        }
        holder.txtTitle!!.tag = section
        holder.txtRemark!!.tag = section
        holder.txtDate!!.tag = section
        holder.editSection!!.tag = section
        holder.deleteSection!!.tag = section
        holder.txtTitle!!.text = section.name
        holder.txtRemark!!.text = section.notes

        // LongDate contains Date as Long value
        // HexDate is LongDate as Hex-String
        val longDate = section.DatNum()
        val hexDate = java.lang.Long.toHexString(longDate)

        // Provides Date of Section list, if any
        if (hexDate == "0") {
            holder.txtDate!!.text = ""
        } else {
            // section.getDateTime fetches date and time as string from created_at
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

    // Edit section by clicking on edit button
    private val mOnEditClickListener = View.OnClickListener { v ->
        setPrefs()
        soundButtonSound()
        buttonVib()
        sct = v.tag as Section

        // Store section_id into SharedPreferences.
        // That makes sure that the current selected section can be retrieved
        // by EditSectionActivity when returning from AddSpeciesActivity
        val editor = prefs!!.edit()
        editor.putInt("section_id", sct!!.id)
        editor.commit()
        if (MyDebug.LOG) Log.e(TAG, "Sect Id = " + sct!!.id)
        val intent = Intent(getContext(), EditSectionActivity::class.java)
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
            // perform the deleting in the activity
            (mContext as ListSectionActivity).deleteSection(sct)
        }.setNegativeButton(
            R.string.cancel
        ) { dialog: DialogInterface, _: Int -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    // Don't delete
    private val mOnNodeleteClickListener = View.OnClickListener { v ->
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
                    Uri.parse(buttonSound)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
                val r = RingtoneManager.getRingtone(getContext(), notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buttonVib() {
        if (buttonVibPref) {
            try {
                val vibrator: Vibrator
                val vibratorManager: VibratorManager
                if (Build.VERSION.SDK_INT >= 31) {
                    vibratorManager =
                        mContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    vibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                100,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            } catch (e: java.lang.Exception) {
                if (MyDebug.LOG) Log.e(TAG, "could not vibrate.", e)
            }
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        setPrefs()
    }

    companion object {
        private const val TAG = "transektSectListAdapt"

        /**
         * Checks if a CharSequence is not empty (""), not null and not whitespace only.
         *
         *
         * isNotBlank(null)      = false
         * isNotBlank("")        = false
         * isNotBlank(" ")       = false
         * isNotBlank("bob")     = true
         * isNotBlank("  bob  ") = true
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is
         * not empty and not null and not whitespace
         */
        private fun isNotBlank(cs: CharSequence?): Boolean {
            return !isBlank(cs)
        }

        /**
         * Following functions are taken from the Apache commons-lang3-3.4 library
         * licensed under Apache License Version 2.0, January 2004
         *
         *
         * Checks if a CharSequence is whitespace, empty ("") or null
         *
         *
         * isBlank(null)      = true
         * isBlank("")        = true
         * isBlank(" ")       = true
         * isBlank("bob")     = false
         * isBlank("  bob  ") = false
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is null, empty or whitespace
         */
        private fun isBlank(cs: CharSequence?): Boolean {
            val strLen: Int? = cs?.length
            if (cs == null || strLen == 0) {
                return true
            }
            for (i in 0 until strLen!!) {
                if (!Character.isWhitespace(cs[i])) {
                    return false
                }
            }
            return true
        }
    }
}
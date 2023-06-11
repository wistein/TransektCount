package com.wmstein.transektcount;

import static android.content.Context.VIBRATOR_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.wmstein.transektcount.database.Section;

import java.util.List;

import androidx.annotation.NonNull;

import static java.lang.Long.toHexString;

/*********************************************************
 * SectionListAdapter is called from ListSectionLActivity
 * Based on ProjectListAdapter.java by milo on 05/05/2014.
 * Adopted with additions for TransektCount by wmstein since 2016-02-18
 * Last edited on 2023-06-09
 */
class SectionListAdapterL extends ArrayAdapter<Section> implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "transektSectListAdapt";
    private final Context context;
    private final int layoutResourceId;
    private final List<Section> sections;
    private final Context mContext;
    private Section sct;

    // preferences
    private boolean buttonSoundPref;
    private boolean buttonVibPref;
    private String buttonSound;
    private SharedPreferences prefs;

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false); // make button sound
        buttonSound = prefs.getString("button_sound", null); // use standard button sound
        buttonVibPref = prefs.getBoolean("pref_button_vib", false); // make vibration
    }

    // Constructor
    SectionListAdapterL(Context context, int layoutResourceId, List<Section> sections)
    {
        super(context, layoutResourceId, sections);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.sections = sections;
        mContext = context;
    }

    private static class SectionHolder
    {
        TextView txtTitle;
        TextView txtRemark;
        TextView txtDate;
        ImageButton editSection;
        ImageButton deleteSection;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        View row = convertView;
        SectionHolder holder;

        prefs = TransektCountApplication.getPrefs();
        prefs.registerOnSharedPreferenceChangeListener(this);
        getPrefs();

        if (row == null)
        {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SectionHolder();
            holder.txtTitle = row.findViewById(R.id.txtTitle);
            holder.txtRemark = row.findViewById(R.id.txtRemark);
            holder.txtDate = row.findViewById(R.id.txtDate);
            holder.editSection = row.findViewById(R.id.editSection);
            holder.deleteSection = row.findViewById(R.id.deleteSection);

            holder.txtTitle.setOnClickListener(mOnTitleClickListener);
            holder.txtRemark.setOnClickListener(mOnTitleClickListener);
            holder.editSection.setOnClickListener(mOnEditClickListener);
            holder.deleteSection.setOnClickListener(mOnDeleteClickListener);

            row.setTag(holder);
        }
        else
        {
            holder = (SectionHolder) row.getTag();
        }

        Section section = sections.get(position);
        holder.txtTitle.setTag(section);
        holder.txtRemark.setTag(section);
        holder.txtDate.setTag(section);
        holder.editSection.setTag(section);
        holder.deleteSection.setTag(section);
        holder.txtTitle.setText(section.name);
        holder.txtRemark.setText(section.notes);

        // LongDate contains Date as Long value
        // HexDate is LongDate as Hex-String  
        Long LongDate = section.DatNum();
        String HexDate = toHexString(LongDate);

        // Provides Date of Section list, if any      
        if (HexDate.equals("0"))
        {
            holder.txtDate.setText("");
        }
        else
        {
            // section.getDateTime fetches date and time as string from created_at 
            holder.txtDate.setText(section.getDateTime());
        }
        return row;
    }

    // Start counting by clicking on title
    private final View.OnClickListener mOnTitleClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            getPrefs();
            soundButtonSound();
            buttonVib();

            sct = (Section) v.getTag();
            Intent intent = new Intent(getContext(), CountingLActivity.class);
            intent.putExtra("section_id", sct.id);
            mContext.startActivity(intent);
        }
    };

    // Edit section by clicking on edit button
    private final View.OnClickListener mOnEditClickListener = new View.OnClickListener()
    {
        @SuppressLint({"LongLogTag", "ApplySharedPref"})
        @Override
        public void onClick(final View v)
        {
            getPrefs();
            soundButtonSound();
            buttonVib();

            sct = (Section) v.getTag();
            
            // Store section_id into SharedPreferences.
            // That makes sure that the current selected section can be retrieved 
            // by EditSectionActivity when returning from AddSpeciesActivity
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("section_id", sct.id);
            editor.commit();
            if (MyDebug.LOG)
                Log.e(TAG, "Sect Id = " + sct.id);

            Intent intent = new Intent(getContext(), EditSectionLActivity.class);
            mContext.startActivity(intent);
        }
    };

    // Delete section by clicking on delete button
    private final View.OnClickListener mOnDeleteClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sct = (Section) v.getTag();
            // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
            // could make the dialog central in the popup - to do later
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(sct.name 
                + ": " 
                + mContext.getString(R.string.confirmDelete)).setCancelable(false).setPositiveButton(R.string.deleteButton, 
                (dialog, id) -> 
                {
                // perform the deleting in the activity
                ((ListSectionActivity) mContext).deleteSection(sct);
                }
            ).setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel()
            );
            AlertDialog alert = builder.create();
            alert.show();
        }
    };

    // button sound
    private void soundButtonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonSound) && buttonSound != null)
                {
                    notification = Uri.parse(buttonSound);
                }
                else
                {
                    notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                r.play();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void buttonVib()
    {
        if (buttonVibPref)
        {
            try
            {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(150);
                }
            } catch (Exception e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "could not vibrate.", e);
            }
        }
    }

    /**
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     <p>
     * isNotBlank(null)      = false
     * isNotBlank("")        = false
     * isNotBlank(" ")       = false
     * isNotBlank("bob")     = true
     * isNotBlank("  bob  ") = true
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace
     */
    private static boolean isNotBlank(final CharSequence cs)
    {
        return !isBlank(cs);
    }

    /**
     * Following functions are taken from the Apache commons-lang3-3.4 library
     * licensed under Apache License Version 2.0, January 2004
     <p>
     * Checks if a CharSequence is whitespace, empty ("") or null
     <p>
     * isBlank(null)      = true
     * isBlank("")        = true
     * isBlank(" ")       = true
     * isBlank("bob")     = false
     * isBlank("  bob  ") = false
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     */
    private static boolean isBlank(final CharSequence cs)
    {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0)
        {
            return true;
        }
        for (int i = 0; i < strLen; i++)
        {
            if (!Character.isWhitespace(cs.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        getPrefs();
    }

}
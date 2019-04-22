package com.wmstein.transektcount;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.wmstein.transektcount.database.Section;

import java.util.List;

import static java.lang.Long.toHexString;

/*********************************************************
 * SectionListAdapter is called from ListSectionActivity
 * Based on ProjectListAdapter.java by milo on 05/05/2014.
 * Adopted with additions for TransektCount by wmstein since 2016-02-18
 * Last edited on 2019-02-22
 */
class SectionListAdapter extends ArrayAdapter<Section> implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "transektcountSectListAdapt";
    private Context context;
    private int layoutResourceId;
    private List<Section> sections;
    private Context mContext;
    private Section sct;

    // preferences
    private boolean buttonSoundPref;
    private String buttonAlertSound;
    private SharedPreferences prefs;
    private boolean screenOrientL; // option for screen orientation

    /*
     * So preferences can be loaded at the start, and also when a change is detected.
     */
    private void getPrefs()
    {
        buttonSoundPref = prefs.getBoolean("pref_button_sound", false);
        buttonAlertSound = prefs.getString("alert_button_sound", null);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

    // Constructor
    SectionListAdapter(Context context, int layoutResourceId, List<Section> sections)
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
            // section.getDate fetches date as string from created_at 
            holder.txtDate.setText(section.getDate());
        }
        return row;
    }

    // Start counting by clicking on title
    private View.OnClickListener mOnTitleClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            getPrefs();
            buttonSound();

            if (screenOrientL)
            {
                sct = (Section) v.getTag();
                Intent intent = new Intent(getContext(), CountingLActivity.class);
                intent.putExtra("section_id", sct.id);
                mContext.startActivity(intent);
            }
            else
            {
                sct = (Section) v.getTag();
                Intent intent = new Intent(getContext(), CountingActivity.class);
                intent.putExtra("section_id", sct.id);
                mContext.startActivity(intent);
            }
        }
    };

    // Edit section by clicking on edit button
    private View.OnClickListener mOnEditClickListener = new View.OnClickListener()
    {
        @SuppressLint({"LongLogTag", "ApplySharedPref"})
        @Override
        public void onClick(final View v)
        {
            getPrefs();
            buttonSound();

            sct = (Section) v.getTag();
            
            // Store section_id into SharedPreferences.
            // That makes sure that the current selected section can be retrieved 
            // by EditSectionActivity when returning from AddSpeciesActivity
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("section_id", sct.id);
            editor.commit();
            if (MyDebug.LOG)
                Log.e(TAG, "Sect Id = " + sct.id);

            Intent intent = new Intent(getContext(), EditSectionActivity.class);
            mContext.startActivity(intent);
        }
    };

    // Delete section by clicking on delete button
    private View.OnClickListener mOnDeleteClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sct = (Section) v.getTag();
            // http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
            // could make the dialog central in the popup - to do later
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(sct.name + ": " + mContext.getString(R.string.confirmDelete)).setCancelable(false).setPositiveButton(R.string.deleteButton, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        // perform the deleting in the activity
                        ((ListSectionActivity) mContext).deleteSection(sct);
                    }
                }
            ).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                }
            );
            AlertDialog alert = builder.create();
            alert.show();
        }
    };

    // button sound
    private void buttonSound()
    {
        if (buttonSoundPref)
        {
            try
            {
                Uri notification;
                if (isNotBlank(buttonAlertSound) && buttonAlertSound != null)
                {
                    notification = Uri.parse(buttonAlertSound);
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

    /**
     * Checks if a CharSequence is not empty (""), not null and not whitespace only.
     * <p/>
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
     * <p>
     * Checks if a CharSequence is whitespace, empty ("") or null
     * <p/>
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
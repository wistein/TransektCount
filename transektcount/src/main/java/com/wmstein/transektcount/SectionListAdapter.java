package com.wmstein.transektcount;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.wmstein.transektcount.database.Section;

import java.util.List;

import static java.lang.Long.toHexString;

/**
 * Based on ProjectListAdapter.java by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */

public class SectionListAdapter extends ArrayAdapter<Section>
{
    private static final String TAG = "transektcountSectionListAdapter";
    Context context;
    int layoutResourceId;
    List<Section> sections = null;
    private Context mContext;
    private TransektCountApplication transektCount;
    private Section sct;
    private Handler mHandler = new Handler();


    // Constructor
    public SectionListAdapter(Context context, int layoutResourceId, List<Section> sections)
    {
        super(context, layoutResourceId, sections);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.sections = sections;
        mContext = context;
        transektCount = (TransektCountApplication) context.getApplicationContext();
    }

    static class SectionHolder
    {
        TextView txtTitle;
        TextView txtRemark;
        TextView txtDate;
        ImageButton deleteSection;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        SectionHolder holder;

        if (row == null)
        {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SectionHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.txtTitle);
            holder.txtRemark = (TextView) row.findViewById(R.id.txtRemark);
            holder.txtDate = (TextView) row.findViewById(R.id.txtDate);
            holder.deleteSection = (ImageButton) row.findViewById(R.id.deleteSection);

            holder.txtTitle.setOnClickListener(mOnTitleClickListener);
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
        holder.deleteSection.setTag(section);
        holder.txtTitle.setText(section.name);
        holder.txtRemark.setText(section.notes);

        // Meldung contains Date as Long value
        // Meld is Meldung as Hex-String  
        Long Meldung = section.DatNum();
        String Meld = toHexString(Meldung);

        // Provides Date of Section list, if any      
        if (Meld.equals("0"))
        {
            holder.txtDate.setText("");
        }
        else
        {
            // section.getDate Holt Datum als String aus created_at 
            holder.txtDate.setText(section.getDate());
        }
        return row;
    }

    /*
     * Start counting by clicking on title, delete by clicking on button.
     */
    private View.OnClickListener mOnTitleClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            Toast.makeText(mContext, mContext.getString(R.string.wait), Toast.LENGTH_SHORT).show();

            // pause for 100 msec to show toast immediately
            mHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    sct = (Section) v.getTag();
                    Intent intent = new Intent(getContext(), CountingActivity.class);
                    intent.putExtra("section_id", sct.id);
                    //transektCount.section_id = sct.id;
                    mContext.startActivity(intent);
                }
            }, 100);
        }
    };

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
            ).setNegativeButton(R.string.cancelButton, new DialogInterface.OnClickListener()
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
}

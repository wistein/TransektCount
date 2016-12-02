package com.wmstein.transektcount.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmstein.transektcount.AutoFitText;
import com.wmstein.transektcount.R;
import com.wmstein.transektcount.database.Count;

import java.lang.reflect.Field;

/****************************************************
 * Created by milo on 25/05/2014.
 * Adopted for TransektCount by wmstein on 18.02.2016
 */
public class CountingWidget extends RelativeLayout
{
    public static String TAG = "transektcountCountingWidget";

    private TextView countName;
    private ImageView pSpecies;
    private AutoFitText countCount;
    private AutoFitText countCounta;

    public Count count;
    

    public CountingWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_counting, this, true);
        countCount = (AutoFitText) findViewById(R.id.countCount);
        countCounta = (AutoFitText) findViewById(R.id.countCounta);
        countName = (TextView) findViewById(R.id.countName);
        pSpecies = (ImageView) findViewById(R.id.pSpecies);
    }

    public void setCount(Count newcount)
    {
        count = newcount;
        String rname = "p" + count.code; // species picture resource name
        
        int resId = getResId(rname);
        //Log.i(TAG, "rname = " + rname);
        //Log.i(TAG, "resId = " + String.valueOf(resId));
        if (resId != 0)
        {
            pSpecies.setImageResource(resId);
        }
        
        countName.setText(count.name);
        countCount.setText(String.valueOf(count.count));
        countCounta.setText(String.valueOf(count.counta));
        ImageButton countUpButton = (ImageButton) findViewById(R.id.buttonUp);
        countUpButton.setTag(count.id);
        ImageButton countUpButtona = (ImageButton) findViewById(R.id.buttonUpa);
        countUpButtona.setTag(count.id);
        ImageButton countDownButton = (ImageButton) findViewById(R.id.buttonDown);
        countDownButton.setTag(count.id);
        ImageButton countDownButtona = (ImageButton) findViewById(R.id.buttonDowna);
        countDownButtona.setTag(count.id);
        ImageButton editButton = (ImageButton) findViewById(R.id.buttonEdit);
        editButton.setTag(count.id);
    }

    public void countUp()
    {
        count.increase();
        countCount.setText(String.valueOf(count.count));
    }

    public void countDown()
    {
        count.safe_decrease();
        countCount.setText(String.valueOf(count.count));
    }

    public void countUpa()
    {
        count.increasea();
        countCounta.setText(String.valueOf(count.counta));
    }

    public void countDowna()
    {
        count.safe_decreasea();
        countCounta.setText(String.valueOf(count.counta));
    }
    
    // Get resource ID from resource name
    public int getResId(String rName) 
    {
        try 
        {
            Class res = R.drawable.class;
            Field idField = res.getField(rName);
            return idField.getInt(null);
        } catch (Exception e) 
        {
            return 0;
        }
    }
}

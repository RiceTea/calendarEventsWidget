package com.benny.calendarEvents;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by benny on 17/03/15.
 */
public class GlobalFunctions {
  Context mContext;
  String DatesPreferences = "DatesPreferences";
  String useDefault = "useDefaultDateRange";

  Calendar calStart;
  Calendar calEnd;

  SharedPreferences prefs;


  // constructor
  public GlobalFunctions(Context context){
    this.mContext = context;
//    showToast(context, "GlobalFunctions constructor called", 0, 1);
  }



  public Object[] getDateRange() {
    prefs = mContext.getSharedPreferences(DatesPreferences, Context.MODE_PRIVATE);
    boolean isDefault = prefs.getBoolean(useDefault, true);
//    Toast t = Toast.makeText(mContext,"getDateRange called",Toast.LENGTH_LONG); t.show();

    if (isDefault) {
      // set today for one year if using default preference
      calStart = Calendar.getInstance();
      calEnd = Calendar.getInstance();
      calEnd.add(Calendar.DATE, 52 * 7); // add a year
    } else {
      // otherwise, get date range from preferences
      int tempYear = prefs.getInt("startingYear", 2015);
      int tempMonth = prefs.getInt("startingMonth", 1);
      int tempDay = prefs.getInt("startingDay", 1);
      calStart = Calendar.getInstance();
      calStart.set(tempYear, tempMonth, tempDay);

      tempYear = prefs.getInt("endingYear", 2016);
      tempMonth = prefs.getInt("endingMonth", 11);
      tempDay = prefs.getInt("endingDay", 31);
      calEnd = Calendar.getInstance();
      calEnd.set(tempYear, tempMonth, tempDay);
    }

    return new Object[]{calStart, calEnd};

  }  // end of getDateRange


  // duration is parsed in seconds
  protected void showToast(Context context,String toastMessage, int colour, int duration) {
    Context mContext = context;
    int xOffset = 50; int yOffset = 450;

    LayoutInflater mInflater;
    mInflater = LayoutInflater.from(mContext);
    // get my custom toast layout
    View layout = mInflater.inflate(R.layout.customtoast, null);
    // following works but destroys border
//    layout.setBackgroundColor(colour);

    TextView text = (TextView) layout.findViewById(R.id.toastText);
    text.setText(toastMessage);
    // so far, following is not working
    LayerDrawable bubble = (LayerDrawable) mContext.getResources().getDrawable(R.drawable.layer_list);
    GradientDrawable outerRect = (GradientDrawable) bubble.findDrawableByLayerId(R.id.outerRectangle);
    outerRect.setColor(Color.parseColor("#0faf02"));
    Toast toast = new CustomToast(mContext);

    toast.setDuration(duration); // show for nnn seconds
    toast.setGravity(Gravity.TOP | Gravity.LEFT, xOffset, yOffset);
    toast.setView(layout);
    toast.show();
  }  // end of showToast


}

/*
Copyright 2014 Yahoo Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.benny.calendarEvents;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.Toast;

import java.util.Calendar;


public class DateRangePreferences extends Activity {

  DatePicker startDate;
  DatePicker endDate;
  String DatesPreferences = "DatesPreferences";
  String useDefaultDateRange = "useDefaultDateRange";
  String useCalendarGrouping = "useCalendarGrouping";
  Button startDateButton, endDateButton;
  CheckBox chkDefaultDateRange, chkCalendarGrouping;


  @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datetimepicker);
    final Context mContext = getApplicationContext();
    final SharedPreferences prefs;

    chkCalendarGrouping = (CheckBox) findViewById(R.id.chkGroupByCalendars);
    chkDefaultDateRange = (CheckBox) findViewById(R.id.chkDefaultDateRange);

    prefs = mContext.getSharedPreferences(DatesPreferences, MODE_PRIVATE);


    // ---------------------------------------------------------------------- //
    boolean isCalendarGrouping =   prefs.getBoolean(useCalendarGrouping, false);
    if (isCalendarGrouping) {
      chkCalendarGrouping.setChecked(true);
    }
    else {
      chkCalendarGrouping.setChecked(false);
    }

    chkCalendarGrouping.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View view) {
        final SharedPreferences.Editor editor;
        editor = getSharedPreferences(DatesPreferences, MODE_PRIVATE).edit();

        if (((CheckBox) view).isChecked()) {
          editor.putBoolean(useCalendarGrouping, true);
//                Toast tNo = Toast.makeText(mContext,"CheckBox --yes--",Toast.LENGTH_LONG); tNo.show();
        }
        else {
          editor.putBoolean(useCalendarGrouping, false);
//          Toast tNo = Toast.makeText(mContext,"CheckBox --no--",Toast.LENGTH_LONG); tNo.show();
        }
        editor.commit();
      }
    });


    // ---------------------------------------------------------------------- //
    boolean isDefaultDateRange =   prefs.getBoolean(useDefaultDateRange, true);
    if (isDefaultDateRange) {
      setDefault(mContext);
      chkDefaultDateRange.setChecked(true);
    }
    else {
      setDates(mContext);
      chkDefaultDateRange.setChecked(false);
    }

    chkDefaultDateRange.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View view) {
    final SharedPreferences.Editor editor;
    editor = getSharedPreferences(DatesPreferences, MODE_PRIVATE).edit();

    if (((CheckBox) view).isChecked()) {
      setDefault(mContext);
      editor.putBoolean(useDefaultDateRange, true);
      }
    else {
      setDates(mContext);
      editor.putBoolean(useDefaultDateRange, false);
    }
    editor.commit();
      }
    });






    startDateButton = (Button) findViewById(R.id.changeStartDate);
    startDateButton.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View view) {
        int year = startDate.getYear();
        int month = startDate.getMonth();
        int day = startDate.getDayOfMonth();
        final SharedPreferences.Editor editor;
      editor = getSharedPreferences(DatesPreferences, MODE_PRIVATE).edit();
      editor.putInt("startingYear", year);
      editor.putInt("startingMonth", month);
      editor.putInt("startingDay", day);
      editor.commit();
        }
      });

    endDateButton = (Button) findViewById(R.id.changeEndDate);
    endDateButton.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View view) {
        int year = endDate.getYear();
        int month = endDate.getMonth();
        int day = endDate.getDayOfMonth();
        final SharedPreferences.Editor editor;
        editor = getSharedPreferences(DatesPreferences, MODE_PRIVATE).edit();
        editor.putInt("endingYear", year);
        editor.putInt("endingMonth", month);
        editor.putInt("endingDay", day);
        editor.commit();
        }
      });


    }  // end of onCreate

public void setDefault(Context mContext) {
  Calendar calStart = Calendar.getInstance();
  Calendar calEnd = Calendar.getInstance();
  calEnd.add(Calendar.DATE, 52*7); // add a year
  int tempStartDay =  calStart.get(Calendar.DAY_OF_WEEK);
  int tempStartMonth = calStart.get(Calendar.MONTH);
  int tempStartYear = calStart.get(Calendar.YEAR);

  int tempEndDay =  calEnd.get(Calendar.DAY_OF_WEEK);
  int tempEndMonth = calEnd.get(Calendar.MONTH);
  int tempEndYear = calEnd.get(Calendar.YEAR);
  startDate = (DatePicker) findViewById(R.id.start_datepicker);
  startDate.updateDate(tempStartYear, tempStartMonth, tempStartDay);
  endDate = (DatePicker) findViewById(R.id.end_datepicker);
  endDate.updateDate(tempEndYear, tempEndMonth, tempEndDay);
}

  public void setDates(Context mContext) {
    final SharedPreferences prefs;
    prefs = mContext.getSharedPreferences(DatesPreferences, MODE_PRIVATE);
    int tempStartYear =  prefs.getInt("startingYear", 2015);
    int tempStartMonth =  prefs.getInt("startingMonth", 1);
    int tempStartDay =  prefs.getInt("startingDay", 1);

    int tempEndYear =  prefs.getInt("endingYear", 2016);
    int tempEndMonth =  prefs.getInt("endingMonth", 11);
    int tempEndDay =  prefs.getInt("endingDay", 30);

    startDate = (DatePicker) findViewById(R.id.start_datepicker);
    startDate.updateDate(tempStartYear, tempStartMonth, tempStartDay);
    endDate = (DatePicker) findViewById(R.id.end_datepicker);
    endDate.updateDate(tempEndYear, tempEndMonth, tempEndDay);
  }

}


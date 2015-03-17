package com.benny.calendarEvents;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class WidgetService extends RemoteViewsService {
  @Override
  public RemoteViewsFactory onGetViewFactory(Intent intent) {
    return(new WidgetDisplay(this.getApplicationContext(), intent));
  }
}  // end of WidgetService



class WidgetDisplay implements RemoteViewsService.RemoteViewsFactory {
  private static final String tag="bennyCalendarEvents";
  private Context mContext;
  private int mAppWidgetId;
  private int numberOfItems = 5;
  int requestCode1 = 1;
  public static  long milliseconds = 60*60*1000;     // one hour
  Boolean once = false;
  boolean isDefaultDateRange;
  boolean isCalendarMajor;
  String calendarIDKey = "6";
  Calendar calStart;
  Calendar calEnd;


  SharedPreferences prefs;

  String dateRangeString;
  String DatesPreferences = "DatesPreferences";
  String useDefault = "useDefaultDateRange";
  String useCalendarGrouping = "useCalendarGrouping";

  String aFewSpaces = "&nbsp;&nbsp;&nbsp;&nbsp;";
  String someSpaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
  String headerString = "<!--header-->";

  List<Spanned> mCollections = new ArrayList<>();
  HashMap<String, String> calendarMap =  new HashMap<>();
  HashMap<String, Integer> calendarColourMap =  new HashMap<>();


  public WidgetDisplay(Context context, Intent intent) {
    mContext = context;
    mAppWidgetId = intent.getIntExtra( AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

    Log.d(tag, "factory created");


    // add an alarm service
    int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

    Intent alarmIntent = new Intent(mContext, WidgetProvider.class);

    alarmIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    alarmIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
    PendingIntent pintent = PendingIntent.getBroadcast(mContext, requestCode1, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    AlarmManager alarm = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);

    if (!once) {
      once = true;
      alarm.setRepeating(AlarmManager.RTC_WAKEUP, milliseconds, milliseconds, pintent);
    }

    // initial list array display
    onDataSetChanged();

  }


  //Called when your factory is first constructed.
  //The same factory may be shared across multiple
  //RemoteViewAdapters depending on the intent passed.
  public void onCreate()
  {
    Log.d(tag,"onCreate called for widget id:" + mAppWidgetId);
  }

  //Called when the last RemoteViewsAdapter that is
  //associated with this factory is unbound.
  public void onDestroy()
  {
    Log.d(tag,"destroy called for widget id:" + mAppWidgetId);
  }

  //The total number of items in this list
  public int getCount()
  {
    return numberOfItems;
  }


  public RemoteViews getViewAt(int position) {
    Spanned span = mCollections.get(position);
    RemoteViews rvRow;
    String temp = span.toString();

    rvRow = new RemoteViews(mContext.getPackageName(), R.layout.dark_row);
    rvRow.setTextViewText(R.id.widget_item, span);

    // use the correct calendar colour for the background of the calendar names
    if (temp.contains("---")) {
      int startIndex = temp.indexOf("---");
      int endIndex = startIndex+3+temp.substring(startIndex+3).indexOf("---");
      int calendarColour =  calendarColourMap.get(temp.substring(startIndex+3, endIndex));
      // using opacity of 112
      rvRow.setInt(R.id.widget_item, "setBackgroundColor",  Color.parseColor(String.format("#%06X", (0x70FFFFFF & calendarColour))));
    }
    else {
      rvRow.setInt(R.id.widget_item, "setBackgroundColor",  Color.argb(54,161,162,156));
    }

    this.loadItemOnClickExtras(rvRow, position);
    return rvRow;
  }


  private void loadItemOnClickExtras(RemoteViews rv, int position) {
    Intent showHeader = new Intent();
    showHeader.putExtra(WidgetProvider.EXTRA_DATE_RANGE,""+dateRangeString);
    rv.setOnClickFillInIntent(R.id.date_range, showHeader);

    Intent ei = new Intent();

    Spanned span = mCollections.get(position);

    ei.putExtra(WidgetProvider.EXTRA_LIST_ITEM_TEXT,""+span);

    rv.setOnClickFillInIntent(R.id.widget_item, ei);
  }

  //This allows for the use of a custom loading view
  //which appears between the time that getViewAt(int)
  //is called and returns. If null is returned,
  //a default loading view will be used.
  public RemoteViews getLoadingView()
  {
    return null;
  }

  //Not sure how this matters.
  //How many different types of views
  //are there in this list.
  public int getViewTypeCount()
  {
    return 1;
  }

  //The internal id of the item
  //at this position
  public long getItemId(int position)
  {
    return position;
  }

  //True if the same id
  //always refers to the same object.
  public boolean hasStableIds()  { return true; }

  //Called when notifyDataSetChanged() is triggered
  //on the remote adapter. This allows a RemoteViewsFactory
  //to respond to data changes by updating
  //any internal references.
  //Note: expensive tasks can be safely performed
  //synchronously within this method.
  //In the interim, the old data will be displayed
  //within the widget.

  // used an asynchronous task and it caused all sorts of problems
  // then did the calendar calls synchronously and it works!

  // ----------------------------------------------------------------------- //
  //                           onDataSetChanged                              //
  // ----------------------------------------------------------------------- //
  public void onDataSetChanged() {
    prefs = mContext.getSharedPreferences(DatesPreferences, Context.MODE_PRIVATE);
    isDefaultDateRange =   prefs.getBoolean(useDefault, true);
    isCalendarMajor =   prefs.getBoolean(useCalendarGrouping, false);


    mCollections.clear();
    calendarMap.clear();
    calendarColourMap.clear();

    if (isDefaultDateRange) {
      // set today for one year if using default preference
      calStart = Calendar.getInstance();
      calEnd = Calendar.getInstance();
      calEnd.add(Calendar.DATE, 52*7); // add a year
    }
    else {
      int tempYear = prefs.getInt("startingYear", 2015);
      int tempMonth = prefs.getInt("startingMonth", 0);
      int tempDay = prefs.getInt("startingDay", 1);
      calStart = Calendar.getInstance();
      calStart.set(tempYear, tempMonth, tempDay);

      tempYear = prefs.getInt("endingYear", 2017);
      tempMonth = prefs.getInt("endingMonth", 11);
      tempDay = prefs.getInt("endingDay", 31);
      calEnd = Calendar.getInstance();
      calEnd.set(tempYear, tempMonth, tempDay);
    }

    if (isCalendarMajor) {
      ContentResolver contentResolver = mContext.getContentResolver();
      Cursor calendarCursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, null, null, null, null);
      while (calendarCursor.moveToNext()) {
        int calendarIDIndex = calendarCursor.getColumnIndex(CalendarContract.Calendars._ID);
        String calendarID = calendarCursor.getString(calendarIDIndex);
        int calendarIndex = calendarCursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
        String calendar = calendarCursor.getString(calendarIndex);
        int calendarColourIndex = calendarCursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR);
        int calendarColour = calendarCursor.getInt(calendarColourIndex);
        // add to a dictionary skipping unwanted entries
        if (calendar.contains("@")) { continue; }
        if (calendar.contains("Birthdays")) { continue; }
        calendarIDKey = calendarID;
        calendarMap.put(calendarID, calendar);
        calendarColourMap.put(calendar, calendarColour);
        loopThroughRows();
      }
      calendarCursor.close();
    }
    else
      {loopThroughRows();}

  } // end of onDataSetChanged


  // ----------------------------------------------------------------------- //
  //                           loopThroughRows                               //
  // ----------------------------------------------------------------------- //
  public void loopThroughRows() {
    ContentResolver contentResolver = mContext.getContentResolver();
    Cursor cursor;
    String stringDateStartInMillseconds = Long.toString(calStart.getTimeInMillis());
    String stringDateEndinMilliseconds = Long.toString(calEnd.getTimeInMillis());
    String[] selectionArgs;
    Spanned spanCalendars;
    String spanCalendarsString;

    String selection;
    String datesSelection    = "( (" + CalendarContract.Events.DTSTART + " >= ?) AND (" + CalendarContract.Events.DTEND + " <= ?) )";
    String calendarSelection = "( (" + CalendarContract.Events.DTSTART + " >= ?) AND (" + CalendarContract.Events.DTEND + " <= ?) AND (" + CalendarContract.Events.CALENDAR_ID + " = ?) )";


    if (isCalendarMajor) {
      selection = calendarSelection;
      selectionArgs = new String[]{stringDateStartInMillseconds, stringDateEndinMilliseconds, calendarIDKey};
    } else {
      selection = datesSelection;
      selectionArgs = new String[]{stringDateStartInMillseconds, stringDateEndinMilliseconds};
    }


    // get the calendar rows in start date sequence
    cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI,  null, selection, selectionArgs, "dtstart ASC");
    // do not create empty list rows
    if (cursor.getCount() ==0) {return; }

    // only output calendar name when there are rows
    if (isCalendarMajor) {
      spanCalendarsString = headerString + "<br><b><font color='#0f0f0f0f'>" + aFewSpaces + "---" +calendarMap.get(calendarIDKey) + "---</font>";
      spanCalendars = Html.fromHtml(spanCalendarsString);
      mCollections.add(spanCalendars);
    }

    Spanned span;
    String lastStartDate = "";
    String spanStr;

    Date dateStart = calStart.getTime();
    SimpleDateFormat dmyFormatter = new SimpleDateFormat("dd MMM yyyy");
    Date dateEnd = calEnd.getTime();
    String stringDateStart = dmyFormatter.format(dateStart);
    String stringDateEnd = dmyFormatter.format(dateEnd);
    spanStr = "<b><font color='#0fafac'>" + stringDateStart + " to xxxxxxxx" + stringDateEnd +"</font>";
    span = Html.fromHtml(spanStr);
    final RemoteViews rvWidget = new RemoteViews(mContext.getPackageName(), R.layout.widget);
    rvWidget.setTextViewText(R.id.date_range, span);

    spanStr = "";
    while (cursor.moveToNext()) {
      int titleColumn = cursor.getColumnIndex(CalendarContract.Events.TITLE);
      int descriptionColumn = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION);
      int dtStartColumn = cursor.getColumnIndex(CalendarContract.Events.DTSTART);
      int colourColumn = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR);
      int eventLocation = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);

      String title = cursor.getString(titleColumn);
      String description = cursor.getString(descriptionColumn);
      Date startDate = new Date(cursor.getLong(dtStartColumn));
      int colour = cursor.getInt(colourColumn);
      String hexColour = String.format("#%06X", (0xFFFFFF & colour));

      String location = cursor.getString(eventLocation);

      SimpleDateFormat sdf = new SimpleDateFormat("E dd MMM");
      String strStartDate = sdf.format(startDate);

      // new date - only comparing date, not time
      if (!strStartDate.equals(lastStartDate)) {
        // ignore first pass as spanStr will be empty
        if (spanStr.length() > 1) {
          span = Html.fromHtml(spanStr);
          mCollections.add(span);
        }
        spanStr = "<b><font color='#fffffc'>" + strStartDate + "</font>";
      }


      // show the title using the calendar event colour
      spanStr += "<br><b><small><font color='" + hexColour + "'>"  +  someSpaces+title+ "</font></small>";

      if (location.length() > 0) {
        spanStr += "<br><b><small><font color='" + hexColour + "'>" + someSpaces+"at "+location   + "</font></small>";
      }

      lastStartDate = strStartDate;

      if (description.length() > 1)      {
        spanStr +=  "<br><b><font color='#ffb49a'>" + description + ":</font>";
      }

    }  // end of moving through cursor

    // output the last date processed
    span = Html.fromHtml(spanStr);
    mCollections.add(span);

    numberOfItems = mCollections.size();
    }  // end of loopThroughRows

  }  // end of WidgetDisplay class



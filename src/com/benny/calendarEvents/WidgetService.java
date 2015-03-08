package com.benny.calendarEvents;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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


  String dateRangeString;
  String DatesPreferences = "DatesPreferences";
  String useDefault = "useDefaultDateRange";


  String someSpaces = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

  List<Spanned> mCollections = new ArrayList<Spanned>();
  List<Spanned> mRows = new ArrayList<Spanned>();

  // ----------------------------------------------------------------------- //
  //public static  long milliseconds = AlarmManager.INTERVAL_HOUR;
  // ----------------------------------------------------------------------- //


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

    //Log.d(tag,"getView called: " + (position+1));
    RemoteViews rvRow = new RemoteViews(mContext.getPackageName(),
        R.layout.row);
    rvRow.setTextViewText(R.id.row_text, span);

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

    rv.setOnClickFillInIntent(R.id.row_text, ei);
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
  public void onDataSetChanged() {
    SharedPreferences prefs;
    prefs = mContext.getSharedPreferences(DatesPreferences, Context.MODE_PRIVATE);
    Calendar calStart;
    Calendar calEnd;

    ContentResolver contentResolver = mContext.getContentResolver();
    Cursor cursor;
    String selection = "((" + CalendarContract.Events.DTSTART +
        " >= ?) AND (" + CalendarContract.Events.DTEND + " <= ?))";

    boolean isDefault =   prefs.getBoolean(useDefault, true);
    if (isDefault) {
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

    String stringDateStartInMillseconds = Long.toString(calStart.getTimeInMillis());
    String stringDateEndinMilliseconds = Long.toString(calEnd.getTimeInMillis());


    String[] selectionArgs = new String[] { stringDateStartInMillseconds, stringDateEndinMilliseconds };
    // get the calendar rows in start date sequence
    cursor = contentResolver.query(CalendarContract.Events.CONTENT_URI,
        null, selection, selectionArgs, "dtstart ASC");

    mCollections.clear();
    mRows.clear();
    Spanned span;
    String lastStartDate = "";
    String spanStr;
    String rowStr;

    Date dateStart = calStart.getTime();
    SimpleDateFormat dmyFormatter = new SimpleDateFormat("dd MMM yyyy");
    Date dateEnd = calEnd.getTime();
    String stringDateStart = dmyFormatter.format(dateStart);
    String stringDateEnd = dmyFormatter.format(dateEnd);
    spanStr = "<b><font color='#0fafac'>" + stringDateStart + " to xxxxxxxx" + stringDateEnd +"</font>";
    span = Html.fromHtml(spanStr);
    final RemoteViews rvWidget = new RemoteViews(mContext.getPackageName(), R.layout.widget);
    rvWidget.setTextViewText(R.id.date_range, span);
//    mCollections.add(span);

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

//      Log.i(tag,"-----------------------------------------------------------\n");
//      Log.i(tag,"title: "+title+"\n");
//      Log.i(tag,"description: "+description+"\n");
//      Log.i(tag,"startDate: "+startDate+"\n");
//      Log.i(tag,"colour: "+colour+"\n");
//      Log.i(tag,"colour: "+hexColour+"\n");

      // new date - only comparing date, not time
      if (!strStartDate.equals(lastStartDate)) {
        // ignore first pass as spanStr will be empty
        if (spanStr.length() > 1) {
          span = Html.fromHtml(spanStr);
          mCollections.add(span);
        }
        spanStr = "<b><font color='#fffffc'>" + strStartDate + "</font>";
        // always output each new date
        mRows.add(Html.fromHtml(spanStr));
      }


      // show the title using the calendar event colour
      spanStr += "<br><b><small><font color='" + hexColour + "'>" + someSpaces+title   + "</font></small>";
      rowStr = "<b><small><font color='" + hexColour + "'>" + someSpaces+title   + "</font></small>";
      mRows.add(Html.fromHtml(rowStr));

      if (location.length() > 0) {
        spanStr += "<br><b><small><font color='" + hexColour + "'>" + someSpaces+"at "+location   + "</font></small>";
        rowStr = "<b><small><font color='" + hexColour + "'>" + someSpaces+"at "+location   + "</font></small>";
        mRows.add(Html.fromHtml(rowStr));
      }

      lastStartDate = strStartDate;

      // put a coloured box in front of the title
      //"<br><big><font  color='" + hexColor + "'>&#9632; "   + "</font></big>" +

      if (description.length() > 1)      {
        spanStr +=  "<br><b><font color='#ffb49a'>" + description + ":</font>";
        rowStr =  "<b><font color='#ffb49a'>" + description + ":</font>";
        mRows.add(Html.fromHtml(rowStr));
      }

    }  // end of moving through cursor

    // output the last date processed
    span = Html.fromHtml(spanStr);
    mCollections.add(span);

//    mCollections = mRows;  // use separate rows for details
    numberOfItems = mCollections.size();

    Log.i(tag,"number of calendar events: "+numberOfItems+"\n");
  } // end of onDataSetChanged


}  // end of WidgetDisplay class






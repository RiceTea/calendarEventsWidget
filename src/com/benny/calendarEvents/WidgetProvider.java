package com.benny.calendarEvents;


// 10 July 2014
// Used code for this from GitHub:
// Android Pro book code - ProAndroid4_Ch26_TestListWidget
// started coding. had a few problems adding the calendar contract
//  using an asynchronous task. Then noticed the comment that it should
//  run successfully synchronously.

// The reason for writing this widget was to have a model which allowed
//  vertical scrolling in a widget.

// It uses RemoteViewsFactory to achieve this.
// The list is populated with Google calendar events (variable set to a fortnight).
// They are sorted by start date and grouped by date. The type of event determines
//  the title text colour.

// Noticed resizing the width kind of zooms in on it ... will sort that out another day.

// finished this evening and am sending it to GitHub
// Won't use the widget as have purchased DigiCal+ and they have excellent widgets

// 11 Jul 2014
// fixed the width not being handled correctly when resized

// 25 Feb 2015
// looking at this again as DigiCal+ does not have an option to ignore time. So it
// repeats the Day on an indented row2. Looks sloppy.
// As of today, my changes are working except for refreshing.  Will still use DigiCal+
// for updating and to view details. Will probably code to toast details when a row2
// is long pressed.
// Initially the app shows events from the current day for the next 15 weeks. Plan
// to create some preferences for start date and end date, etc

// 4 Mar 2015
// finished initial coding for preferences

// 15 Mar 2015
// added preference for displaying the list in calendar sequence

// TODO header not being updated when preference change or refresh
// TODO savedInstance

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class WidgetProvider extends AppWidgetProvider
{
  private static final String tag = "WidgetProvider";
  public static String SHOW_PREFERENCES = "SHOW_PREFERENCES";
  public static String REFRESH = "REFRESH";
  String version = " version (e1) ";               // ******************************************** //
  String widgetName = "CalendarEvents";
  String buttonCode="empty";
  Rect sourceBounds;
  int xOffset = 0, yOffset = 0;
  int toastBackgroundColour;

  String DatesPreferences = "DatesPreferences";
  String useDefault = "useDefaultDateRange";


  public static final String ACTION_LIST_CLICK =
      "com.benny.calendarEvents.listclick";

  public static final String EXTRA_LIST_ITEM_TEXT =
      "com.benny.calendarEvents.list_item_text";

  public static final String EXTRA_DATE_RANGE =
      "com.benny.calendarEvents.date_range";

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] allWidgetIds)
  {
//    Toast t = Toast.makeText(context,"onUpdate called",Toast.LENGTH_LONG); t.show();

    Log.d(tag, "onUpdate called");
    final int N = allWidgetIds.length;
    Log.d(tag, "Number of widgets: " + N);

    for (int i=0; i<N; i++) {
      int appWidgetId = allWidgetIds[i];
      updateAppWidget(context, appWidgetManager, appWidgetId);

    }

    // Build the intent to call the service
    Intent serviceIntent = new Intent(context.getApplicationContext(), WidgetService.class);
    serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);


    // tell the service which button was touched
    serviceIntent.putExtra(widgetName, buttonCode);
    // tell the service the bounds of the widget    - could be null
    serviceIntent.putExtra("bounds", sourceBounds);

    // Update the widgets via the service
    context.startService(serviceIntent);

    final RemoteViews rvWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
    Spanned span = setHeader(context, rvWidget);
    toastBackgroundColour = context.getResources().getColor(R.color.Crimson);
//    showToast(context, span, toastBackgroundColour, 3);

    super.onUpdate(context,appWidgetManager, allWidgetIds);
  }


  public void onDeleted(Context context, int[] allWidgetIds)
  {
    Log.d(tag, "onDelete called");
    super.onDeleted(context,allWidgetIds);
  }

  public void onEnabled(Context context)
  {
    Log.d(tag, "onEnabled called");
    super.onEnabled(context);
  }

  public void onDisabled(Context context)
  {
    Log.d(tag, "onDisabled called");
    super.onEnabled(context);
  }

  private void updateAppWidget(Context context,
                               AppWidgetManager appWidgetManager,
                               int appWidgetId)
  {
//    Toast t = Toast.makeText(context,"onUpdate called for widget",Toast.LENGTH_LONG); t.show();

    Log.d(tag, "updateAppWidget called for widget: " + appWidgetId);


    final RemoteViews rvWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
    setHeader(context, rvWidget);

    Intent preferencesIntent = new Intent(context, WidgetProvider.class);
    preferencesIntent.setAction(SHOW_PREFERENCES);
    PendingIntent pendingIntentPreferences = PendingIntent.getBroadcast(context,0, preferencesIntent, 0);
    rvWidget.setOnClickPendingIntent(R.id.preferences, pendingIntentPreferences);


    Intent refreshIntent = new Intent(context, WidgetProvider.class);
    refreshIntent.setAction(REFRESH);
    PendingIntent pendingIntentRefresh = PendingIntent.getBroadcast(context,0, refreshIntent, 0);
    rvWidget.setOnClickPendingIntent(R.id.refresh, pendingIntentRefresh);



    // Specify the service to provide data for the
    // collection widget. Note that we need to
    // embed the appWidgetId via the data otherwise
    // it will be ignored.
    final Intent intent = new Intent(context, WidgetService.class);

    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,  appWidgetId);

    rvWidget.setRemoteAdapter(R.id.listwidget_list_view_id, intent);

    //setup a list view call back.
    //we need a pending intent that is unique
    //for this widget id. Send a message to
    //ourselves which we will catch in OnReceive.
    Intent onListClickIntent = new Intent(context,WidgetProvider.class);

    //set an action so that this receiver can distinguish it
    //from other widget related actions
    onListClickIntent.setAction(WidgetProvider.ACTION_LIST_CLICK);

    //because this receiver serves all instances
    //of this app widget. We need to know which
    //specific instance this message is targeted for.
    onListClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

    //Make this intent unique as we are getting ready
    //to create a pending intent with it.
    //The toUri method loads the extras as
    //part of the uri string.
    //The data of this intent is not used at all except
    //to establish this intent as a unique pending intent.
    //See intent.filter Equals() method to see
    //how intents are compared to see if they are unique.
    onListClickIntent.setData(Uri.parse(onListClickIntent.toUri(Intent.URI_INTENT_SCHEME)));

    //we need to deliver this intent later when
    //the remote view is clicked as a broadcast intent
    //to this same receiver.
    final PendingIntent onListClickPendingIntent =
        PendingIntent.getBroadcast(context, 0, onListClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    //Set this pending intent as a template for
    //the list item view.
    //Each view in the list will then need to specify
    //a set of additional extras to be appended
    //to this template and then broadcast the
    //final template.
    //See how the remoteViewsFactory() sets up
    //each item in the list remoteView.
    //See also docs for RemoteViews.setFillIntent()
    rvWidget.setPendingIntentTemplate(R.id.listwidget_list_view_id, onListClickPendingIntent);

    //update the widget
    appWidgetManager.updateAppWidget(appWidgetId, rvWidget);
  }

  @Override
  public void onReceive(Context context, Intent intent) {

    super.onReceive(context, intent);

    String action = intent.getAction();
    // get the bounds of the widget
    sourceBounds = intent.getSourceBounds();

    if (action.equals(WidgetProvider.ACTION_LIST_CLICK)) {
      handleListAction(context,intent);
    }
    else if(action.equalsIgnoreCase(SHOW_PREFERENCES)) {
      handlePreferences(context, intent);
    }
    else if(action.equalsIgnoreCase(REFRESH)) {
      handleRefresh(context, intent);
    }

    else {
      final RemoteViews rvWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
      setHeader(context, rvWidget);
    }

  }  // end of onReceive

  public void handleListAction(Context context, Intent  intent)
  {
    String clickedItemText =
        intent.getStringExtra( WidgetProvider.EXTRA_LIST_ITEM_TEXT );
    if (clickedItemText == null)
      { clickedItemText = "Error"; }
    updateWidget(context);
  }


  private void handlePreferences(Context context, Intent  intent) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int allWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
    appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, R.id.listwidget_list_view_id);

    // show preference activity
    Intent prefsIntent = new Intent(context, DateRangePreferences.class);
    prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(prefsIntent);
    final RemoteViews rvWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
    setHeader(context, rvWidget);
  }


  private void handleRefresh(Context context, Intent  intent) {
    final RemoteViews rvWidget = new RemoteViews(context.getPackageName(), R.layout.widget);
    Spanned span = setHeader(context, rvWidget);
    toastBackgroundColour = context.getResources().getColor(R.color.PaleGoldenrod);
    showToast(context, span, toastBackgroundColour, 3);
  }


  private void updateWidget(Context context) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listwidget_list_view_id);
  }


  public Spanned setHeader (Context context, RemoteViews rvWidget) {
    rvWidget.setEmptyView(R.id.listwidget_list_view_id,  R.id.listwidget_empty_view_id);
    rvWidget.setTextViewText(R.id.version, version);
    Calendar calStart;
    Calendar calEnd;

    SharedPreferences prefs;
    prefs = context.getSharedPreferences(DatesPreferences, Context.MODE_PRIVATE);
    boolean isDefault =   prefs.getBoolean(useDefault, true);

    if (isDefault) {
      // set today for one year if using default preference
      calStart = Calendar.getInstance();
      calEnd = Calendar.getInstance();
      calEnd.add(Calendar.DATE, 52*7); // add a year
    }
    else {
      // otherwise, get date range from preferences
      int tempYear =  prefs.getInt("startingYear", 2015);
      int tempMonth =  prefs.getInt("startingMonth", 1);
      int tempDay =  prefs.getInt("startingDay", 1);
      calStart = Calendar.getInstance();
      calStart.set(tempYear,tempMonth,tempDay);

      tempYear =  prefs.getInt("endingYear", 2016);
      tempMonth =  prefs.getInt("endingMonth", 11);
      tempDay =  prefs.getInt("endingDay", 31);
      calEnd = Calendar.getInstance();
      calEnd.set(tempYear,tempMonth,tempDay);
    }

    Spanned span;
    String spanStr;
    SimpleDateFormat dmyFormatter = new SimpleDateFormat("dd MMM yyyy");

    Date dateStart = calStart.getTime();
    String stringDateStart = dmyFormatter.format(dateStart);
    Date dateEnd = calEnd.getTime();
    String stringDateEnd = dmyFormatter.format(dateEnd);

    spanStr = "<b><font color='#F08080'>" + stringDateStart + " to " + stringDateEnd +"</font>";
    span = Html.fromHtml(spanStr);

    rvWidget.setTextViewText(R.id.date_range, span);
    return span;
  }


  // duration is parsed in seconds
  protected void showToast(Context context,Spanned toastMessage, int colour, int duration) {
    Context mContext = context;
    xOffset = 50; yOffset = 450;

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
    outerRect.setColor(Color.parseColor("#cfcfc2"));
    Toast toast = new CustomToast(mContext);

    toast.setDuration(duration); // show for nnn seconds
    toast.setGravity(Gravity.TOP | Gravity.LEFT, xOffset, yOffset);
    toast.setView(layout);
    toast.show();
  }

} //eof-class

//    background.setColor(mContext.getResources().getColor(R.color.buleish));

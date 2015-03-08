CalendarEventsWidget
====================

A widget that displays a vertically scrollable list from a Google calendar

Used code for this from GitHub:
Android Pro book code - ProAndroid4_Ch26_TestListWidget

The reason for writing this widget was to have a model which allowed
vertical scrolling in a widget.

It uses RemoteViewsFactory to achieve this.
The list is populated with Google calendar events using a preferences date range.
They are sorted by start date and grouped by date. The type of event determines
the title text colour.


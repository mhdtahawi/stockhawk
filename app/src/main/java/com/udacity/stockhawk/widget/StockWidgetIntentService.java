package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.ui.StockDetails;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static com.udacity.stockhawk.R.id.symbol;

/**
 * Created by monac on 5/14/2017.
 */

public class StockWidgetIntentService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetViewFactory(getApplicationContext());
    }

    private class StockWidgetViewFactory implements RemoteViewsFactory {
        private final Context mApplicationContext;
        private List<ContentValues> mCvList = new ArrayList<>();
        private final DecimalFormat dollarFormat;
        private final DecimalFormat dollarFormatWithPlus;
        private final DecimalFormat percentageFormat;


        public StockWidgetViewFactory(Context applicationContext) {
            mApplicationContext = applicationContext;
            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            percentageFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");

        }


        public void getData() {

            mCvList.clear();

            ContentResolver contentResolver = mApplicationContext.getContentResolver();


            Cursor cursor = contentResolver.query(Contract.Quote.URI, null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {

                    String symbol = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                    Float price = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                    Float absChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                    Float percentChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));

                    ContentValues cv = new ContentValues();

                    cv.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    cv.put(Contract.Quote.COLUMN_PRICE, price);
                    cv.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, absChange);
                    cv.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);

                    mCvList.add(cv);
                }
            }


            cursor.close();
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {

            /*
            http://stackoverflow.com/a/20645908/1182207
            final long token = Binder.clearCallingIdentity();
            try {
                getData();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            */
            Thread thread = new Thread() {
                public void run() {
                    getData();
                }
            };
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
            }

        }

        @Override
        public void onDestroy() {

        }


        @Override
        public int getCount() {
            return mCvList.size();
        }

        @Override
        public RemoteViews getViewAt(int i) {
            ContentValues cv = mCvList.get(i);
            RemoteViews views = new RemoteViews(mApplicationContext.getPackageName(), R.layout.list_item_quote);

            views.setTextViewText(R.id.symbol, cv.getAsString(Contract.Quote.COLUMN_SYMBOL));
            views.setTextViewText(R.id.price, dollarFormat.format(cv.getAsFloat(Contract.Quote.COLUMN_PRICE)));

            float absChange = cv.getAsFloat(Contract.Quote.COLUMN_ABSOLUTE_CHANGE);
            float perChange = cv.getAsFloat(Contract.Quote.COLUMN_PERCENTAGE_CHANGE);

            if (absChange > 0) {
                views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);

            }
            if (PrefUtils.getDisplayMode(mApplicationContext)
                    .equals(mApplicationContext.getString(R.string.pref_display_mode_absolute_key))) {
                views.setTextViewText(R.id.change, dollarFormatWithPlus.format(perChange / 100));
            } else {
                views.setTextViewText(R.id.change, percentageFormat.format(perChange / 100));
            }


            Intent fillInIntent = new Intent(mApplicationContext, StockDetails.class);
            fillInIntent.putExtra("SYMBOL", cv.getAsString(Contract.Quote.COLUMN_SYMBOL));
            views.setOnClickFillInIntent(R.id.item_quote_root, fillInIntent);


            //Intent lunchDetailsActivityIntent = new Intent(mApplicationContext, StockDetails.class);
            //lunchDetailsActivityIntent.putExtra("SYMBOL", symbol);

            //PendingIntent pendingIntent = PendingIntent.getActivity(mApplicationContext, 0, lunchDetailsActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            //views.setOnClickPendingIntent(R.id.item_quote_root, pendingIntent);


            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }


}

package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;

import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;


import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import timber.log.Timber;

public class StockDetails extends AppCompatActivity {


    //@BindView(R.id.symbol)
    TextView symboltv;

    //@BindView(R.id.price)
    TextView pricetv;

    //@BindView(R.id.change)
    TextView changetv;

    LineChart chart;

    String change;
    String percentage;
    String history;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_details);

        Intent intent = getIntent();



        symboltv = (TextView) findViewById(R.id.symbol);
        pricetv = (TextView) findViewById(R.id.price);
        changetv = (TextView) findViewById(R.id.change);
        chart = (LineChart) findViewById(R.id.chart);

        if (intent != null && intent.hasExtra("SYMBOL")) {

            String symbol = intent.getStringExtra("SYMBOL");


            populateDetails(symbol);

            // chart





            List<Entry> entries = new ArrayList<>();

            CSVReader reader = new CSVReader(new StringReader(history));
            String [] nextLine;
            final List<Long> xAxisValues = new ArrayList<>();
            int xAxisPosition = 0;
            try {
                while ((nextLine = reader.readNext()) != null) {
                    xAxisValues.add(Long.valueOf(nextLine[0]));
                    Entry entry = new Entry(
                            xAxisPosition,
                            Float.valueOf(nextLine[1])
                    );
                    entries.add(entry);
                    xAxisPosition++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            LineData lineData = new LineData(new LineDataSet(entries, symbol));
            chart.setData(lineData);
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    Date date = new Date(xAxisValues.get(xAxisValues.size()-(int)value-1));
                    return new SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH).format(date);
                }
            });


//
//            Timber.d("Creating data");
//            LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset
//
//            dataSet.setCircleRadius(5);
//            dataSet.setLineWidth(10);
//            dataSet.setColor(Color.argb(255, 255, 0, 0));
//            dataSet.setValueTextColor(Color.argb(255, 0, 255, 0)); // styling, ...
//            LineData lineData = new LineData(dataSet);
//            Timber.d("Setting up chart");
//            chart.setData(lineData);
//            chart.getAxisLeft().setSpaceBottom(10);
//            chart.getAxisRight().setSpaceTop(10);
//            chart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
//
//                private SimpleDateFormat mFormat = new SimpleDateFormat("YYYY");
//
//                @Override
//                public String getFormattedValue(float value, AxisBase axis) {
//
//                    //long millis = TimeUnit.HOURS.toMillis((long) value);
//                    long millis = (long) (value );
//                    return mFormat.format(new Date(millis));
//                }});
//
//
//            chart.setMaxVisibleValueCount(100);
//
//
//            //chart.invalidate(); // refresh

            Timber.d("I am here now");

        }




    }

    private void populateDetails(String symbol) {
        Cursor cursor = getContentResolver().query(Contract.Quote.makeUriForStock(symbol), null, null, null, null);
        cursor.moveToNext();

        float price = cursor.getFloat(Contract.Quote.POSITION_PRICE);
        float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
        history = cursor.getString(Contract.Quote.POSITION_HISTORY);


        DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        DecimalFormat percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
        symboltv.setText(symbol);

        pricetv.setText(dollarFormat.format(price));


        if (rawAbsoluteChange > 0) {
            changetv.setBackgroundResource(R.drawable.percent_change_pill_green);
        } else {
            changetv.setBackgroundResource(R.drawable.percent_change_pill_red);
        }

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        setChangeFormat(change, percentage);
    }

    private void setChangeFormat(String change, String percentage) {
        if (PrefUtils.getDisplayMode(this)
                .equals(this.getString(R.string.pref_display_mode_absolute_key))) {
            changetv.setText(change);
        } else {
            changetv.setText(percentage);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            populateDetails(symboltv.getText().toString()); //
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }
}

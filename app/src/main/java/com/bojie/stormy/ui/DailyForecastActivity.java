package com.bojie.stormy.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.bojie.stormy.R;

public class DailyForecastActivity extends ListActivity {

    private String[] daysOfTheWeek =
            { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

    private ArrayAdapter<String> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        mAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_list_item_1,
                daysOfTheWeek);

        setListAdapter(mAdapter);

    }



}

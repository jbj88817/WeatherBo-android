package com.bojie.stormy.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.bojie.stormy.R;
import com.bojie.stormy.adapter.DayAdapter;
import com.bojie.stormy.weather.Day;

public class DailyForecastActivity extends ListActivity {

    private String[] daysOfTheWeek =
            {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    private ArrayAdapter<String> mAdapter;
    private Day[] mDays;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);

        DayAdapter adapter = new DayAdapter(this, mDays);
    }

}

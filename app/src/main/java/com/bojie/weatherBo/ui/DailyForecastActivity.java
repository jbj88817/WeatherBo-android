package com.bojie.weatherbo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bojie.weatherbo.R;
import com.bojie.weatherbo.adapter.DayAdapter;
import com.bojie.weatherbo.util.UnitConvert;
import com.bojie.weatherbo.weather.Day;

import java.text.DecimalFormat;
import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DailyForecastActivity extends ActionBarActivity {

    private Day[] mDays;

    @InjectView(android.R.id.list)
    ListView mListView;
    @InjectView(android.R.id.empty)
    TextView mEmptyTextView;
    @InjectView(R.id.tv_LocationLabel)
    TextView mLocationLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_forecast);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        Parcelable[] parcelables = intent.getParcelableArrayExtra(MainActivity.DAILY_FORECAST);
        mDays = Arrays.copyOf(parcelables, parcelables.length, Day[].class);

        DayAdapter adapter = new DayAdapter(this, mDays);
        mListView.setAdapter(adapter);
        mListView.setEmptyView(mEmptyTextView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String dayOfTheWeek = mDays[position].getDayOfTheWeek();
                String conditions = mDays[position].getSummary();

                double TempInF = mDays[position].getTemperatureMax();
                double TempInC = UnitConvert.fahrenheitToCelsius(TempInF);
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                String highTemp = df.format(TempInF) + "";
                if (MainActivity.mButtonUnitConvert.getText() == "F") {
                    highTemp = df.format(TempInF) + "";
                } else if(MainActivity.mButtonUnitConvert.getText() == "C"){
                    highTemp = df.format(TempInC) + "";
                }
                String message = String.format("On %s the high will be %s and it will be %s",
                        dayOfTheWeek,
                        highTemp,
                        conditions);

                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

            }
        });
        String cityName = intent.getStringExtra(MainActivity.CITY_NAME);
        mLocationLabel.setText(cityName);

    }
}

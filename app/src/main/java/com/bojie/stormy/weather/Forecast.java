package com.bojie.stormy.weather;

/**
 * Created by bojiejiang on 2/28/15.
 */
public class Forecast {
    private Current mCurrent;
    private Hour[] mHourlyForecast;
    private Day[] mDaylyForecast;

    public Current getCurrent() {
        return mCurrent;
    }

    public void setCurrent(Current current) {
        mCurrent = current;
    }

    public Hour[] getHourlyForecast() {
        return mHourlyForecast;
    }

    public void setHourlyForecast(Hour[] hourlyForecast) {
        mHourlyForecast = hourlyForecast;
    }

    public Day[] getDaylyForecast() {
        return mDaylyForecast;
    }

    public void setDaylyForecast(Day[] daylyForecast) {
        mDaylyForecast = daylyForecast;
    }
}

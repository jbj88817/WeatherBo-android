package com.bojie.stormy.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bojie.stormy.R;
import com.bojie.stormy.ui.MainActivity;
import com.bojie.stormy.util.UnitConvert;
import com.bojie.stormy.weather.Hour;

/**
 * Created by bojiejiang on 3/5/15.
 */
public class HourAdapter extends RecyclerView.Adapter<HourAdapter.HourViewHolder> {

    private Hour[] mHours;
    private Context mContext;

    public HourAdapter(Context contexts, Hour[] hours) {
        mHours = hours;
        mContext = contexts;
    }


    @Override
    public HourViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.hourly_list_item, parent, false);
        HourViewHolder viewHolder = new HourViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(HourViewHolder holder, int position) {
        holder.bindHour(mHours[position]);
    }

    @Override
    public int getItemCount() {
        return mHours.length;
    }

    public class HourViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public TextView mTimeLabel;
        public TextView mSummaryLabel;
        public TextView mTemperatureLabel;
        public ImageView mIconImageView;


        public HourViewHolder(View itemView) {
            super(itemView);
            mTimeLabel = (TextView) itemView.findViewById(R.id.tv_hourlyTimeLabel);
            mSummaryLabel = (TextView) itemView.findViewById(R.id.tv_hourlySummaryLabel);
            mTemperatureLabel = (TextView) itemView.findViewById(R.id.tv_hourlyTemperatureLabel);
            mIconImageView = (ImageView) itemView.findViewById(R.id.hourlyIconImageView);

            itemView.setOnClickListener(this);
        }

        public void bindHour(Hour hour) {
            mTimeLabel.setText(hour.getHour());
            mSummaryLabel.setText(hour.getSummary());
            // Convert unit
            double TempInF = hour.getTemperature();
            double TempInC = UnitConvert.fahrenheitToCelsius(TempInF);
            mTemperatureLabel.setText(Math.round(TempInF) + "");
            if (MainActivity.mButtonUnitConvert.getText() == "F") {
                mTemperatureLabel.setText(Math.round(TempInF) + "");
            } else if(MainActivity.mButtonUnitConvert.getText() == "C"){
                mTemperatureLabel.setText(Math.round(TempInC) + "");
            }
            mIconImageView.setImageResource(hour.getIconId());
        }

        @Override
        public void onClick(View v) {
            String time = mTimeLabel.getText().toString();
            String temperature = mTemperatureLabel.getText().toString();
            String summary = mSummaryLabel.getText().toString();
            String message = String.format("At %s it will be %s and %s",
                    time,
                    temperature,
                    summary);

            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        }
    }
}

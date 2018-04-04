package com.example.bartek.followyou.DetailActivities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

/**
 * Created by bartek on 31.03.2018.
 */

public class DetailsInfoActivity extends Fragment {
    public final static String TAG = "DetailsInfoActivity";
    public static final double radius_of_earth = 6378.1;
    private TextView textViewStartTime, textViewEndTime, textViewTime, textViewMaxSpeed, textViewAvgSpeed, textViewDistance;
    private LineChart lineChart;
    private int wayID;
    private AppDatabase database;
    private LocDao locDao;
    private List<Loc> locList;
    ArrayList<Entry> entries = new ArrayList<>();
    double lat1, lon1, lat2, lon2, distance, spped, avgSpeed, maxSpeed;
    long diffTime;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wayID = getArguments().getInt("wayID", 1);
    }

    public static DetailsInfoActivity newInstance(int wayID) {
        DetailsInfoActivity fragmetInfo = new DetailsInfoActivity();
        Bundle args = new Bundle();
        args.putInt("wayID", wayID);
        fragmetInfo.setArguments(args);
        return fragmetInfo;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_details_info, container, false);
        progressDialog = new ProgressDialog(getContext());

        textViewStartTime = (TextView) view.findViewById(R.id.textViewInfoStartTime);
        textViewEndTime = (TextView) view.findViewById(R.id.textViewInfoEndTime);
        textViewTime = (TextView) view.findViewById(R.id.textViewInfoTime);
        textViewMaxSpeed = (TextView) view.findViewById(R.id.textViewInfoMaxSpeed);
        textViewAvgSpeed = (TextView) view.findViewById(R.id.textViewInfoAvgSpeed);
        textViewDistance = (TextView) view.findViewById(R.id.textViewInfoDistance);
        lineChart = (LineChart) view.findViewById(R.id.barChart);

        database = Room.databaseBuilder(getContext(), AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        locDao = database.locDao();
        progressDialog = new ProgressDialog(getContext());

        new AsyncTask<Void, Void, List<Loc>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Please wait.");
                progressDialog.show();
            }

            @Override
            protected List<Loc> doInBackground(Void... voids) {
                return locDao.getLocsForWayID(wayID);
            }

            @Override
            protected void onPostExecute(List<Loc> locs) {
                locList = locs;
                setGui();
            }
        }.execute();
        return view;
    }

    private void setGui() {
        int locListSize = locList.size();
        if (locListSize < 10) {
            progressDialog.dismiss();
            return;
        }
        long startTime = locList.get(0).getTime();
        long endTime = locList.get(locListSize - 1).getTime();
        diffTime = endTime - startTime;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        SimpleDateFormat format = new SimpleDateFormat("hh:mm, EEEE, d MMMM, yyyy");
        textViewStartTime.setText(format.format(calendar.getTime()));
        calendar.setTimeInMillis(endTime);
        textViewEndTime.setText(format.format(calendar.getTime()));

        int seconds = (int) ((diffTime) / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        hours %= 60;
        textViewTime.setText(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));

        for (int i = 0; i < locListSize - 1; i++) {
            lat1 = locList.get(i).getLatitude();
            lon1 = locList.get(i).getLongitude();
            lat2 = locList.get(i + 1).getLatitude();
            lon2 = locList.get(i + 1).getLongitude();
            distance += haversine(lat1, lon1, lat2, lon2);
        }
        textViewDistance.setText(String.format("%02f", distance) + " km");

        for (int i = 0; i < locListSize; i++) {
            spped = locList.get(i).getSpeed();
            spped *= 3.6;
            avgSpeed += spped;
            if (i == 0) {
                entries.add(new BarEntry(0, (float) spped));
                diffTime = 0;
            } else {
                diffTime += locList.get(i).getTime() - locList.get(i - 1).getTime();
                Log.d(TAG, "setGui: " + diffTime);
                entries.add(new BarEntry(diffTime / 1000, (float) spped));
            }
            //entries.add(new BarEntry(i, (float) spped));
            if (spped > maxSpeed) maxSpeed = spped;
        }
        avgSpeed /= locListSize;
        textViewMaxSpeed.setText(String.format("%02f", maxSpeed) + " km/h");
        textViewAvgSpeed.setText(String.format("%02f", avgSpeed) + " km/h");

        LineDataSet dataset = new LineDataSet(entries, "km/h");
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataset.setCubicIntensity(0.1f);
        dataset.setDrawFilled(true);
        dataset.setDrawCircles(false);
        dataset.setLineWidth(4.8f);
        dataset.setColor(Color.RED);
        dataset.setFillColor(Color.RED);
        dataset.setFillAlpha(100);
        dataset.setDrawHorizontalHighlightIndicator(true);
        dataset.setDrawValues(false);
        lineChart.getAxisLeft().setDrawLabels(true);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getXAxis().setDrawLabels(true);
        lineChart.getLegend().setEnabled(true);
        LineData data = new LineData(dataset);
        lineChart.setData(data);
        lineChart.setScaleYEnabled(false);
        lineChart.notifyDataSetChanged();
        progressDialog.dismiss();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return radius_of_earth * c;
    }

}


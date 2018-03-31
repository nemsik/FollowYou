package com.example.bartek.followyou.DetailActivities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bartek.followyou.R;
import com.github.mikephil.charting.charts.LineChart;

/**
 * Created by bartek on 31.03.2018.
 */

public class DetailsInfoActivity extends Fragment {
    public final static String TAG = "DetailsInfoActivity";
    private int userID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userID = getArguments().getInt("wayID", 1);
    }

    public static DetailsInfoActivity newInstance(int userID){
        DetailsInfoActivity fragmetInfo = new DetailsInfoActivity();
        Bundle args = new Bundle();
        args.putInt("wayID", userID);
        fragmetInfo.setArguments(args);
        return fragmetInfo;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_details_info, container, false);
        return view;
    }
}

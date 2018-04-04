package com.example.bartek.followyou.History;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.example.bartek.followyou.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

/**
 * Created by bartek on 04.04.2018.
 */

public class HistoryAdapter extends ArrayAdapter<Way> {
    private static final String TAG = "HistoryAdapter";
    private Context context;
    private TextView textViewStartTime, textViewDiffTime, textViewDistance;
    private ImageView imageView;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private List<Long> startTime;
    private List<Long> diffTime;
    private List<Double> distance;
    private long start, diff;

    public HistoryAdapter(Context context, int resource, final List<Way> ways, final List<Long> startTime, final List<Long> diffTime, final List<Double> distance) {
        super(context, resource, ways);
        this.context = context;
        this.startTime = startTime;
        this.diffTime = diffTime;
        this.distance = distance;
        database = Room.databaseBuilder(context, AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        wayDao = database.wayDao();
        locDao = database.locDao();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;


        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(context);
            v = vi.inflate(R.layout.history_adapter, null);
        }
        textViewStartTime = (TextView) v.findViewById(R.id.historyTextViewStartTime);
        textViewDiffTime = (TextView) v.findViewById(R.id.historyTextViewDiffTime);
        textViewDistance = (TextView)v.findViewById(R.id.historyTextViewDistance);
        imageView = (ImageView) v.findViewById(R.id.historyImageView);
        imageView.setImageResource(R.mipmap.ic_launcher);

        final Way way = getItem(position);

        if (way != null){
            start = startTime.get(position);
            Calendar calendar = Calendar.getInstance();
            if(start !=0) {
                calendar.setTimeInMillis(start);
                SimpleDateFormat format = new SimpleDateFormat("hh:mm, d MMMM, yyyy");
                textViewStartTime.setText(format.format(calendar.getTime()));

                int seconds = (int) ((diffTime.get(position)) / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds %= 60;
                minutes %= 60;
                hours %= 60;
                textViewDiffTime.setText(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
                calendar.setTimeInMillis(diffTime.get(position));
                textViewDistance.setText(String.format("%02f", distance.get(position)) + " km");
            } else {
                textViewStartTime.setText("???");
                textViewDiffTime.setText("???");
                textViewDistance.setText("???");
            }
            Log.d(TAG, "getView: distance" + distance.get(position));
        }

        return v;
    }
}

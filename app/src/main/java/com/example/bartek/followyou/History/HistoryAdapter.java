package com.example.bartek.followyou.History;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.example.bartek.followyou.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

/**
 * Created by bartek on 04.04.2018.
 */

public class HistoryAdapter extends ArrayAdapter<Way> {
    private static final String TAG = "HistoryAdapter";
    private Context context;
    private TextView textViewStartTime;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;

    public HistoryAdapter(Context context, int resource, final List<Way> ways) {
        super(context, resource, ways);
        this.context = context;
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

        final Way way = getItem(position);

        Log.d(TAG, "getView: ");




        /*new AsyncTask<Void, Void, List<Loc>>(){
            @Override
            protected List<Loc> doInBackground(Void... voids) {
                Log.e(TAG, "doInBackground: ");
                return locDao.getLocsForWayID(way.getId());
            }

            @Override
            protected void onPostExecute(List<Loc> locs) {
                super.onPostExecute(locs);
                if (way != null){
                    try{
                        textViewStartTime.setText(locs.get(0).getTime() + "");
                    }catch (Exception e){
                        textViewStartTime.setText("null");
                    }
                }

            }
        }.execute();*/
        //notifyDataSetChanged();

        return v;
    }
}

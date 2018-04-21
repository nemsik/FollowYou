package com.example.bartek.followyou.History;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.example.bartek.followyou.DetailActivities.DetailsActivity;
import com.example.bartek.followyou.MainActivity;
import com.example.bartek.followyou.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.example.bartek.followyou.DetailActivities.DetailsInfoActivity.radius_of_earth;
import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

public class HistoryActivity extends AppCompatActivity {
    private ListView listView;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private HistoryAdapter historyAdapter;
    private Context context;
    private List<Way> wayList;
    private List<Loc> locList;
    private ArrayList<Long> wayStartTime = new ArrayList<>();
    private ArrayList<Long> wayDiffTime = new ArrayList<>();
    private ArrayList<Double> wayDistance = new ArrayList<>();
    private double distance;
    private Intent mapsIntent;
    private ProgressDialog progressDialog;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    finish();
                    startActivity(mapsIntent);
                    return true;
                case R.id.navigation_history:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        context = getApplicationContext();
        database = Room.databaseBuilder(context, AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        wayDao = database.wayDao();
        locDao = database.locDao();

        mapsIntent = new Intent(this, MainActivity.class);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation_history);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_history);

        listView = (ListView) findViewById(R.id.listView);

        progressDialog = new ProgressDialog(this);

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Please wait.");
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                wayStartTime.clear();
                wayDiffTime.clear();
                wayList = wayDao.getAll();
                Collections.reverse(wayList);
                int wayId;
                for(int i=0; i<wayList.size(); i++){
                    try {
                        distance = 0;
                        wayId = wayList.get(i).getId();
                        locList = locDao.getLocsForWayID(wayId);
                        wayStartTime.add(locList.get(0).getTime());
                        wayDiffTime.add(locList.get(locList.size()-1).getTime() - locList.get(0).getTime());
                        for (int j=1; j<locList.size(); j++){
                            distance += haversine(locList.get(j).getLatitude(), locList.get(j).getLongitude(), locList.get(j-1).getLatitude(), locList.get(j-1).getLongitude());
                        }
                        wayDistance.add(distance);
                    }catch (Exception e){
                        wayStartTime.add(0l);
                        wayDiffTime.add(0l);
                        wayDistance.add(0d);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                setList(wayList, wayStartTime, wayDiffTime, wayDistance);
                progressDialog.dismiss();
            }
        }.execute();
    }

    private void setList(final List<Way> ways, final List<Long> wayStartTime, final List<Long> wayDiffTime, final List<Double> wayDistance){
        historyAdapter = new HistoryAdapter(context, R.layout.history_adapter, ways, wayStartTime, wayDiffTime, wayDistance);
        listView.setAdapter(historyAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent detailsActivity = new Intent(context, DetailsActivity.class);
                detailsActivity.putExtra(MainActivity.DetailsIntentTag, ways.get(i).getId());
                startActivity(detailsActivity);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int pos, long l) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                builder.setTitle("Delete entry")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                                new AsyncTask<Void, Void, Void>(){
                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        progressDialog.show();
                                    }

                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        wayDao.deleteById(ways.get(pos).getId());
                                        ways.remove(pos);
                                        wayStartTime.remove(pos);
                                        wayDiffTime.remove(pos);
                                        wayDistance.remove(pos);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        historyAdapter.notifyDataSetChanged();
                                        progressDialog.dismiss();
                                    }
                                }.execute();

                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        startActivity(mapsIntent);
    }
}

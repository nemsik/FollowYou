package com.example.bartek.followyou;

import android.Manifest;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    public static String TAG = "MainActivity";
    public static String NAME_DATABASE = "FollowYou";

    private GoogleMap mMap;
    private boolean permissionGranted, runnerisStarted = false;
    private Button bStartStop, bHistory;
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewAvgSpeed;
    private Context context;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private List<Loc> locList;
    private List<Way> wayList;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bHistory = (Button) findViewById(R.id.buttonHistory);
        context = this;

        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, NAME_DATABASE)
                        .fallbackToDestructiveMigration().build();

        wayDao = database.wayDao();
        locDao = database.locDao();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);

        bStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>(){
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Way way = new Way();
                        wayDao.insert(way);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        startService(new Intent(context, LocationService.class));
                    }
                }.execute();
            }
        });

        bHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TestDataBase();
            }
        });

    }

    private boolean checkPermissions() {
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            if (!mMap.isMyLocationEnabled() && permissionGranted) mMap.setMyLocationEnabled(true);
        }
        return permissionGranted;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                checkPermissions();
            }
        });

    }

    private void TestDataBase() {

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.e(TAG, "onPreExecute: ");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                wayList = wayDao.getAll();
                int size = wayList.size();
                locList = locDao.findRepositoriesForUser(size);
                //for(int i=0 ;i<locList.size(); i++){
                    //Log.d(TAG, locList.get(i).getLocation().toString());
                //}
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.e(TAG, "locList size: "+locList.size());
            }
        }.execute();


        //Test test = new Test();
        //test.execute();

    }

    private class Test extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                wayList.clear();
            }catch (Exception e){e.getMessage();}
        }

        @Override
        protected Void doInBackground(Void... voids) {
            wayList.clear();
            wayList = wayDao.getAll();

            for (int i = 0; i < wayList.size(); i++) {
                id = wayList.get(i).getId();
                Log.e("Way id: ", id + "");

                try {
                    locList = locDao.findRepositoriesForUser(id);
                }catch (Exception e){
                    Log.e(TAG, e.toString());
                }

                for (int j = 0; j < locList.size(); j++) {
                    Log.d("loc id: ", locList.get(j).getId() + "");
                    try {
                        Log.d("loc location: ", locList.get(j).getLocation().toString());
                    }catch (Exception e){
                        Log.d(TAG, "doInBackground: " + e.getMessage());
                    }
                }
                locList.clear();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: ");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


}

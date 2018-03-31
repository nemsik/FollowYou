package com.example.bartek.followyou;

import android.Manifest;
import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.example.bartek.followyou.DetailActivities.DetailsActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;


public class MainActivity extends AppCompatActivity implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    public static String TAG = "MainActivity";
    public static String NAME_DATABASE = "FollowYou";
    public static final String Filter = "GpsIntentFilter";
    public static final String DetailsIntentTag = "WayId";
    public static final String SharedTag = "SharedPreferencesRunner";
    public static final String SharedRunnerIsStarted = "followIsStarted";

    private GoogleMap mMap;
    private boolean permissionGranted, runnerisStarted = false;
    private Button bStartStop, bHistory;
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewAvgSpeed;
    private Context context;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private Way way;
    private Loc loc;
    private LocationManager locationManager;
    private Intent locationService, historyIntent, detailsIntent;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter = new IntentFilter(Filter);
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bStartStop.setOnClickListener(new bStartStopClick());
        bHistory = (Button) findViewById(R.id.buttonHistory);


        checkPermissions();
        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        wayDao = database.wayDao();
        locDao = database.locDao();

        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        initializeIntents();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: ");
            }
        };
    }

    private boolean checkPermissions() {
        permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            permissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
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
        if (checkPermissions() == true) mMap.setMyLocationEnabled(true);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
            }
        });
    }

    private void initializeIntents(){
        locationService = new Intent(this, LocationService.class);
        //historyIntent = new Intent(this, HistoryActivity.class);
        detailsIntent = new Intent(this, DetailsActivity.class);
    }

    private class bStartStopClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!runnerisStarted) {
                if(!checkisGPSenabled()) buildAlertMessageNoGps();
                else startFollow();
            } else stopFollow();
        }
    }

    private void startFollow(){
        if (!checkPermissions()) return;
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected void onPreExecute() {
                runnerisStarted = true;
                bStartStop.setText("Stop");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                way = new Way();
                wayDao.insert(way);
                startService(locationService);
                registerReceiver(broadcastReceiver, intentFilter);
                return null;
            }
        }.execute();
    }

    private void stopFollow(){
        bStartStop.setText("Start");
        runnerisStarted = false;
        mMap.clear();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Log.i(TAG, "can't unregister receiver");
        }
        try {
            stopService(locationService);
        } catch (Exception e) {
            Log.i(TAG, "can't stop gpsService");
        }
        try {
            //handler.removeCallbacks(runnable);
        } catch (Exception e) {
            Log.i(TAG, "can't remove callbacks");
        }
        new AsyncTask<Void, Void, Way>(){
            @Override
            protected Way doInBackground(Void... voids) {
                way = wayDao.getLastWay();
                return way;
            }

            @Override
            protected void onPostExecute(Way way) {
                detailsIntent.putExtra(DetailsIntentTag, way.getId());
                startActivity(detailsIntent);
            }
        }.execute();
    }

    private void continueFollow(){
        bStartStop.setText("Stop");
        startService(locationService);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void saveState() {
        editor.putBoolean(SharedRunnerIsStarted, runnerisStarted);
        editor.commit();
    }

    private void loadState() {
        runnerisStarted = sharedPreferences.getBoolean(SharedRunnerIsStarted, false);
        Log.d(TAG, "loadState: " + runnerisStarted);
        if (runnerisStarted) continueFollow();
    }

    private boolean checkisGPSenabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true;
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
    }

}

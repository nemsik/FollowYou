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
import android.os.Handler;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;
import java.util.List;

import static com.example.bartek.followyou.DetailActivities.DetailsInfoActivity.radius_of_earth;


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
    private LatLng latLng;
    private PolylineOptions rectOptions;
    private boolean permissionGranted, runnerisStarted = false;
    private Button bStartStop, bHistory;
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewAvgSpeed;
    private Context context;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private Way way;
    private int wayID;
    private Loc loc;
    private LocationManager locationManager;
    private Intent locationService, historyIntent, detailsIntent;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter = new IntentFilter(Filter);
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Handler handler = new Handler();
    private long startTime, difftime;
    private double spped, avgspeed, distance, lat1, lat2, lon1, lon2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bStartStop.setOnClickListener(new bStartStopClick());
        bHistory = (Button) findViewById(R.id.buttonHistory);
        textViewSpeed = (TextView) findViewById(R.id.textViewSpeed);
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        textViewAvgSpeed = (TextView) findViewById(R.id.textViewAvgSpeed);

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
        rectOptions = new PolylineOptions();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: ");
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        spped = locDao.getLastLoc().getSpeed();
                        spped *= 3.6;
                        lat2 = locDao.getLastLoc().getLatitude();
                        lon2 = locDao.getLastLoc().getLongitude();
                        if (lat1 != 0) {
                            distance += haversine(lat1, lon1, lat2, lon2);
                        }
                        lat1 = lat2;
                        lon1 = lon2;
                        difftime = locDao.getLastLoc().getTime() - locDao.getFirstLocById(wayID).getTime();
                        avgspeed = ((distance * 1000) / (difftime / 1000)) * 3.6;
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        textViewSpeed.setText(String.format("%.2f", spped) + " km/h");
                        textViewDistance.setText(String.format("%.2f", distance) + " km");
                        textViewAvgSpeed.setText(String.format("%.2f", avgspeed) + " km/h");
                        drawRoute(lat2, lon2);
                    }
                }.execute();
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

    private void initializeIntents() {
        locationService = new Intent(this, LocationService.class);
        //historyIntent = new Intent(this, HistoryActivity.class);
        detailsIntent = new Intent(this, DetailsActivity.class);
    }

    private class bStartStopClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!runnerisStarted) {
                if (!checkisGPSenabled()) buildAlertMessageNoGps();
                else startFollow();
            } else stopFollow();
        }
    }

    private void startFollow() {
        if (!checkPermissions()) return;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                runnerisStarted = true;
                bStartStop.setText("Stop");
                rectOptions = new PolylineOptions();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                way = new Way();
                wayDao.insert(way);
                way = wayDao.getLastWay();
                wayID = way.getId();
                startService(locationService);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                registerReceiver(broadcastReceiver, intentFilter);
                startTime = Calendar.getInstance().getTimeInMillis();
                handler.postDelayed(runnable, 1000);
            }
        }.execute();
    }

    private void stopFollow() {
        difftime = 0;
        distance = 0;
        lat1 = 0;
        lon1 = 0;
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
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
            Log.i(TAG, "can't remove callbacks");
        }
        new AsyncTask<Void, Void, Way>() {
            @Override
            protected Way doInBackground(Void... voids) {
                way = wayDao.getLastWay();
                wayID = way.getId();
                return way;
            }

            @Override
            protected void onPostExecute(Way way) {

                detailsIntent.putExtra(DetailsIntentTag, wayID);
                startActivity(detailsIntent);
            }
        }.execute();
    }

    private void continueFollow() {
        bStartStop.setText("Stop");
        rectOptions = new PolylineOptions();

        new AsyncTask<Void, Void, List<Loc>>() {
            @Override
            protected List<Loc> doInBackground(Void... voids) {
                way = wayDao.getLastWay();
                wayID = way.getId();
                startTime = locDao.getFirstLocById(wayID).getTime();
                lat1 = locDao.getLastLoc().getLatitude();
                lon1 = locDao.getLastLoc().getLongitude();
                return locDao.getLocsForWayID(wayID);
            }

            @Override
            protected void onPostExecute(List<Loc> locs) {
                distance = 0;
                for (int i = 0; i < locs.size() - 1; i++) {
                    distance += haversine(locs.get(i).getLatitude(), locs.get(i).getLongitude(), locs.get(i + 1).getLatitude(), locs.get(i + 1).getLongitude());
                }


                Log.e(TAG, "onPostExecute: distance" + distance);
                for (int i = 0; i < locs.size(); i++)
                    drawRoute(locs.get(i).getLatitude(), locs.get(i).getLongitude());
                startService(locationService);
                registerReceiver(broadcastReceiver, intentFilter);
                handler.postDelayed(runnable, 1000);
            }
        }.execute();
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long milisecondTime = Calendar.getInstance().getTimeInMillis() - startTime;
            int seconds = (int) (milisecondTime / 1000);
            Log.d(TAG, "run: " + milisecondTime);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds %= 60;
            minutes %= 60;
            hours %= 60;
            textViewTime.setText(String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
            handler.postDelayed(this, 1000);
        }
    };

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

    private void drawRoute(double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        rectOptions.add(latLng);
        mMap.addPolyline(rectOptions);
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
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Log.d(TAG, "can't unregister receiver");
        }
        try {
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
            Log.d(TAG, "can't remove callbacks");
        }
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        loadState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Log.d(TAG, "can't unregister receiver");
        }
        try {
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
            Log.d(TAG, "can't remove callbacks");
        }
        saveState();
    }


}

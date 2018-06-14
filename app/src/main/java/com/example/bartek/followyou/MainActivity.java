package com.example.bartek.followyou;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.Database.Way;
import com.example.bartek.followyou.Database.WayDao;
import com.example.bartek.followyou.DetailActivities.DetailsActivity;
import com.example.bartek.followyou.History.HistoryActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
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
    public static final String SharedTag = "SharedPreferencesFollowMe";
    public static final String SharedFollowMeIsStarted = "followIsStarted";

    private GoogleMap mMap;
    private LatLng latLng;
    private PolylineOptions pointOptions;
    private boolean permissionGranted, followMeisStarted = false;
    private Button bStartStop;
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewAvgSpeed;
    private Context context;
    private AppDatabase database;
    private WayDao wayDao;
    private LocDao locDao;
    private Way way;
    private int wayID;
    private LocationManager locationManager;
    private Intent locationService, historyIntent, detailsIntent;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter = new IntentFilter(Filter);
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Handler handler = new Handler();
    private long startTime, difftime;
    private double spped, avgspeed, distance, lastLat, lat, lastLon, lon;
    private ProgressDialog progressDialog;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //bHistory.setText(R.string.title_home);
                    return true;
                case R.id.navigation_history:
                    //bHistory.setText(R.string.title_dashboard);
                    finish();
                    startActivity(historyIntent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        bStartStop = (Button) findViewById(R.id.buttonStartStop);
        bStartStop.setOnClickListener(new bStartStopClick());
        textViewSpeed = (TextView) findViewById(R.id.textViewSpeed);
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewDistance = (TextView) findViewById(R.id.textViewDistance);
        textViewAvgSpeed = (TextView) findViewById(R.id.textViewAvgSpeed);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        checkPermissions();
        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        wayDao = database.wayDao();
        locDao = database.locDao();

        sharedPreferences = getSharedPreferences(SharedTag, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        initializeIntents();
        progressDialog = new ProgressDialog(this);

        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
        pointOptions = new PolylineOptions();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: ");
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        spped = locDao.getLastLoc().getSpeed();
                        spped *= 3.6;
                        lat = locDao.getLastLoc().getLatitude();
                        lon = locDao.getLastLoc().getLongitude();
                        if (lastLat != 0) {
                            distance += haversine(lastLat, lastLon, lat, lon);
                        }
                        lastLat = lat;
                        lastLon = lon;
                        difftime = locDao.getLastLoc().getTime() - locDao.getFirstLocById(wayID).getTime();
                        avgspeed = ((distance * 1000) / (difftime / 1000)) * 3.6;
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        textViewSpeed.setText(String.format("%.2f", spped) + " km/h");
                        textViewDistance.setText(String.format("%.2f", distance) + " km");
                        textViewAvgSpeed.setText(String.format("%.2f", avgspeed) + " km/h");
                        drawRoute(lat, lon);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 13));
                    }
                }.execute();
            }
        };
        Log.d(TAG, "onCreate: ");
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }mMap.setMyLocationEnabled(true);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
            }
        });
    }

    private void initializeIntents() {
        locationService = new Intent(this, LocationService.class);
        historyIntent = new Intent(this, HistoryActivity.class);
        detailsIntent = new Intent(this, DetailsActivity.class);
    }

    private class bStartStopClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!followMeisStarted) {
                if (!checkisGPSenabled()) buildAlertMessageNoGps();
                else startFollow();
            } else{
                stopFollow();
                clearData();
            }
        }
    }

    private void startFollow() {
        if (!checkPermissions()) return;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                followMeisStarted = true;
                bStartStop.setText("Stop");
                bStartStop.setBackgroundResource(R.drawable.stop_button);
                pointOptions = new PolylineOptions();
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
        lastLat = 0;
        lastLon = 0;
        bStartStop.setText("Start");
        bStartStop.setBackgroundResource(R.drawable.start_button);
        followMeisStarted = false;
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
        bStartStop.setBackgroundResource(R.drawable.stop_button);
        pointOptions = new PolylineOptions();

        new AsyncTask<Void, Void, List<Loc>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog.setMessage("Please wait.");
                progressDialog.show();
            }

            @Override
            protected List<Loc> doInBackground(Void... voids) {
                way = wayDao.getLastWay();
                wayID = way.getId();
                try {
                    startTime = locDao.getFirstLocById(wayID).getTime();
                    lat = locDao.getLastLoc().getLatitude();
                    lon = locDao.getLastLoc().getLongitude();
                } catch (Exception e) {
                    startTime = Calendar.getInstance().getTimeInMillis();
                    startService(locationService);
                    registerReceiver(broadcastReceiver, intentFilter);
                    handler.postDelayed(runnable, 1000);
                    cancel(true);
                    progressDialog.dismiss();
                }
                return locDao.getLocsForWayID(wayID);
            }

            @Override
            protected void onPostExecute(final List<Loc> locs) {

                new AsyncTask<List<Loc>, Void, Void>(){
                    @Override
                    protected Void doInBackground(List<Loc>[] lists) {
                        distance = 0;
                        for (int i = 0; i < locs.size() - 1; i++) {
                            distance += haversine(locs.get(i).getLatitude(), locs.get(i).getLongitude(), locs.get(i + 1).getLatitude(), locs.get(i + 1).getLongitude());
                        }
                        Log.e(TAG, "onPostExecute: distance" + distance);
                        for (int i = 0; i < locs.size(); i++){
                            latLng = new LatLng(locs.get(i).getLatitude(), locs.get(i).getLongitude());
                            pointOptions.add(latLng);
                        }
                        startService(locationService);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        registerReceiver(broadcastReceiver, intentFilter);
                        handler.postDelayed(runnable, 1000);
                        mMap.addPolyline(pointOptions);
                        progressDialog.dismiss();
                    }
                }.execute(locs);
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

    private void clearData(){
        spped = avgspeed = difftime = 0;
        distance = 0;
        textViewTime.setText("00:00:00");
        textViewDistance.setText("0.00 km");
        textViewSpeed.setText("0.00 km/h");
        textViewAvgSpeed.setText("0.00 km/h");
    }

    private void saveState() {
        editor.putBoolean(SharedFollowMeIsStarted, followMeisStarted);
        editor.commit();
    }

    private void loadState() {
        followMeisStarted = sharedPreferences.getBoolean(SharedFollowMeIsStarted, false);
        Log.d(TAG, "loadState: " + followMeisStarted);
        if (followMeisStarted) continueFollow();
    }

    private boolean checkisGPSenabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return true;
        return false;
    }

    private void drawRoute(double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        pointOptions.add(latLng);
        mMap.addPolyline(pointOptions);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
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
        //followMeisStarted = false;
        saveState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}

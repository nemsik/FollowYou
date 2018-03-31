package com.example.bartek.followyou.DetailActivities;

import android.arch.persistence.room.Room;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.bartek.followyou.Database.AppDatabase;
import com.example.bartek.followyou.Database.Loc;
import com.example.bartek.followyou.Database.LocDao;
import com.example.bartek.followyou.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import static com.example.bartek.followyou.MainActivity.NAME_DATABASE;

/**
 * Created by bartek on 31.03.2018.
 */

public class DetailsMapActivity extends Fragment {

    private final static String TAG = "DetailsMaps";
    private GoogleMap mMap;
    private MapView mMapView;
    private PolylineOptions rectOptions;
    private int wayID;
    private AppDatabase database;
    private LocDao locDao;
    private List<Loc> locList;
    private double lat, lon;
    private LatLng latLng;

    public static DetailsMapActivity newInstance(int wayID) {
        DetailsMapActivity fragmentMaps = new DetailsMapActivity();
        Bundle args = new Bundle();
        args.putInt("wayID", wayID);
        fragmentMaps.setArguments(args);
        return fragmentMaps;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wayID = getArguments().getInt("wayID", 0);
        Log.e(TAG, "Way id " + wayID);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_details_map, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();

        database = Room.databaseBuilder(getContext(), AppDatabase.class, NAME_DATABASE)
                .fallbackToDestructiveMigration().build();
        locDao = database.locDao();

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mMap.getUiSettings().setScrollGesturesEnabled(false);
                        mMap.getUiSettings().setZoomGesturesEnabled(false);
                        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker) {
                                marker.showInfoWindow();
                                return true;
                            }
                        });
                        new AsyncTask<Void, Void, List<Loc>>() {
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
                    }
                });
            }
        });
        return view;
    }

    private void setGui() {
        int locListSize = locList.size();
        rectOptions = new PolylineOptions();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < locListSize; i++) {
            lat = locList.get(i).getLatitude();
            lon = locList.get(i).getLongitude();
            latLng = new LatLng(lat, lon);
            builder.include(latLng);
            rectOptions.add(latLng);
        }
        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.12);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cameraUpdate);
        mMap.addPolyline(rectOptions);
        Marker startMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(locList.get(0).getLatitude(), locList.get(0).getLongitude())).title("Start"));
        Marker endMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(locList.get(locListSize - 1).getLatitude(), locList.get(locListSize - 1).getLongitude())).title("End"));
        startMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        startMarker.showInfoWindow();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}

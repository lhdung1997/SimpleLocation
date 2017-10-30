package com.example.hoangdung.simplelocation;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    //For Debugging
    private String TAG = MapsActivity.class.getSimpleName();
    private int DEFAULT_ZOOM = 15;
    //Google Map model
    private GoogleMap mMap;

    //Fused Location Provider
    private FusedLocationProviderClient mLocationProvider;

    //Last known location
    private Location mLastknownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mLocationProvider = LocationServices.getFusedLocationProviderClient(this);
    }//onCreate


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateUI();
        showLastknownLocation();
    }//onMapReady

    private void showLastknownLocation() {
       try{
           Task<Location> lastLocation = mLocationProvider.getLastLocation();
           lastLocation.addOnCompleteListener(this, new OnCompleteListener<Location>() {
               @Override
               public void onComplete(@NonNull Task<Location> task) {
                   if (task.isComplete() && task.isSuccessful()) {
                       mLastknownLocation = task.getResult();
                       mMap.moveCamera(CameraUpdateFactory
                               .newLatLngZoom(
                                       new LatLng(mLastknownLocation.getLatitude(), mLastknownLocation.getLongitude())
                                       , DEFAULT_ZOOM));
                   }
               }
           });
       }
       catch (SecurityException e){

       }

    }// showLastknowLocation

    private void updateUI() {
        try{
           mMap.setMyLocationEnabled(true);
        }
        catch (SecurityException e){

        }
    }
}

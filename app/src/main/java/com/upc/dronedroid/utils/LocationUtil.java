package com.upc.dronedroid.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

public class LocationUtil {

    private static LocationUtil locationUtilInstance;
    private String chosenLocationProvider;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private LocationUtil(){

    }

    public static LocationUtil getLocationUtilInstance(Activity activity){
        if(locationUtilInstance == null){
            locationUtilInstance = new LocationUtil();
            locationUtilInstance.setPreferredProvider(activity);
        }
        return locationUtilInstance;
    }


    @SuppressLint("MissingPermission")
    public void setPreferredProvider(Activity activity) {

        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        //Create both location listeners
        LocationListener gpsLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d("INFO", "Location by GPS is: lat: " + location.getLatitude() + ", longitude: " + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        LocationListener networkLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.d("INFO", "Location by network is: lat: " + location.getLatitude() + ", longitude: " + location.getLongitude());
                Intent intent = new Intent();
                intent.setAction("com.upc.dronedroid.UPDATE_LOCATION");
                intent.putExtra("latitude", location.getLatitude());
                intent.putExtra("longitude", location.getLongitude());
                activity.sendBroadcast(intent);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        //Request location using both providers
        if (hasGps) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    gpsLocationListener
            );
        }
        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0F,
                    networkLocationListener
            );

        }
        //Compare accuracy for both providers and determine which one is going to be used
        Location lastKnownLocationByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownLocationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (lastKnownLocationByGps != null && lastKnownLocationByNetwork != null) {
            if (lastKnownLocationByGps.getAccuracy() > lastKnownLocationByNetwork.getAccuracy()) {
                this.chosenLocationProvider = LocationManager.GPS_PROVIDER;
                this.locationListener = gpsLocationListener;
            } else {
                this.chosenLocationProvider = LocationManager.NETWORK_PROVIDER;
                this.locationListener = networkLocationListener;
            }
        } else if (lastKnownLocationByGps == null) {
            this.chosenLocationProvider = LocationManager.NETWORK_PROVIDER;
            this.locationListener = networkLocationListener;
        } else {
            this.chosenLocationProvider = LocationManager.GPS_PROVIDER;
            this.locationListener = gpsLocationListener;
        }
    }

    @SuppressLint("MissingPermission")
    public double[] getLocation() {

        locationManager.requestLocationUpdates(
                chosenLocationProvider,
                5000,
                0F,
                locationListener
        );
        Location loc = locationManager.getLastKnownLocation(chosenLocationProvider);
        double[] cords = new double[2];
        cords[0] = loc.getLatitude();
        cords[1] = loc.getLongitude();
        return cords;
    }

    public static boolean hasArrived(double latDestination, double lonDestination, double latCurrent, double lonCurrent){
        double latitudeDiff = latCurrent - latDestination;
        if (latitudeDiff == 0.0) {
            latitudeDiff = latDestination - latCurrent;
        }
        double longitudeDiff = lonCurrent - lonDestination;
        if (longitudeDiff == 0.0) {
            longitudeDiff = lonDestination - lonCurrent;
        }
        return latitudeDiff < 10E-2 && latitudeDiff > -10E-2 && longitudeDiff < 10E-2 && longitudeDiff > -10E-2;
    }
}

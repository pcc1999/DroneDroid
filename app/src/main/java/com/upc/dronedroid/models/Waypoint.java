package com.upc.dronedroid.models;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

import java.util.LinkedList;
import java.util.List;

public class Waypoint {
    private GeoPoint coords;
    private Boolean takePicture;

    public GeoPoint getCoords() {
        return coords;
    }

    public void setCoords(GeoPoint coords) {
        this.coords = coords;
    }

    public Boolean getTakePicture() {
        return takePicture;
    }

    public void setTakePicture(Boolean takePicture) {
        this.takePicture = takePicture;
    }

    public Waypoint(double latitude, double longitude, Boolean takePicture) {
        this.coords = new GeoPoint(latitude, longitude);
        this.takePicture = takePicture;
    }

    public static List<Waypoint> getWPsFromJSONArray(JSONArray jsonArray) throws JSONException {
        Gson gson = new Gson();
        List<Waypoint> waypoints = new LinkedList<>();
        //Check if waypoints are GeoPoint or simple lat lon fields
        try {
            jsonArray.getJSONObject(1).getJSONObject("coords");
            //If no exception it is from GeoPoint
            //If exception, take as simple properties
            for(int i = 0; i < jsonArray.length(); i++){
                Waypoint waypoint = gson.fromJson(jsonArray.getJSONObject(i).toString(), Waypoint.class);
                /*
                GeoPoint geoPoint = gson.fromJson(jsonArray.getJSONObject(i).getJSONObject("coords").toString(), GeoPoint.class);
                Waypoint waypoint = new Waypoint(
                        geoPoint.getLatitude(),
                        geoPoint.getLongitude(),
                        jsonArray.getJSONObject(i).getBoolean("takePicture")
                );*/
                waypoints.add(waypoint);
            }
        } catch (JSONException e) {
            //If exception, take as simple properties
            for(int i = 0; i < jsonArray.length(); i++){
                Waypoint waypoint = new Waypoint(
                        jsonArray.getJSONObject(i).getDouble("lat"),
                        jsonArray.getJSONObject(i).getDouble("lon"),
                        jsonArray.getJSONObject(i).getBoolean("takePic")
                );
                waypoints.add(waypoint);
            }
        }
        return waypoints;
    }

}



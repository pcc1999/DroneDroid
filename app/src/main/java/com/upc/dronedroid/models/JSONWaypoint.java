package com.upc.dronedroid.models;

import org.osmdroid.util.GeoPoint;

public class JSONWaypoint {
    private double lat;
    private double lon;
    private Boolean takePic;

    public JSONWaypoint(GeoPoint coords, Boolean takePic){
        this.lat = coords.getLatitude();
        this.lon = coords.getLongitude();
        this.takePic = takePic;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Boolean getTakePic() {
        return takePic;
    }

    public void setTakePic(Boolean takePic) {
        this.takePic = takePic;
    }
}

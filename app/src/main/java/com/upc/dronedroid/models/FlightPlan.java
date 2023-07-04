package com.upc.dronedroid.models;

import com.upc.dronedroid.utils.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FlightPlan implements Comparable {
    public String id;
    public String name;
    public Date creationDate;
    public Date lastModifiedDate;
    public List<Waypoint> waypoints;
    protected String filename;

    public FlightPlan(String id, String name, Date creationDate, Date lastModifiedDate, List<Waypoint> waypoints, String filename) {
        this.id = id;
        this.name = name;
        this.creationDate = creationDate;
        this.lastModifiedDate = lastModifiedDate;
        this.waypoints = waypoints;
        this.filename = filename;
    }

    public FlightPlan() {
        this.id = UUID.randomUUID().toString();
        this.creationDate = new Date();
        this.waypoints = new ArrayList<>();
    }

    public static FlightPlan getFlightPlanFromJSON(JSONObject jsonFP, String filename) throws JSONException {
        return new FlightPlan(
                jsonFP.getString("id"),
                jsonFP.getString("name"),
                DateUtil.convertStringToDate(jsonFP.getString("creationDate")),
                DateUtil.convertStringToDate(jsonFP.getString("lastModifiedDate")),
                Waypoint.getWPsFromJSONArray(jsonFP.getJSONArray("waypoints")),
                filename
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = DateUtil.convertStringToDate(creationDate);
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = DateUtil.convertStringToDate(lastModifiedDate);
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addWaypoint(Waypoint wp) {
        this.waypoints.add(wp);
    }

    public JSONFlightPlan convertToJSONFlightPlan() {
        return new JSONFlightPlan(this.id, this.name, this.creationDate, this.lastModifiedDate, this.waypoints);
    }

    @Override
    public int compareTo(Object o) {
        FlightPlan flightPlan = (FlightPlan) o;
        long compareTo = flightPlan.getLastModifiedDate().getTime();
        return (int) (compareTo - this.getLastModifiedDate().getTime());
    }
}


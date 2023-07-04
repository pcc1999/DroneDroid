package com.upc.dronedroid.models;

import com.upc.dronedroid.utils.DateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JSONFlightPlan {
    private String id;
    private String name;
    private String creationDate;
    private String lastModifiedDate;
    private List<JSONWaypoint> waypoints;

    public JSONFlightPlan(String id, String name, Date creationDate, Date lastModifiedDate, List<Waypoint> waypointList){
        this.id = id;
        this.name = name;
        this.creationDate = DateUtil.convertDateToString(creationDate);
        this.lastModifiedDate = DateUtil.convertDateToString(lastModifiedDate);
        this.waypoints = new ArrayList<>();
        for(Waypoint waypoint : waypointList){
            this.waypoints.add(new JSONWaypoint(waypoint.getCoords(), waypoint.getTakePicture()));
        }
    }
    public List<JSONWaypoint> getWaypoints(){
        return this.waypoints;
    }
}

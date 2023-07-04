package com.upc.dronedroid.utils;

import android.graphics.drawable.Drawable;

import com.upc.dronedroid.models.Waypoint;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapUtil {

    // moveDrone function: given a map, id and new position, moves the marker for the corresponding id
    // to the new position in the given map
    public static boolean moveMarker(MapView mapView, String id, double newLat, double newLon) {
        boolean ret = false;
        int c = 0;
        while (c < mapView.getOverlayManager().overlays().size()) {
            if (mapView.getOverlayManager().overlays().get(c).getClass().getName().equals("org.osmdroid.views.overlay.Marker")) {
                Marker marker = (Marker) mapView.getOverlayManager().overlays().get(c);
                if (marker.getId().equals(id)) {
                    marker.setPosition(new GeoPoint(newLat, newLon));
                    ret = true;
                    break;
                }
            }
            c++;
        }
        mapView.invalidate();
        return ret;
    }

    public static void setTiles(MapView mapView, OnlineTileSourceBase tile) {
        mapView.setTileSource(tile);
        mapView.invalidate();
    }

    public static boolean deleteWaypoints(MapView mapView) {
        boolean ret = false;
        for (Overlay overlay : mapView.getOverlayManager().overlays()) {
            switch (overlay.getClass().getName()) {
                case "org.osmdroid.views.overlay.Marker": {
                    Marker marker = (Marker) overlay;
                    if (marker.getId().equals("waypoint")) {
                        ret = true;
                        mapView.getOverlayManager().overlays().remove(overlay);
                    }
                    break;
                }
                case "org.osmdroid.views.overlay.Polyline": {
                    mapView.getOverlayManager().overlays().remove(overlay);
                }
            }
        }
        mapView.invalidate();
        return ret;
    }

    public static void setWaypointsMarkers(Drawable marker, MapView mapView, List<Waypoint> waypointList) {
        deleteWaypoints(mapView);
        List<GeoPoint> pointList = new ArrayList<>();
        for (Waypoint waypoint : waypointList) {
            Marker userMarker = new Marker(mapView);
            userMarker.setIcon(marker);
            userMarker.setId("waypoint");
            userMarker.setOnMarkerClickListener(null);
            userMarker.setOnMarkerDragListener(null);
            userMarker.setInfoWindow(null);
            userMarker.setPosition(waypoint.getCoords());
            mapView.getOverlayManager().add(userMarker);
            pointList.add(waypoint.getCoords());
        }
        //Once waypoints have been set, set also the route between them
        Polyline polyline = new Polyline();
        polyline.setPoints(pointList);
        mapView.getOverlayManager().add(polyline);
    }

}

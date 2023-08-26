package com.upc.dronedroid.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upc.dronedroid.R;
import com.upc.dronedroid.databinding.ActivityFlightPlansTabletBinding;
import com.upc.dronedroid.models.FlightPlan;
import com.upc.dronedroid.models.JSONFlightPlan;
import com.upc.dronedroid.models.Waypoint;
import com.upc.dronedroid.ui.utils.MyRecyclerViewAvailableFPsAdapter;
import com.upc.dronedroid.ui.utils.MyRecyclerViewWaypointsAdapter;
import com.upc.dronedroid.utils.AssetsUtil;
import com.upc.dronedroid.utils.ConnectionUtil;
import com.upc.dronedroid.utils.LocationUtil;
import com.upc.dronedroid.utils.MapUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import timber.log.Timber;

public class FlightPlansTabletActivity extends AppCompatActivity {

    public final BroadcastReceiver brChangeLocation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0.0f);
            double longitude = intent.getDoubleExtra("longitude", 0.0f);
            MapView map = findViewById(R.id.OSMView);
            MapUtil.moveMarker(map, "user", latitude, longitude);
        }
    };
    public final BroadcastReceiver brDeletedWaypoint = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String jsonCurrentWaypoints = intent.getStringExtra("currentWaypoints");
            MapView map = findViewById(R.id.OSMView);
            try {
                JSONArray waypointsJSONArray = new JSONArray(jsonCurrentWaypoints);
                List<Waypoint> waypointsList = Waypoint.getWPsFromJSONArray(waypointsJSONArray);
                MapUtil.setWaypointsMarkers(ContextCompat.getDrawable(context, R.drawable.ic_pin_1_svgrepo_com), map, waypointsList);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
    private final List<FlightPlan> availableFPs = new LinkedList<>();
    ActivityFlightPlansTabletBinding fpTabletBinding;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intentResult = result.getData();
                    assert intentResult != null;
                    Uri uri = intentResult.getData();
                    if (uri.toString().substring(uri.toString().lastIndexOf(".") + 1).equals("json")) {
                        String src = correctUriForImport(uri);
                        File importedFile = new File(Objects.requireNonNull(Objects.requireNonNull(src).split("\\.")[0]));
                        String json = AssetsUtil.loadJSONFromFile(this, null, Uri.fromFile(importedFile));
                        try {
                            FlightPlan importedFlightPlan = FlightPlan.getFlightPlanFromJSON(new JSONObject(Objects.requireNonNull(json)), importedFile.getName());
                            //Save into internal storage
                            AssetsUtil.saveJSON(this, importedFlightPlan.getFilename(), "FlightPlans", json);
                            availableFPs.add(importedFlightPlan);
                            //Notify availableFPs adapter that a new item has been added
                            RecyclerView availableFPsRecyclerView = findViewById(R.id.availableFPs);
                            MyRecyclerViewAvailableFPsAdapter availableFPsAdapter = (MyRecyclerViewAvailableFPsAdapter) availableFPsRecyclerView.getAdapter();
                            assert availableFPsAdapter != null;
                            availableFPsAdapter.notifyItemInserted(availableFPs.size());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Timber.tag("INFO LOADING FILE").d("File loaded successfully");
                    } else {
                        Snackbar.make(findViewById(R.id.flightPlansMainLayoutTable), "Please use only files with .json extension", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private String correctUriForImport(Uri uri) {
        String lastPath = "";
        //Get last part of path and redirect to Downloads folder
        if (uri.toString().contains("home%3A") && uri.toString().contains("com.android.externalstorage")) {
            //from root of any folder
            String[] parts = uri.toString().split("/");
            lastPath = parts[parts.length - 1].split("%3A")[parts[parts.length - 1].split("%3A").length - 1];
        } else {
            //from subfolder of any folder
        }
        if (uri.toString().contains("downloads")) {
            return Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + lastPath;
        } else if (uri.toString().contains("documents")) {
            return Environment.getExternalStorageDirectory().getPath() + "/" + Environment.DIRECTORY_DOCUMENTS + "/" + lastPath;
        } else {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fpTabletBinding = DataBindingUtil.setContentView(this, R.layout.activity_flight_plans_tablet);

        //Initialize Bindings
        fpTabletBinding.setFlightPlanSelected(null);
        fpTabletBinding.setFlightPlanPositionSelected(-1);
        fpTabletBinding.setIsEditable(false);
        fpTabletBinding.setIsCreating(false);
        fpTabletBinding.setCanRunPlan(getIntent().getBooleanExtra("canRunPlan", false));

        //Hide action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        //Get current location from locationUtil
        LocationUtil locationUtil = LocationUtil.getLocationUtilInstance(this);
        double[] latlon = new double[2];
        try {
            latlon = locationUtil.getLocation();
        } catch (Exception e) {
            e.printStackTrace();
            //Fallback to DroneLab
            latlon[0] = 41.276271;
            latlon[1] = 1.988435;
        }

        //Set marker for user's position
        MapView mapView = findViewById(R.id.OSMView);
        Marker userMarker = new Marker(mapView);
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_pin_svgrepo_com));
        userMarker.setId("user");
        userMarker.setOnMarkerClickListener(null);
        userMarker.setOnMarkerDragListener(null);
        userMarker.setInfoWindow(null);
        userMarker.setPosition(new GeoPoint(latlon[0], latlon[1]));
        mapView.getOverlayManager().add(userMarker);
        //And initialize the map
        MapController mapController = new MapController(mapView);
        GeoPoint startPoint = new GeoPoint(latlon[0], latlon[1]);
        mapController.setZoom(18);
        mapController.setCenter(startPoint);

        //Set the listener for location changes
        IntentFilter locationFilter = new IntentFilter("com.upc.dronedroid.UPDATE_LOCATION");
        ContextCompat.registerReceiver(this, brChangeLocation, locationFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        //Set the listener for waypoints deletion
        IntentFilter deleteFilter = new IntentFilter("com.upc.dronedroid.WAYPOINT_DELETED");
        ContextCompat.registerReceiver(this, brDeletedWaypoint, deleteFilter, ContextCompat.RECEIVER_NOT_EXPORTED);


        //TODO: The map tiles in this activity should be the same as were set up in the previous activity - Must be informed through intent
        loadFPsList(mapView, mapController);
    }

    public void onNewClick(View v) {
        MapView mapView = findViewById(R.id.OSMView);
        //Check if creating. If not, create new FlightPlan.
        if (!fpTabletBinding.getIsCreating()) {
            //In any case, create first the empty flight plan
            FlightPlan newFlightPlan = new FlightPlan();
            //Show dialog to determine if loaded from device or new from scratch
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
            bottomSheetDialog.setContentView(R.layout.choose_new_fp_dialog);
            FrameLayout bottomSheetLayout = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior<FrameLayout> behaviour = BottomSheetBehavior.from(bottomSheetLayout);
            behaviour.setState(BottomSheetBehavior.STATE_EXPANDED);

            LinearLayout fromLocal = bottomSheetDialog.findViewById(R.id.fromLocal);
            LinearLayout fromScratch = bottomSheetDialog.findViewById(R.id.fromScratch);

            Activity that = this;
            OnClickListener fromLocalClickListener = view -> {
                bottomSheetDialog.dismiss();
                //From local storage
                //1. Open file explorer to select file
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //Android does not support application/json - workaround and handle when receiving data
                intent.setType("*/*");
                intent = Intent.createChooser(intent, "Choose a file");
                launcher.launch(intent);
            };
            OnClickListener fromScratchClickListener = view -> {
                bottomSheetDialog.dismiss();
                //From scratch
                //1. Set isCreating and isEditable booleans true. Set new flight plan as selected and position size + 1
                fpTabletBinding.setIsCreating(true);
                fpTabletBinding.setFlightPlanSelected(newFlightPlan);
                fpTabletBinding.setFlightPlanPositionSelected(availableFPs.size() + 1);
                //2. Handle map
                //2.1 Initialize map attributes
                mapView.setClickable(true);
                mapView.setFocusable(true);
                mapView.setFocusableInTouchMode(true);
                //MapUtil.deleteWaypoints(mapView);
                //2.2 Add touch overlay for the map
                Overlay touchOverlay = new Overlay() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView1) {
                        Projection proj = mapView1.getProjection();
                        double waypointLatitude = Double.parseDouble(String.valueOf(proj.fromPixels((int) e.getX(), (int) e.getY()).getLatitude()).substring(0, 9));
                        double waypointLongitude = Double.parseDouble(String.valueOf(proj.fromPixels((int) e.getX(), (int) e.getY()).getLongitude()).substring(0, 9));
                        GeoPoint location = new GeoPoint(waypointLatitude, waypointLongitude);
                        newFlightPlan.addWaypoint(new Waypoint(location.getLatitude(), location.getLongitude(), false));
                        //Update flight plan to show waypoints in Recycler View
                        fpTabletBinding.setFlightPlanSelected(newFlightPlan);
                        createWaypointsAdapter(newFlightPlan.getWaypoints(), that, mapView1, new MapController(mapView1), 1);
                        //Add waypoint to the map
                        MapUtil.setWaypointsMarkers(ContextCompat.getDrawable(that, R.drawable.ic_pin_1_svgrepo_com), mapView1, newFlightPlan.getWaypoints());
                        //Invalidate map to show new items
                        mapView1.invalidate();
                        return true;
                    }
                };
                mapView.getOverlayManager().add(touchOverlay);
                //3. Clear adapter for waypoints
                createWaypointsAdapter(newFlightPlan.getWaypoints(), that, mapView, new MapController(mapView), 1);
            };

            fromLocal.setOnClickListener(fromLocalClickListener);
            fromScratch.setOnClickListener(fromScratchClickListener);

            bottomSheetDialog.show();
        } else {
            //If creating, save changes to new file
            FlightPlan newFlightPlan = fpTabletBinding.getFlightPlanSelected();
            //Check if the plan has a name, if so, save
            if (!Objects.equals(newFlightPlan.getName(), null) && !Objects.equals(newFlightPlan.getName(), "")) {
                newFlightPlan.setLastModifiedDate(String.valueOf(new Date()));
                Gson gson = new Gson();
                String flightPlanJSONString = gson.toJson(newFlightPlan.convertToJSONFlightPlan());
                AssetsUtil.saveJSON(this, newFlightPlan.getName(), "FlightPlans", flightPlanJSONString);
                //And set layout for new flight plan
                mapView.setClickable(false);
                mapView.setFocusable(false);
                mapView.setFocusableInTouchMode(false);
                fpTabletBinding.setIsCreating(false);
                //Remove all the overlays except the one that shows the user's position
                List<Overlay> overlayList = mapView.getOverlayManager().overlays();
                for (Overlay overlay : overlayList) {
                    if (overlay.getClass().getName().equals("org.osmdroid.views.overlay.Marker")) {
                        Marker marker = (Marker) overlay;
                        if (!marker.getId().equals("user")) {
                            mapView.getOverlayManager().overlays().remove(overlay);
                        }
                    } else {
                        mapView.getOverlayManager().overlays().remove(overlay);
                    }
                }
                loadFPsList(mapView, new MapController(mapView));
            } else {
                Snackbar.make(findViewById(R.id.flightPlansMainLayoutTable), "Name is mandatory for new flight plans", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public void onDeleteClick(View v) {
        RecyclerView availableFPsRecyclerView = findViewById(R.id.availableFPs);
        MyRecyclerViewAvailableFPsAdapter availableFPsAdapter = (MyRecyclerViewAvailableFPsAdapter) availableFPsRecyclerView.getAdapter();
        RecyclerView waypointsRecyclerView = findViewById(R.id.waypoints);
        MyRecyclerViewWaypointsAdapter waypointsAdapter = (MyRecyclerViewWaypointsAdapter) waypointsRecyclerView.getAdapter();
        //Check if editable/creating, if not, delete selected FlightPlan from app storage
        if (!(fpTabletBinding.getIsEditable() || fpTabletBinding.getIsCreating())) {
            if (AssetsUtil.deleteFile(this, fpTabletBinding.getFlightPlanSelected().getFilename())) {
                //Delete from list of available flightPlans
                availableFPs.remove(fpTabletBinding.getFlightPlanPositionSelected());
                assert availableFPsAdapter != null;
                availableFPsAdapter.notifyItemRemoved(fpTabletBinding.getFlightPlanPositionSelected());
                //Clear selection
                availableFPsAdapter.clearSelection();
            } else {
                Toast.makeText(this, "Error when deleting Flight Plan", Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                if (fpTabletBinding.getIsEditable()) {
                    //Discard changes (reload flightplan from storage, in case it was editing)
                    String json = AssetsUtil.loadJSONFromFile(this, fpTabletBinding.getFlightPlanSelected().getFilename(), null);
                    assert json != null;
                    FlightPlan oldFlightPlan = FlightPlan.getFlightPlanFromJSON(new JSONObject(json), fpTabletBinding.getFlightPlanSelected().getFilename());
                    availableFPs.set(fpTabletBinding.getFlightPlanPositionSelected(), oldFlightPlan);
                    assert availableFPsAdapter != null;
                    availableFPsAdapter.notifyItemChanged(fpTabletBinding.getFlightPlanPositionSelected());
                    fpTabletBinding.setIsEditable(false);
                    assert waypointsAdapter != null;
                    waypointsAdapter.toggleEditable();
                } else {
                    //Discard changes (destroy currently selected FlightPlan)
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onEditClick(View v) {
        //Check if editable. If not, set editable flag true
        if (!fpTabletBinding.getIsEditable()) {
            RecyclerView waypointsRecyclerView = findViewById(R.id.waypoints);
            MyRecyclerViewWaypointsAdapter waypointsAdapter = (MyRecyclerViewWaypointsAdapter) waypointsRecyclerView.getAdapter();
            fpTabletBinding.setIsEditable(true);
            assert waypointsAdapter != null;
            waypointsAdapter.toggleEditable();
            Activity that = this;
            MapView mapView = findViewById(R.id.OSMView);
            Overlay touchOverlay = new Overlay() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                    Projection proj = mapView.getProjection();
                    double waypointLatitude = Double.parseDouble(String.valueOf(proj.fromPixels((int) e.getX(), (int) e.getY()).getLatitude()).substring(0, 9));
                    double waypointLongitude = Double.parseDouble(String.valueOf(proj.fromPixels((int) e.getX(), (int) e.getY()).getLongitude()).substring(0, 9));
                    GeoPoint location = new GeoPoint(waypointLatitude, waypointLongitude);
                    fpTabletBinding.getFlightPlanSelected().addWaypoint(new Waypoint(location.getLatitude(), location.getLongitude(), false));
                    //Update flight plan to show waypoints in Recycler View
                    createWaypointsAdapter(fpTabletBinding.getFlightPlanSelected().getWaypoints(), that, mapView, new MapController(mapView), 1);
                    //Add waypoint to the map
                    MapUtil.setWaypointsMarkers(ContextCompat.getDrawable(that, R.drawable.ic_pin_1_svgrepo_com), mapView, fpTabletBinding.getFlightPlanSelected().getWaypoints());
                    //Invalidate map to show new items
                    mapView.invalidate();
                    return true;
                }
            };
            mapView.getOverlayManager().add(touchOverlay);
        } else {
            MapView mapView = findViewById(R.id.OSMView);
            //Save changes to file
            FlightPlan newFlightPlan = fpTabletBinding.getFlightPlanSelected();
            //Check if the plan has a name, if so, save
            if (!Objects.equals(newFlightPlan.getName(), null) && !Objects.equals(newFlightPlan.getName(), "")) {
                newFlightPlan.lastModifiedDate = new Date();
                Gson gson = new Gson();
                String flightPlanJSONString = gson.toJson(newFlightPlan.convertToJSONFlightPlan());
                AssetsUtil.saveJSON(this, newFlightPlan.getFilename(), "FlightPlans", flightPlanJSONString);
                //And set layout for new flight plan
                mapView.setClickable(false);
                mapView.setFocusable(false);
                mapView.setFocusableInTouchMode(false);
                fpTabletBinding.setIsEditable(false);
                //Remove all the overlays except the one that shows the user's position
                List<Overlay> overlayList = mapView.getOverlayManager().overlays();
                for (Overlay overlay : overlayList) {
                    if (overlay.getClass().getName().equals("org.osmdroid.views.overlay.Marker")) {
                        Marker marker = (Marker) overlay;
                        if (!marker.getId().equals("user")) {
                            mapView.getOverlayManager().overlays().remove(overlay);
                        }
                    } else {
                        mapView.getOverlayManager().overlays().remove(overlay);
                    }
                }/*
                //And reload flight plans adapter with new flight plan
                availableFPs.add(fpTabletBinding.getFlightPlanSelected());
                //And clear selection
                RecyclerView availableFPsRecyclerView = findViewById(R.id.availableFPs);
                MyRecyclerViewAvailableFPsAdapter availableFPsAdapter = (MyRecyclerViewAvailableFPsAdapter) availableFPsRecyclerView.getAdapter();
                assert availableFPsAdapter != null;
                availableFPsAdapter.clearSelection();*/
                loadFPsList(mapView, new MapController(mapView));
            } else {
                Snackbar.make(findViewById(R.id.flightPlansMainLayoutTable), "Name is mandatory for new flight plans", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public void onExportClick(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        File fileToExport = new File(getExternalFilesDir("FlightPlans") + "/" + fpTabletBinding.getFlightPlanSelected().getFilename());
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = Uri.parse(fileToExport.getAbsolutePath());
        } else {
            uri = Uri.fromFile(fileToExport);
        }
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "Share using"));
    }

    private void createWaypointsAdapter(List<Waypoint> waypointList, Activity activity, MapView mapView, MapController mapController, int action) {
        //Action - 0 for editing flight plans, 1 for creating flight plans
        //Set new adapter to the waypoints' Recycler View
        RecyclerView waypointsRecyclerView = findViewById(R.id.waypoints);
        LinearLayoutManager linearLayoutManagerWP = new LinearLayoutManager(activity);
        linearLayoutManagerWP.setOrientation(LinearLayoutManager.VERTICAL);
        waypointsRecyclerView.setLayoutManager(linearLayoutManagerWP);
        MyRecyclerViewWaypointsAdapter waypointsAdapter = new MyRecyclerViewWaypointsAdapter(activity, waypointList);
        waypointsAdapter.setOnItemClickListener((waypointPosition, waypointView) -> {
            //Set center on this WP
            mapController.setCenter(
                    waypointList.get(waypointPosition).getCoords()
            );
        });
        MapUtil.deleteWaypoints(mapView);
        if (action == 1) {
            //If creating, set creating boolean to the adapter
            waypointsAdapter.setIsCreating(true);
        }
        if (waypointsRecyclerView.getAdapter() != null) {
            waypointsRecyclerView.swapAdapter(waypointsAdapter, true);
        } else {
            waypointsRecyclerView.setAdapter(waypointsAdapter);
        }
    }

    public void onRunClick(View v) {
        JSONFlightPlan jsonFP = fpTabletBinding.getFlightPlanSelected().convertToJSONFlightPlan();
        ConnectionUtil.executeFlightPlan(jsonFP, this);
        onBackPressed();
    }

    public void loadFPsList(MapView mapView, MapController mapController) {
        //Load Flight Plans already present in assets of the app
        try {
            //First clear availableFPs list
            availableFPs.clear();
            //Then, load from external storage
            File directory = this.getExternalFilesDir("FlightPlans");
            String[] files = directory.list();
            assert files != null;
            for (String flightPlan : files) {
                String json = AssetsUtil.loadJSONFromFile(this, flightPlan, null);
                assert json != null;
                FlightPlan fp = FlightPlan.getFlightPlanFromJSON(new JSONObject(json), flightPlan);
                availableFPs.add(fp);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Snackbar.make(findViewById(R.id.flightPlansMainLayoutTable), "An error happened when loading flight plans", Snackbar.LENGTH_SHORT).show();
        }
        //Sort availableFPs based on its lastModifiedDate
        Collections.sort(availableFPs);

        //Set Adapter for AvailableFPs Recycler View
        RecyclerView availableFPsRecyclerView = findViewById(R.id.availableFPs);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        availableFPsRecyclerView.setLayoutManager(linearLayoutManager);
        MyRecyclerViewAvailableFPsAdapter availableFPsAdapter = new MyRecyclerViewAvailableFPsAdapter(this, availableFPs);
        availableFPsAdapter.setOnItemClickListener((FPPosition) -> {
            //If not creating or editing another flight plan
            if (!fpTabletBinding.getIsCreating() && !fpTabletBinding.getIsEditable()) {
                //Notify availableFPsAdapter
                availableFPsRecyclerView.post(availableFPsAdapter::notifyDataSetChanged);
                //Set as selected
                if (FPPosition != -1) {
                    fpTabletBinding.setFlightPlanSelected(availableFPsAdapter.getAvailableFlightPlan(FPPosition));
                    fpTabletBinding.setFlightPlanPositionSelected(FPPosition);
                    createWaypointsAdapter(availableFPsAdapter.getAvailableFlightPlan(FPPosition).getWaypoints(), this, mapView, mapController, 0);
                    //Set waypoints in map, center in first waypoint
                    MapUtil.setWaypointsMarkers(ContextCompat.getDrawable(this, R.drawable.ic_pin_1_svgrepo_com), mapView, fpTabletBinding.getFlightPlanSelected().getWaypoints());
                } else {
                    //Just rebind with no selection
                    fpTabletBinding.setFlightPlanSelected(null);
                    fpTabletBinding.setFlightPlanPositionSelected(-1);
                    //And delete all the waypoints in map
                    MapUtil.deleteWaypoints(mapView);
                }
            }
        });
        if (availableFPsRecyclerView.getAdapter() != null) {
            availableFPsRecyclerView.swapAdapter(availableFPsAdapter, true);
        } else {
            availableFPsRecyclerView.setAdapter(availableFPsAdapter);
        }
    }

}
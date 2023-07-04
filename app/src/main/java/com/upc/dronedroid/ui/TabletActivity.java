package com.upc.dronedroid.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;

import com.upc.dronedroid.BuildConfig;
import com.upc.dronedroid.R;
import com.upc.dronedroid.databinding.ActivityMainBinding;
import com.upc.dronedroid.utils.ConnectionUtil;
import com.upc.dronedroid.utils.LocationUtil;
import com.upc.dronedroid.utils.MapUtil;
import com.upc.dronedroid.utils.MqttClientUtil;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.net.ssl.SSLContext;

import timber.log.Timber;

public class TabletActivity extends AppCompatActivity {

    public final BroadcastReceiver brChangeLocation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0.0f);
            double longitude = intent.getDoubleExtra("longitude", 0.0f);
            MapView map = findViewById(R.id.OSMView);
            MapUtil.moveMarker(map, "user", latitude, longitude);
        }
    };
    private final HashMap<String, OnlineTileSourceBase> tilesMap = new HashMap<>();
    private boolean spinnerOpened = false;
    private boolean steeringWheelMode = false;
    private ActivityMainBinding tabletBinding;
    private final BroadcastReceiver brTelemetry = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String telemetryInfo;
            if (ConnectionUtil.isMQTTConnection()) {
                byte[] payload = intent.getByteArrayExtra("payload");
                telemetryInfo = new String(payload, StandardCharsets.UTF_8);
            } else {
                telemetryInfo = intent.getStringExtra("payload");
            }
            updateTelemetryInfo(telemetryInfo);
        }
    };
    private String pastDirection = "";
    private String pastYawRotation = "";
    private final SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            String direction = "";
            String yawRotation = "";
            if (steeringWheelMode) {
                if (2 < y && y < 10) {
                    Timber.tag("TestGyroSteering").d("Bearing counterclockwise");
                    //Send update to bear anticlockwise
                    yawRotation = "CounterClockwise";
                } else if (-10 < y && y < -2) {
                    Timber.tag("TestGyroSteering").d("Bearing clockwise");
                    //Send update to bear clockwise
                    yawRotation = "Clockwise";
                } else if (-2 < y && y < 2) {
                    Timber.tag("TestGyroSteering").d("Going straight");
                    //Do not send any update on heading (unless it is direct connection)
                    yawRotation = "Straight";
                }
                //Also check the x rotation to go front/back
                if (-6.0f < x && 0 > x) {
                    //Front
                    Timber.tag("TestGyroSteering").d("Going Front");
                    direction = "Front";
                } else if (-10f < x && -9.0f > x) {
                    //Back
                    Timber.tag("TestGyroSteering").d("Going Back");
                    direction = "Back";
                } else {
                    //Stop
                    Timber.tag("TestGyroSteering").d("Going STOP");
                    direction = "Stop";
                }
            } else {
                if (-7.0f < x && -4.0f > x) {
                    //No x inclination -> Could be STOP, EAST or WEST
                    if (-1.0f < y && 1.0f > y) {
                        //STOP
                        Timber.tag("Gyroscope Info").d("Going STOP");
                        direction = "Stop";
                    } else if (y <= -1.0f) {
                        //EAST
                        Timber.tag("Gyroscope Info").d("Going EAST");
                        direction = "East";
                    } else {
                        //It can only be WEST
                        Timber.tag("Gyroscope Info").d("Going WEST");
                        direction = "West";
                    }
                } else if (-10f < x && -7.0f > x) {
                    //"Negative" inclination on x -> Could be SOUTH, SOUTHEAST or SOUTHWEST
                    if (-1.0f < y && 1.0f > y) {
                        //SOUTH
                        Timber.tag("Gyroscope Info").d("Going SOUTH");
                        direction = "South";
                    } else if (y <= -1.0f) {
                        //SOUTHEAST
                        Timber.tag("Gyroscope Info").d("Going SOUTHEAST");
                        direction = "SouthEst";
                    } else {
                        //It can only be SOUTHWEST
                        Timber.tag("Gyroscope Info").d("Going SOUTHWEST");
                        direction = "SouthWest";
                    }
                } else {
                    //Otherwise it can only be NORTH, NORTHEAST of NORTHWEST
                    if (-1.0f < y && 1.0f > y) {
                        //NORTH
                        Timber.tag("Gyroscope Info").d("Going NORTH");
                        direction = "North";
                    } else if (y <= -1.0f) {
                        //NORTHEAST
                        Timber.tag("Gyroscope Info").d("Going NORTHEAST");
                        direction = "NorthEst";
                    } else {
                        //It can only be NORTHWEST
                        Timber.tag("Gyroscope Info").d("Going NORTHWEST");
                        direction = "NorthWest";
                    }
                }
            }
            if (!direction.equals(pastDirection)) {
                Log.d("SteeringWheelMode", "New direction detected: " + direction + ". Previous was: " + pastDirection);
                pastDirection = direction;
                ConnectionUtil.go(direction, null, steeringWheelMode);
            }
            if (!yawRotation.equals(pastYawRotation)) {
                pastYawRotation = yawRotation;
                ConnectionUtil.go(yawRotation, null, steeringWheelMode);
            }
        }
    };
    private Integer tileSelected = 0;
    private SensorManager sensorManager;
    private Sensor sensor;

    @BindingAdapter("tint")
    public static void setColorFilter(ImageView img, Integer color) {
        img.setColorFilter(color);
    }

    @BindingAdapter("iconTint")
    public static void setColor(ImageButton imgbtn, Integer color) {
        imgbtn.setColorFilter(color);
    }

    @BindingAdapter("srcCompat")
    public static void setSrcCompat(ImageButton imgbtn, Drawable drawable) {
        imgbtn.setImageDrawable(drawable);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "WrongConstant"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Timber.plant(new Timber.DebugTree());
        tabletBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        //Initialize Bindings
        tabletBinding.setIsBrokerConnected(false);
        tabletBinding.setIsAutopilotConnected(false);
        tabletBinding.setIsArmed(false);
        tabletBinding.setIsAir(false);
        tabletBinding.setIsFlying(false);
        tabletBinding.setShowTelemetryInfo(false);
        tabletBinding.setGyroscopeMode(false);
        tabletBinding.setIsArming(false);
        tabletBinding.setIsTakingOff(false);
        tabletBinding.setIsLanding(false);
        //Hide action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        MapView map = findViewById(R.id.OSMView);
        //set up tiles in splash screen
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setTilesScaledToDpi(true);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        //Populate tilesMap
        tilesMap.put("Open Street Maps", TileSourceFactory.MAPNIK);
        tilesMap.put("Topographic", TileSourceFactory.OpenTopo);
        tilesMap.put("Bing - Hybrid", null);
        tilesMap.put("Bing - Road", null);
        tilesMap.put("Bing - Aerial", null);
        // Workaround in order to force TLS v1.2 in Android 4.0 (minSDK is 15)
        try {
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
                 | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        // Related with visibility of maps either if tablet or if other device
        // If the device is a tablet we must handle the map
        LocationUtil locationUtilInstance = LocationUtil.getLocationUtilInstance(this);
        double[] latlon = new double[2];
        try {
            latlon = locationUtilInstance.getLocation();
        } catch (Exception e) {
            e.printStackTrace();
            //Fallback to DroneLab
            latlon[0] = 41.276271;
            latlon[1] = 1.988435;
        }

        //Set marker for user's position
        Marker userMarker = new Marker(map);
        userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_pin_svgrepo_com));
        //userMarker.setIcon(this.getResources().getDrawable(R.drawable.ic_map_pin_svgrepo_com));
        userMarker.setId("user");
        userMarker.setOnMarkerClickListener(null);
        userMarker.setOnMarkerDragListener(null);
        userMarker.setInfoWindow(null);
        userMarker.setPosition(new GeoPoint(latlon[0], latlon[1]));
        map.getOverlayManager().add(userMarker);

        //And initialize the map
        MapController mapController = new MapController(map);
        GeoPoint startPoint = new GeoPoint(latlon[0], latlon[1]);
        mapController.setZoom(18);
        mapController.setCenter(startPoint);

        String brokerSelected = getIntent().getStringExtra("brokerSelected");
        if (brokerSelected != null) {
            ConnectionUtil.getInstance(brokerSelected, this);
        }

        //Initialize sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Set the listener for location changes
        IntentFilter filter = new IntentFilter("com.upc.dronedroid.UPDATE_LOCATION");
        int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
        ContextCompat.registerReceiver(this.getApplicationContext(), brChangeLocation, filter, receiverFlags);

        //Set listener to steeringModeSwitch
        SwitchCompat steeringWheelSwitch = findViewById(R.id.steeringWheelSwitch);
        steeringWheelSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                steeringWheelMode = steeringWheelSwitch.isChecked();
                if (!ConnectionUtil.isMQTTConnection() && tabletBinding.getGyroscopeMode()) {
                    ConnectionUtil.go("Straight", null, !steeringWheelMode);
                    ConnectionUtil.go("Stop", null, !steeringWheelMode);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that it is the SecondActivity with an OK result
        if (requestCode == 1) {
            if (resultCode == 200) {
                if (ConnectionUtil.isMQTTConnection()) {
                    MqttClientUtil.releaseSemaphore();
                }
            }
        }
    }

    public void onImageClick(View view) {
        ConnectionUtil.takePicture(this);
    }

    public void onVideoClick(View view) {
        Intent intent = new Intent(this.getApplicationContext(), PictureActivity.class);
        intent.putExtra("origin", "video");
        ConnectionUtil.sendMessage("/cameraService/startVideoStream", "", this);
        startActivity(intent);
    }

    @SuppressLint("NonConstantResourceId")
    public void onDirectionClick(View v) {
        if (tabletBinding.getIsArmed() && tabletBinding.getIsAutopilotConnected() && tabletBinding.getIsAir()) {
            if (v.getId() == R.id.buttonNorth) {
                ConnectionUtil.go("North", this, false);
            } else if (v.getId() == R.id.buttonNorthEast) {
                ConnectionUtil.go("NorthEst", this, false);
            } else if (v.getId() == R.id.buttonNorthWest) {
                ConnectionUtil.go("NorthWest", this, false);
            } else if (v.getId() == R.id.buttonEast) {
                ConnectionUtil.go("East", this, false);
            } else if (v.getId() == R.id.buttonWest) {
                ConnectionUtil.go("West", this, false);
            } else if (v.getId() == R.id.buttonSouth) {
                ConnectionUtil.go("South", this, false);
            } else if (v.getId() == R.id.buttonSouthEast) {
                ConnectionUtil.go("SouthEst", this, false);
            } else if (v.getId() == R.id.buttonSouthWest) {
                ConnectionUtil.go("SouthWest", this, false);
            } else if (v.getId() == R.id.buttonStop) {
                ConnectionUtil.go("Stop", this, false);
            }
        } else {
            Toast.makeText(this, "Drone is disarmed, in land or not connected. No command will be sent.", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleSuccessPublish(String topic, Boolean result) {
        if (topic.contains("connect") || topic.contains("disconnect")) {
            if (tabletBinding.getIsAutopilotConnected()) {
                if (ConnectionUtil.isMQTTConnection()) {
                    //If it was connected, unsubscribe from telemetry_info
                    MqttClientUtil.unsubscribeTopic("autopilotService/" + getApplicationContext().getPackageName() + "/telemetry_info");
                } else {
                    tabletBinding.setIsAutopilotConnected(false);
                }
                //And unregister the receiver!
                try {
                    getApplicationContext().unregisterReceiver(brTelemetry);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                if (ConnectionUtil.isMQTTConnection()) {
                    //If it was not connected, subscribe to telemetry_info
                    MqttClientUtil.subscribeTopic("autopilotService/" + getApplicationContext().getPackageName() + "/telemetry_info");
                }
                //And register the broadcast receiver!
                IntentFilter filter = new IntentFilter("com.upc.dronedroid.TELEMETRY_INFO");
                ContextCompat.registerReceiver(this.getApplicationContext(), brTelemetry, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }

        } else if (topic.contains("broker")) {
            tabletBinding.setIsBrokerConnected(result);
        }
        tabletBinding.invalidateAll();
    }

    public void onConnectClick(View v) {
        if (tabletBinding.getIsAutopilotConnected()) {
            ConnectionUtil.disconnect(this);
        } else {
            ConnectionUtil.connect(this);
        }
    }

    public void onArmClick(View v) {
        if (tabletBinding.getIsArmed()) {
            ConnectionUtil.disarm(this);
        } else {
            ConnectionUtil.arm(this);
        }

    }

    public void onTakeOffLandClick(View v) {
        if (tabletBinding.getIsAir()) {
            ConnectionUtil.returnToLaunch(this);
        } else {
            ConnectionUtil.takeOff(this);
        }
    }

    private void updateTelemetryInfo(String info) {
        JsonObject json = (JsonObject) JsonParser.parseString(info);
        MapView mapView = findViewById(R.id.OSMView);
        if (json.get("lat") != null && json.get("lon") != null) {
            if (!MapUtil.moveMarker(mapView, "drone", json.get("lat").getAsDouble(), json.get("lon").getAsDouble())) {
                //If the drone hasn't moved it means that it hasn't been yet created - create the marker, add it to the map and THEN move it!
                Marker drone = new Marker(mapView);
                drone.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_drone));
                drone.setId("drone");
                drone.setOnMarkerClickListener(null);
                drone.setOnMarkerDragListener(null);
                drone.setInfoWindow(null);
                mapView.getOverlayManager().add(drone);
                MapUtil.moveMarker(mapView, "drone", json.get("lat").getAsDouble(), json.get("lon").getAsDouble());
            }
        }
        tabletBinding.setHeading(json.get("heading").getAsDouble());
        tabletBinding.setGroundSpeed(json.get("groundSpeed").getAsDouble());
        tabletBinding.setAltitude(json.get("altitude").getAsDouble());
        tabletBinding.setBattery(json.get("battery").getAsDouble());
        String state = json.get("state").getAsString();

        tabletBinding.setState(state);
        tabletBinding.setIsAutopilotConnected(true);
        //Status handling in function of telemetry info packages
        switch (state) {
            case "connected":
                //Set all states to false
                tabletBinding.setIsTakingOff(false);
                tabletBinding.setIsAir(false);
                tabletBinding.setIsArmed(false);
                tabletBinding.setIsArming(false);
                tabletBinding.setIsFlying(false);
                tabletBinding.setIsLanding(false);
                tabletBinding.setIsAutopilotConnected(true);
                //tabletBinding.setIsBrokerConnected(false);
                break;
            case "armed":
                tabletBinding.setIsArmed(true);
                tabletBinding.setIsArming(false);
                break;
            case "disarmed":
                tabletBinding.setIsArmed(false);
                break;
            case "flying":
                tabletBinding.setIsArmed(true);
                tabletBinding.setIsAir(true);
                tabletBinding.setIsTakingOff(false);
                tabletBinding.setIsFlying(json.get("groundSpeed").getAsDouble() > 0.1f);
                break;
            case "onHearth":
                tabletBinding.setIsArmed(true);
                tabletBinding.setIsAir(false);
                tabletBinding.setIsLanding(false);
                //It shouldn't be this way, but onHearth state means disarmed
                tabletBinding.setIsArmed(false);
                break;
            //Logic for transitory statuses (they must disable temporarily their respective buttons)
            case "takingOff":
                tabletBinding.setIsTakingOff(true);
                break;
            case "arming":
                tabletBinding.setIsArming(true);
                break;
            case "landing":
                tabletBinding.setIsLanding(true);
                break;
            case "returningHome":
                tabletBinding.setIsLanding(true);
                tabletBinding.setIsFlying(false);
                break;
        }
    }


    public void onLayerButtonClick(View v) {
        LinearLayout layout = findViewById(R.id.linearLayout3);
        if (!spinnerOpened) {
            //Show spinner inside the vertical layout
            //Create the spinner
            ArrayList<String> spinnerArray = Lists.newArrayList(Arrays.asList(tilesMap.keySet().toArray(new String[0])));
            Spinner spinner = new Spinner(this);
            spinner.setBackground(null);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setSelection(tileSelected);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    //Semaphore to allow opening the spinner
                    if (spinnerOpened) {
                        if (!tilesMap.keySet().toArray()[position].toString().contains("Bing")) {
                            MapUtil.setTiles(findViewById(R.id.OSMView), tilesMap.get(tilesMap.keySet().toArray()[position]));
                        } else {
                            MapView mapView = findViewById(R.id.OSMView);
                            org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource.retrieveBingKey(getApplicationContext());

                            org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource bing = new org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource(null);
                            //Bing maps are handled in a different way
                            if (tilesMap.keySet().toArray()[position].toString().contains("Aerial")) {
                                bing.setStyle(BingMapTileSource.IMAGERYSET_AERIAL);
                                mapView.setTileSource(bing);
                            } else if (tilesMap.keySet().toArray()[position].toString().contains("Hybrid")) {
                                bing.setStyle(BingMapTileSource.IMAGERYSET_AERIALWITHLABELS);
                                mapView.setTileSource(bing);
                            } else if (tilesMap.keySet().toArray()[position].toString().contains("Road")) {
                                bing.setStyle(BingMapTileSource.IMAGERYSET_ROAD);
                                mapView.setTileSource(bing);
                            }
                        }
                        tileSelected = position;
                        layout.removeView(spinner);
                        spinnerOpened = false;
                    } else {
                        spinnerOpened = true;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Timber.tag("EXIT WITHOUT SELECTION").d("onNothingSelected: EXIT WITHOUT SELECTION");
                }
            });
            spinner.setFocusableInTouchMode(true);
            spinner.performClick();
            layout.addView(spinner);
        } else {
            layout.removeViewAt(5);
            spinnerOpened = false;
            onLayerButtonClick(v);
        }
    }

    public void onControlModeClick(View v) {
        //Send STOP to the Drone
        ConnectionUtil.go("Stop", this, tabletBinding.getGyroscopeMode() && steeringWheelMode);
        if (tabletBinding.getGyroscopeMode()) {
            //If Gyroscope was enabled, remove the Gyroscope listener BEFORE changing state
            sensorManager.unregisterListener(gyroListener, sensor);
        } else {
            //Set the callback for the Sensor Event
            sensorManager.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            //sensorManager.registerListener(gyroListener, sensor, 1000000, 1000000);
        }
        tabletBinding.setGyroscopeMode(!tabletBinding.getGyroscopeMode());
    }

    public void onOpenMaps(View v) {
        //Open new activity
        Intent intent = new Intent(TabletActivity.this, FlightPlansTabletActivity.class);
        intent.putExtra("canRunPlan", !tabletBinding.getIsAir() && !tabletBinding.getIsFlying() && tabletBinding.getIsArmed() && tabletBinding.getIsBrokerConnected());
        startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!ConnectionUtil.isMQTTConnection() && tabletBinding.getIsAutopilotConnected()) {
            //Only disconnect if, at least, autopilot is connected
            ConnectionUtil.disconnect(this);
        }
    }

}

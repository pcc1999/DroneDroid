package com.upc.dronedroid.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.MAVLink.common.msg_command_long;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_FRAME;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.upc.dronedroid.models.JSONFlightPlan;
import com.upc.dronedroid.ui.TabletActivity;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import timber.log.Timber;

public class RadioUtil implements DroneListener, TowerListener, LinkListener {

    private static final String STATE_CONNECTED = "connected";
    private static final String STATE_ARMED = "armed";
    private static final String STATE_ARMING = "arming";
    private static final String STATE_DISARMED = "disarmed";
    private static final String STATE_TAKING_OFF = "takingOff";
    private static final String STATE_FLYING = "flying";
    private static final String STATE_RTL = "returningHome";
    private static final String STATE_LANDING = "landing";
    private static final String STATE_ON_EARTH = "onHearth";
    private static final String DIRECTION_NORTH = "North";
    private static final String DIRECTION_SOUTH = "South";
    private static final String DIRECTION_EAST = "East";
    private static final String DIRECTION_WEST = "West";
    private static final String DIRECTION_SOUTHEAST = "SouthEst";
    private static final String DIRECTION_SOUTHWEST = "SouthWest";
    private static final String DIRECTION_NORTHEAST = "NorthEst";
    private static final String DIRECTION_NORTHWEST = "NorthWest";
    private static final String DIRECTION_STOP = "Stop";
    private static final String DIRECTION_CLOCKWISE = "Clockwise";
    private static final String DIRECTION_COUNTERCLOCKWISE = "CounterClockwise";
    private static final String DIRECTION_FRONT = "Front";
    private static final String DIRECTION_BACK = "Back";
    private static final String DIRECTION_STRAIGHT = "Straight";
    public static final String[] DIRECTIONS_YAW = {DIRECTION_CLOCKWISE, DIRECTION_COUNTERCLOCKWISE, DIRECTION_STRAIGHT};
    public static final String[] DIRECTIONS_RELATIVE = {DIRECTION_BACK, DIRECTION_FRONT, DIRECTION_STOP};
    private static final double targetAltitude = 5;
    private static final float speed = 1.0F;
    private static final float bearingDegrees = 20;

    private static RadioUtil radioUtilInstance;
    private ControlTower controlTower;
    private Drone drone;
    private static int droneType = Type.TYPE_COPTER;
    private final Handler handler = new Handler();
    private TabletActivity tabletActivity;
    private String prevState;
    private Thread goingThread;
    private Thread flightPlanThread;
    private Thread rotatingThread;
    private final Handler delayHandler = new Handler();

    private final Thread telemetryThread = new Thread(() -> {
        Timber.tag(getClass().getSimpleName()).d("Starting telemetryInfo thread.");
        while (!Thread.currentThread().isInterrupted()) {
            if (radioUtilInstance.drone.isConnected()) {
                Gps gps = radioUtilInstance.drone.getAttribute(AttributeType.GPS);
                LatLong latlong = gps.getPosition();
                double lat;
                double lon;
                if (latlong != null) {
                    lat = latlong.getLatitude();
                    lon = latlong.getLongitude();
                } else {
                    lat = 0.00;
                    lon = 0.00;
                }
                Battery batteryAtt = radioUtilInstance.drone.getAttribute(AttributeType.BATTERY);
                double battery = batteryAtt.getBatteryRemain();
                State stateAtt = radioUtilInstance.drone.getAttribute(AttributeType.STATE);
                Altitude altitudeAtt = radioUtilInstance.drone.getAttribute(AttributeType.ALTITUDE);
                double altitude = altitudeAtt.getAltitude();
                String state = getCustomState(stateAtt, altitudeAtt, latlong);
                Speed speed = radioUtilInstance.drone.getAttribute(AttributeType.SPEED);
                double groundSpeed = speed.getGroundSpeed();
                Attitude attitudeAtt = radioUtilInstance.drone.getAttribute(AttributeType.ATTITUDE);
                double heading = attitudeAtt.getYaw() >= 0
                        ? attitudeAtt.getYaw()
                        : 360 + attitudeAtt.getYaw();
                HashMap<String, String> map = new HashMap<>();
                map.put("lat", String.valueOf(lat));
                map.put("lon", String.valueOf(lon));
                map.put("battery", String.valueOf(battery));
                map.put("state", state);
                map.put("groundSpeed", String.valueOf(groundSpeed));
                map.put("altitude", String.valueOf(altitude));
                map.put("heading", String.valueOf(heading));
                JSONObject jsonObject = new JSONObject(map);
                Intent intent = new Intent();
                intent.setAction("com.upc.dronedroid.TELEMETRY_INFO");
                intent.putExtra("payload", jsonObject.toString());
                radioUtilInstance.tabletActivity.sendBroadcast(intent);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    });

    public static RadioUtil getInstance(Activity activity) {
        if (radioUtilInstance == null) {
            radioUtilInstance = new RadioUtil();
            radioUtilInstance.controlTower = new ControlTower(activity);
            radioUtilInstance.drone = new Drone(activity);
            radioUtilInstance.tabletActivity = (TabletActivity) activity;
        }
        return radioUtilInstance;
    }

    public static void create(Activity activity) {
        if (radioUtilInstance == null) {
            radioUtilInstance = getInstance(activity);
        }
        if (!radioUtilInstance.controlTower.isTowerConnected()) {
            radioUtilInstance.controlTower.connect(radioUtilInstance);
        }
    }

    @Override
    public void onTowerConnected() {
        radioUtilInstance.controlTower.registerDrone(radioUtilInstance.drone, radioUtilInstance.handler);
        radioUtilInstance.drone.registerDroneListener(radioUtilInstance);
    }

    @Override
    public void onTowerDisconnected() {

    }


    @Override
    public void onDroneEvent(String event, Bundle bundle) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                Timber.tag("Connection").d("onDroneEvent: Connected!");
                radioUtilInstance.prevState = STATE_CONNECTED;
                radioUtilInstance.tabletActivity.handleSuccessPublish("connect", true);
                radioUtilInstance.telemetryThread.start();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                Timber.tag("Connection").d("onDroneEvent: Disconnected!");
                disconnect();
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = radioUtilInstance.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != droneType) {
                    droneType = newDroneType.getDroneType();
                }
                break;

            default:
                //Timber.tag("droneEvent").d("onDroneEvent: %s", event);
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String s) {
        radioUtilInstance.drone.disconnect();
    }

    public static void connect(Activity activity) {
        if (radioUtilInstance == null || !radioUtilInstance.controlTower.isTowerConnected()) {
            create(activity);
        }
        if (!radioUtilInstance.drone.isConnected()) {
            //TESTING
            //ConnectionParameter connectionParams = ConnectionParameter.newTcpConnection("192.168.1.69", 5762, null, 1);
            //ConnectionParameter connectionParams = ConnectionParameter.newTcpConnection("192.168.1.54", 5762, null, 1);
            //ConnectionParameter connectionParams = ConnectionParameter.newTcpConnection("192.168.1.42", 5762, null, 1);
            //PROD
            ConnectionParameter connectionParams = ConnectionParameter.newUsbConnection(57600, null);
            try {
                radioUtilInstance.controlTower.registerDrone(radioUtilInstance.drone, radioUtilInstance.handler);
                radioUtilInstance.drone.registerDroneListener(radioUtilInstance);
                radioUtilInstance.drone.connect(connectionParams, new LinkListener() {
                    @Override
                    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
                        Toast.makeText(activity, "connectionStatusChanged: " + connectionStatus, Toast.LENGTH_LONG).show();
                        Timber.tag(getClass().getCanonicalName()).d(connectionStatus.getStatusCode(), "New connection status: %s");
                        if (Objects.equals(connectionStatus.getStatusCode(), LinkConnectionStatus.CONNECTED)) {
                        }
                    }
                });
            } catch (Exception e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void disconnect() {
        if (radioUtilInstance.drone.isStarted()) {
            radioUtilInstance.drone.disconnect();
        }
        radioUtilInstance.controlTower.unregisterDrone(radioUtilInstance.drone);
        radioUtilInstance.controlTower.disconnect();
        //Telemetry thread will be always != null and will always be started if drone has been connected anytime
        radioUtilInstance.telemetryThread.interrupt();
        if (radioUtilInstance.goingThread != null) {
            radioUtilInstance.goingThread.interrupt();
        }
        if (radioUtilInstance.flightPlanThread != null) {
            radioUtilInstance.flightPlanThread.interrupt();
        }
        radioUtilInstance.tabletActivity.handleSuccessPublish("disconnect", true);
        radioUtilInstance = null;
    }

    public static void arm() {
        ControlApi.getApi(radioUtilInstance.drone).enableManualControl(true, new ControlApi.ManualControlStateListener() {
            @Override
            public void onManualControlToggled(boolean isEnabled) {
                Timber.tag("ManualControl").d("onManualControlToggled: Manual Control is %s", isEnabled);
            }
        });
        radioUtilInstance.prevState = STATE_ARMING;
        VehicleApi.getApi(radioUtilInstance.drone).arm(true, true, new SimpleCommandListener() {
            @Override
            public void onError(int executionError) {
                //Error when arming
                Timber.tag("error").d("error on arming");
                radioUtilInstance.prevState = STATE_CONNECTED;
            }

            @Override
            public void onTimeout() {
                //Timeout on arming
                Timber.tag("timeout").d("timeout on arming");
                radioUtilInstance.prevState = STATE_CONNECTED;
            }

            @Override
            public void onSuccess() {
                //Success arming
                radioUtilInstance.prevState = STATE_ARMED;
            }
        });
    }

    public static void disarm() {
        VehicleApi.getApi(radioUtilInstance.drone).arm(false, false, new SimpleCommandListener() {
            @Override
            public void onError(int executionError) {
                //Error when arming
                radioUtilInstance.prevState = STATE_ARMED;
                Timber.tag("error").d("error on disarming");
            }

            @Override
            public void onTimeout() {
                //Timeout on arming
                radioUtilInstance.prevState = STATE_ARMED;
                Timber.tag("timeout").d("timeout on disarming");
            }

            @Override
            public void onSuccess() {
                //Success arming
                Timber.tag("success").d("success disarming");
                radioUtilInstance.prevState = STATE_DISARMED;
            }
        });
    }

    public static void takeOff() {
        radioUtilInstance.prevState = STATE_TAKING_OFF;
        ControlApi.getApi(radioUtilInstance.drone).takeoff(5, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                //Taking off
                Timber.tag("success").d("success taking off");
                radioUtilInstance.prevState = STATE_TAKING_OFF;
            }

            @Override
            public void onError(int i) {
                //Error when taking off
                Timber.tag("error").d("error on taking off");
                radioUtilInstance.prevState = STATE_CONNECTED;
            }

            @Override
            public void onTimeout() {
                //Timeout taking off
                Timber.tag("timeout").d("timeout on taking off");
                radioUtilInstance.prevState = STATE_CONNECTED;
            }
        });
    }

    public static void returnToLaunch() {
        VehicleApi.getApi(radioUtilInstance.drone).setVehicleMode(VehicleMode.COPTER_RTL, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                radioUtilInstance.prevState = STATE_RTL;
                Timber.tag("success").d("success start RTL");
            }

            @Override
            public void onError(int executionError) {
                radioUtilInstance.prevState = STATE_FLYING;
                Timber.tag("error").d("error on return to launch");
            }

            @Override
            public void onTimeout() {
                Timber.tag("timeout").d("timeout on return to launch");
            }
        });
    }

    private void runOnMainThread(Runnable runnable) {
        handler.post(runnable);
    }

    private void toggleVideoRecording() {
        SoloCameraApi.getApi(drone).toggleVideoRecording(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                //alertUser("Video recording toggled.");
            }

            @Override
            public void onError(int executionError) {
                //alertUser("Error while trying to toggle video recording: " + executionError);
            }

            @Override
            public void onTimeout() {
                //alertUser("Timeout while trying to toggle video recording.");
            }
        });
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        if (LinkConnectionStatus.FAILED.equals(connectionStatus.getStatusCode())) {
            Bundle extras = connectionStatus.getExtras();
            String msg = null;
            if (extras != null) {
                msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
            }
            Timber.tag("failed").d("connectionfailed: %s", msg);
        }
    }

    private String getCustomState(State stateAtt, Altitude altitudeAtt, LatLong latlong) {
        String state = "";
        //Timber.tag("CustomStateBuilder").d("Previous custom state is: %s", radioUtilInstance.prevState);
        Home home = radioUtilInstance.drone.getAttribute(AttributeType.HOME);
        switch (radioUtilInstance.prevState) {
            case STATE_ARMING:
                if (stateAtt.isArmed()) {
                    state = STATE_ARMED;
                    radioUtilInstance.prevState = STATE_ARMED;
                } else {
                    state = radioUtilInstance.prevState;
                }
                break;
            case STATE_RTL:
                double latitudeDiff = latlong.getLatitude() - home.getCoordinate().getLatitude();
                if (latitudeDiff == 0.0) {
                    latitudeDiff = home.getCoordinate().getLatitude() - latlong.getLatitude();
                }
                double longitudeDiff = latlong.getLongitude() - home.getCoordinate().getLongitude();
                if (longitudeDiff == 0.0) {
                    longitudeDiff = home.getCoordinate().getLongitude() - latlong.getLongitude();
                }
                if (latitudeDiff < 10E-1 && latitudeDiff > -10E-1 && longitudeDiff < 10E-1 && longitudeDiff > -10E-1) {
                    double altitudeDiff = altitudeAtt.getAltitude() - home.getCoordinate().getAltitude();
                    if (altitudeDiff == 0.0) {
                        altitudeDiff = home.getCoordinate().getAltitude() - altitudeAtt.getAltitude();
                    }
                    if (altitudeDiff < 10E-1 && altitudeDiff > -10E-1) {
                        state = STATE_ON_EARTH;
                        radioUtilInstance.prevState = STATE_ON_EARTH;
                    } else {
                        state = STATE_LANDING;
                        radioUtilInstance.prevState = STATE_LANDING;
                    }
                } else {
                    state = radioUtilInstance.prevState;
                }
                break;
            case STATE_LANDING:
                //If drone was landing we have to check home altitude
                double altitudeDiff = altitudeAtt.getAltitude() - home.getCoordinate().getAltitude();
                if (!(altitudeDiff < 10E-1 && altitudeDiff > -10E-1)) {
                    state = radioUtilInstance.prevState;
                } else {
                    state = STATE_ON_EARTH;
                    radioUtilInstance.prevState = STATE_ON_EARTH;
                }
                break;
            case STATE_TAKING_OFF:
                if (home.getCoordinate() != null) {
                    altitudeDiff = targetAltitude + home.getCoordinate().getAltitude() - altitudeAtt.getAltitude();
                } else {
                    altitudeDiff = targetAltitude - altitudeAtt.getAltitude();
                }
                if (!(altitudeDiff < 10E-1 && altitudeDiff > -10E-1)) {
                    state = radioUtilInstance.prevState;
                } else {
                    state = STATE_FLYING;
                    radioUtilInstance.prevState = STATE_FLYING;
                }
                break;
            default:
                if (stateAtt.isFlying() && altitudeAtt != null && home.getCoordinate() != null) {
                    altitudeDiff = altitudeAtt.getAltitude() - home.getCoordinate().getAltitude();
                    if (!(altitudeDiff < 10E-1 && altitudeDiff > -10E-1)) {
                        state = STATE_FLYING;
                        radioUtilInstance.prevState = STATE_FLYING;
                    } else {
                        state = STATE_ARMED;
                        radioUtilInstance.prevState = STATE_ARMED;
                    }
                } else if (stateAtt.isArmed()) {
                    state = STATE_ARMED;
                    radioUtilInstance.prevState = STATE_ARMED;
                } else {
                    state = STATE_CONNECTED;
                    radioUtilInstance.prevState = STATE_CONNECTED;
                }
                break;
        }
        //Timber.tag("CustomStateBuilder").d("Current custom state is: %s", state);
        return state;
    }

    public static void goDirection(String direction) {
        msg_set_position_target_local_ned msgAbs = new msg_set_position_target_local_ned();
        msgAbs.time_boot_ms = 0;
        msgAbs.afx = 0;
        msgAbs.afy = 0;
        msgAbs.afz = 0;
        msgAbs.type_mask = 0b0000111111000111;
        msgAbs.x = 0;
        msgAbs.y = 0;
        msgAbs.z = 0;
        msgAbs.coordinate_frame = (short) (Arrays.asList(DIRECTIONS_RELATIVE).contains(direction)
                ? MAV_FRAME.MAV_FRAME_BODY_OFFSET_NED
                : MAV_FRAME.MAV_FRAME_LOCAL_NED);
        msgAbs.yaw = 0;
        msgAbs.yaw_rate = 0;
        switch (direction) {
            case DIRECTION_NORTH:
            case DIRECTION_FRONT:
                msgAbs.vx = speed;
                msgAbs.vy = 0;
                msgAbs.vz = 0;
                break;
            case DIRECTION_SOUTH:
            case DIRECTION_BACK:
                msgAbs.vx = -speed;
                msgAbs.vy = 0;
                msgAbs.vz = 0;
                break;
            case DIRECTION_EAST:
                msgAbs.vx = 0;
                msgAbs.vy = speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_WEST:
                msgAbs.vx = 0;
                msgAbs.vy = -speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_NORTHEAST:
                msgAbs.vx = speed;
                msgAbs.vy = speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_NORTHWEST:
                msgAbs.vx = speed;
                msgAbs.vy = -speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_SOUTHEAST:
                msgAbs.vx = -speed;
                msgAbs.vy = speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_SOUTHWEST:
                msgAbs.vx = -speed;
                msgAbs.vy = -speed;
                msgAbs.vz = 0;
                break;
            case DIRECTION_STOP:
                msgAbs.vx = 0;
                msgAbs.vy = 0;
                msgAbs.vz = 0;
                break;
            default:
                return;
        }
        ExperimentalApi.getApi(radioUtilInstance.drone).sendMavlinkMessage(new MavlinkMessageWrapper(msgAbs));
    }

    public static void goRelative(String direction) {
        if (Arrays.asList(DIRECTIONS_YAW).contains(direction)) {
            getThreadForYawRotation(direction);
        } else if (Arrays.asList(DIRECTIONS_RELATIVE).contains(direction)) {
            getThreadForRelDirection(direction);
        }
    }

    public static void getThreadForDirection(String direction) {
        if (radioUtilInstance.goingThread != null) {
            radioUtilInstance.goingThread.interrupt();
            radioUtilInstance.delayHandler.removeCallbacksAndMessages(null);
        }
        if (!direction.equals(DIRECTION_STOP)) {
            //Only start thread if direction is NOT stop. If it is stop, simply interrupt the goingThread
            Log.d("GyroscopeRadioUtil", "Starting thread for abs direction " + direction);
            radioUtilInstance.goingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    goDirection(direction);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });
            radioUtilInstance.goingThread.start();
        }
    }

    public static void getThreadForRelDirection(String direction) {
        if (radioUtilInstance.goingThread != null) {
            radioUtilInstance.goingThread.interrupt();
            radioUtilInstance.delayHandler.removeCallbacksAndMessages(null);
        }
        if (!direction.equals(DIRECTION_STOP)) {
            goDirection(direction);
            if (direction.equals(DIRECTION_BACK)) {
                direction = DIRECTION_FRONT;
            }
            String finalDirection = direction;
            radioUtilInstance.delayHandler.postDelayed(() -> {
                Log.d("SteeringWheelMode", "Starting thread for relative direction " + finalDirection);
                radioUtilInstance.goingThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        goDirection(finalDirection);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                });
                radioUtilInstance.goingThread.start();
            }, 4000);
        }
    }

    public static void getThreadForYawRotation(String direction) {
        if (radioUtilInstance.rotatingThread != null) {
            radioUtilInstance.rotatingThread.interrupt();
        }
        Log.d("SteeringWheelMode", "Starting thread for yaw rotation " + direction);
        radioUtilInstance.rotatingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                msg_command_long msgYaw = new msg_command_long();
                msgYaw.command = MAV_CMD.MAV_CMD_CONDITION_YAW;
                msgYaw.target_component = 0;
                msgYaw.target_system = 0;
                msgYaw.param1 = direction.equals(DIRECTION_STRAIGHT)
                        ? 0
                        : bearingDegrees;
                msgYaw.param3 = direction.equals(DIRECTION_CLOCKWISE)
                        ? 1
                        : -1;
                msgYaw.param4 = 1;
                msgYaw.param2 = msgYaw.param5 = msgYaw.param6 = msgYaw.param7 = 0;
                ExperimentalApi.getApi(radioUtilInstance.drone).sendMavlinkMessage(new MavlinkMessageWrapper(msgYaw));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        radioUtilInstance.rotatingThread.start();
    }

    public static void getThreadForFlightPlan(JSONFlightPlan jsonFP) {
        if (radioUtilInstance.flightPlanThread != null) {
            radioUtilInstance.flightPlanThread.interrupt();
        }
        radioUtilInstance.flightPlanThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                executeFlightPlan(jsonFP);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        radioUtilInstance.flightPlanThread.start();
    }

    public static void executeFlightPlan(JSONFlightPlan jsonFP) {
        // TODO: new thread must be created:
        // - Must check periodically if has arrived to the destination
        // - If so, go to next destination
        // - When last position is reached, it must call RTL

    }

}

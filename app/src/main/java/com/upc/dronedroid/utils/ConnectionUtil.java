package com.upc.dronedroid.utils;

import android.app.Activity;

import com.google.gson.Gson;
import com.upc.dronedroid.models.JSONFlightPlan;

import java.util.Arrays;

public class ConnectionUtil {

    public String connectionSelected;

    public static final String[] availableBrokers = {
            "broker.hivemq.com",
            "10.10.10.1",
            "telemetry radio"
    };
    static ConnectionUtil connectionUtilInstance;

    private ConnectionUtil(String connectionSelected) {
        this.connectionSelected = connectionSelected;
    }

    public static void getInstance(String connectionSelected, Activity activity) {
        if (connectionUtilInstance == null) {
            connectionUtilInstance = new ConnectionUtil(connectionSelected);
        }
        initialize(connectionSelected, activity);
    }

    public static void sendMessage(String topic, String payload, Activity activity) {
        String topicPrefix;
        if (activity != null) {
            topicPrefix = activity.getApplicationContext().getPackageName();
        } else {
            Package pack = connectionUtilInstance.getClass().getPackage();
            assert pack != null;
            topicPrefix = pack.getName();
        }
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(topicPrefix + topic, payload, activity);
        }
    }

    public static boolean isMQTTConnection() {
        return Arrays.asList(MqttClientUtil.MQTTBrokers).contains(connectionUtilInstance.connectionSelected);
    }

    public static void initialize(String brokerSelected, Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.getMQTTUtilInstance(activity, brokerSelected);
        } else {
            RadioUtil.create(activity);
        }
    }

    public static void connect(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/connect", "", activity);
        } else {
            RadioUtil.connect(activity);
        }
    }

    public static void disconnect(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/disconnect", "", null);
        } else {
            RadioUtil.disconnect();
        }
    }

    public static void arm(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/armDrone", "", null);
        } else {
            RadioUtil.arm();
        }
    }

    public static void disarm(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/disarmDrone", "", null);
        } else {
            RadioUtil.disarm();
        }
    }

    public static void returnToLaunch(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/returnToLaunch", "", activity);
        } else {
            RadioUtil.returnToLaunch();
        }
    }

    public static void takeOff(Activity activity) {
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/takeOff", "", activity);
        } else {
            RadioUtil.takeOff();
        }
    }

    public static void go(String direction, Activity activity, boolean isRelative) {
        String topicPrefix;
        if (activity != null) {
            topicPrefix = activity.getApplicationContext().getPackageName();
        } else {
            Package pack = connectionUtilInstance.getClass().getPackage();
            assert pack != null;
            topicPrefix = pack.getName();
        }
        if (isMQTTConnection()) {
            if (Arrays.asList(RadioUtil.DIRECTIONS_YAW).contains(direction)) {
                MqttClientUtil.publishMessage(topicPrefix + "/autopilotService/bear", direction, activity);
            } else {
                MqttClientUtil.publishMessage(topicPrefix + "/autopilotService/go", direction, activity);
            }
        } else {
            if (isRelative) {
                RadioUtil.goRelative(direction);
            } else {
                RadioUtil.getThreadForDirection(direction);
            }
        }
    }

    public static void takePicture(Activity activity) {
        String topicPrefix;
        if (activity != null) {
            topicPrefix = activity.getApplicationContext().getPackageName();
        } else {
            Package pack = connectionUtilInstance.getClass().getPackage();
            assert pack != null;
            topicPrefix = pack.getName();
        }
        if (isMQTTConnection()) {
            MqttClientUtil.publishMessage(topicPrefix + "/cameraService/takePicture", "", activity);
        }
    }

    public static void executeFlightPlan(JSONFlightPlan jsonFP, Activity activity) {
        Gson gson = new Gson();
        if (isMQTTConnection()) {
            String waypointsJSON = gson.toJson(jsonFP.getWaypoints());
            MqttClientUtil.publishMessage(activity.getApplicationContext().getPackageName() + "/autopilotService/executeFlightPlan", waypointsJSON, activity);
        } else {
            RadioUtil.getThreadForFlightPlan(jsonFP);
        }
    }

}




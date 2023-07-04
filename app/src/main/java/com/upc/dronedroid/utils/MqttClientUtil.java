package com.upc.dronedroid.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.upc.dronedroid.ui.PictureActivity;
import com.upc.dronedroid.ui.SplashActivity;
import com.upc.dronedroid.ui.TabletActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jetbrains.annotations.NotNull;

public class MqttClientUtil {

    private static final String globalBrokerAddress = "broker.hivemq.com";
    private static final String localBrokerAddress = "10.10.10.1";
    private static String selectedBroker;
    public static final String[] MQTTBrokers = {
            globalBrokerAddress,
            localBrokerAddress
    };
    private static final int brokerPort = 8000;
    private static MqttClientUtil mqttUtilInstance;
    private static boolean pictureIsShowing;
    public MqttAndroidClient client;

    private MqttClientUtil() {

    }

    public static void getMQTTUtilInstance(Activity activity, String selectedBroker) {
        if (mqttUtilInstance == null) {
            mqttUtilInstance = new MqttClientUtil();
            mqttUtilInstance.client = new MqttAndroidClient(activity.getApplicationContext(), "ws://" + selectedBroker + ":" + brokerPort, "", MqttAndroidClient.Ack.AUTO_ACK);
            mqttUtilInstance.client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(this.getClass().getName(), "Connection successful");
                    try {
                        TabletActivity tabletActivity = (TabletActivity) activity;
                        tabletActivity.handleSuccessPublish("broker", true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(this.getClass().getName(), "Connection lost", cause);
                    try {
                        TabletActivity tabletActivity = (TabletActivity) activity;
                        tabletActivity.handleSuccessPublish("broker", true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //Inform the Activity in order to ask for a reconnection
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Log.d(this.getClass().getName(), "Message arrived!");
                    String[] parts = topic.split("/");
                    onMessage(parts[0], parts[2], message, activity);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(this.getClass().getName(), "Message delivered!");
                }
            });
            MqttConnectOptions options = new MqttConnectOptions();
            try {
                mqttUtilInstance.client.connect(options, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(this.getClass().getName(), "Connection successful");
                        //Subscribe to every topic whose origin is this app
                        subscribeTopic("+/" + activity.getApplicationContext().getPackageName() + "/#");
                        TabletActivity tabletActivity = (TabletActivity) activity;
                        tabletActivity.handleSuccessPublish("broker", true);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        exception.printStackTrace();
                        //Inform the Activity in order to ask for a reconnection
                        TabletActivity tabletActivity = (TabletActivity) activity;
                        tabletActivity.handleSuccessPublish("broker", false);
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public static void onMessage(@NotNull String origin, String command, MqttMessage message, Activity activity) {
        switch (origin) {
            case "cameraService": {
                Log.d("MQTTClientUtil", "Message is from camera Service - will be processed in another screen");
                switch (command) {
                    case "videoFrame": {
                        Intent intent = new Intent();
                        intent.setAction("com.upc.dronedroid.VIDEO_STREAM");
                        byte[] bytes = Base64.decode(message.getPayload(), Base64.DEFAULT);
                        intent.putExtra("bytes", bytes);
                        intent.putExtra("origin", "video");
                        activity.sendBroadcast(intent);
                        break;
                    }
                    case "picture": {
                        if (!pictureIsShowing) {
                            byte[] bytes = Base64.decode(message.getPayload(), Base64.DEFAULT);
                            Intent intent = new Intent(activity.getApplicationContext(), PictureActivity.class);
                            intent.putExtra("bytes", bytes);
                            intent.putExtra("origin", "picture");
                            activity.startActivityForResult(intent, 1);
                            pictureIsShowing = true;
                            break;
                        }
                    }
                }
            }
            case "autopilotService": {
                switch (command) {
                    case "telemetryInfo":
                        Intent intent = new Intent();
                        intent.setAction("com.upc.dronedroid.TELEMETRY_INFO");
                        intent.putExtra("payload", message.getPayload());
                        activity.sendBroadcast(intent);
                        break;
                    case "waypointReached":
                        break;
                }
            }
        }
    }

    public static void publishMessage(String topic, String payload, Activity activity) {
        try {
            if (!mqttUtilInstance.client.isConnected()) {
                mqttUtilInstance.client.connect();
                if (mqttUtilInstance.client.isConnected()) {
                    TabletActivity tabletActivity = (TabletActivity) activity;
                    tabletActivity.handleSuccessPublish("broker", true);
                } else {
                    TabletActivity tabletActivity = (TabletActivity) activity;
                    tabletActivity.handleSuccessPublish("broker", false);
                }
            }
            if (mqttUtilInstance.client.isConnected()) {
                MqttMessage message = new MqttMessage();
                message.setPayload(payload.getBytes());
                message.setQos(0);
                mqttUtilInstance.client.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(this.getClass().getName(), "Publish succeed!");
                        if (activity != null) {
                            if (DeviceUtil.isTablet(activity) && !(topic.contains("camera")) && activity.getClass().getName().equals("com.upc.dronedroid.ui.TabletActivity")) {
                                TabletActivity tabletActivity = (TabletActivity) activity;
                                tabletActivity.handleSuccessPublish(topic, true);
                            } else if (!DeviceUtil.isTablet(activity)) {
                                //TODO: Mobile handling of responses
                            }
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.i(this.getClass().getName(), "Publish failed!");
                        Toast.makeText(activity, "Publish failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void subscribeTopic(String topic) {
        try {
            mqttUtilInstance.client.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(this.getClass().getName(), "subscribe succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(this.getClass().getName(), "subscribe failed!");
                    //Inform Activity back!
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void unsubscribeTopic(String topic) {
        try {
            mqttUtilInstance.client.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(this.getClass().getName(), "unsubscribe succeed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(this.getClass().getName(), "unsubscribe failed!");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void releaseSemaphore() {
        pictureIsShowing = false;
    }

}

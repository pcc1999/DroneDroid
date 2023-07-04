# DroneDroid

DroneDroid is a native application developed in Java. It allows the user to do a bunch of things, including:
- Connecting to the drone in three ways: local configuration, global configuration and directly (this is, directly to the autopilot of the drone) through the telemetry radio
- Showing the picture/video stream sent by the camera service
- Free guiding the drone
- Guiding the drone using the gyroscope in two different modes:
  - Simple mode: a mapping of tilt directions and absolute directions
  - Steering wheel mode: the Android device is like a steering wheel so the drone can go forward, backward and rotate clockwise/counterclockwise.
- Showing telemetry data

## Demo

### Splash Activity

Once the application is launched a first dialog will pop up asking for user (and password if needed) and the connection string. The first connection string corresponds to global configuration, the second one corresponds to the local configuration and, the last one, to the direct connection.

After selecting both the user (the Demo User won't ask for a password) and the connection string and if location permissions have been granted (just in the first execution), the main Activity will be launched.

### Tablet Activity

The only available button in middle-left section allows to connect to the drone platform. If the connection is indirect (global/local configuration) the photo/video buttons will also be enabled if the application has connected to the broker.

Once the connection is successful the button below will be activated. This button allows to arm the drone (or disarm it in case it is already armed). Additionally, the telemetry info block and the drone marker will be shown. Also, the autopilot connection indicator will show as green.

Once the drone is armed the take off/land button will be enabled and will show the Take Off text. The arm indicator will also show as green as the drone is currently armed. When the take off button is clicked and the target altitude is reached, the directional pad will be enabled and the user will be able to guide the drone in the desired direction. Additionally, the flying indicator will show as green as well as in the previous cases.

In order to switch to the gyroscope mode the first floating button in top-right section must be clicked (only available when the drone is flying). This will allow to control the drone with the tilt of the device just as each tilt direction was a button. The steering wheel mode can be enabled by switching the corresponding switch in the bottom-center section, below the telemetry info block. In this mode the drone can be controlled by tilting the device forward or backward in order to go, respectively, front or back without bearing the drone. The bearing can be controlled by turning the device in clockwise or counterclockwise direction.

If the user clicks on the second floating button the Flight Plans Activity will be shown. This Activity allows to create, edit, remove, share and run flight plans (only over MQTT connections and if the drone is armed but not flying). 

The user will be able to swap between map sources when clicking on the third floating button and selecting any of the sources in the drop down menu.

### Flight Plans Tablet Activity

To create new flight plans simply click on the "plus" button and start setting the way points. After that the user must set a name for the flight plan and, after that, new flight plan can be saved by clicking the Save button.

To delete flight plans simply select them and click on the Trash button.

To edit flight plans select the flight plan and click on the "pencil" button and follow the same logic as when creating flight plans. The way points can be deleted and it can be decided if a picture must be taken in that position. To save the flight plan, simply click on the Save button.

To run a flight plan, select it and click on Run button.

### Picture Activity

When the video stream is started or a picture requested is received the Picture Activity shows up. There are 4 options regarding filters: no filters, warm filter, cold filter and green filter. 

### Generic

Clicking on any map marker will set the center of the map on that marker.    

## Installation and contribution
In order to run and contribute to the application, you need to install Android Studio. You will also need to download an appropriate Java SDK (we recommend JDK 15) and set up the corresponding environment variable JAVA_HOME pointing towards the directory of the downloaded JDK. A good tutorial can be found in [Android Studio Installation Tutorial](https://www.xatakandroid.com/tutoriales/como-instalar-el-android-sdk-y-para-que-nos-sirve)
To contribute you must follow the contribution protocol described in the main repo of the Drone Engineering Ecosystem.
[![DroneEngineeringEcosystem Badge](https://img.shields.io/badge/DEE-MainRepo-brightgreen.svg)](https://github.com/dronsEETAC/DroneEngineeringEcosystemDEE)
To run the application simply open it in Android Studio after cloning the repository and, after connecting the Android device, click on the Run "app" button in top tool bar. The application will be installed on the device and will open automatically after that.

BatteryFu
=========

BatteryFu (pronounced Battery-Foo, like in Kung-Fu) is an Android app that extends battery life (and lowers data usage) by changing always-on mobile/wifi data to periodic sync (meaning it disables your mobile data and/or Wifi, then checks your accounts every X minutes).

Google Play Store link:
https://play.google.com/store/apps/details?id=com.tobykurien.batteryfu

Integration
===========

You can control BatteryFu from another app by broadcasting Intents. The Intents should have their 
action set to ```batteryfu.intent.action.TOGGLE```, and the action URI is used to specify that to 
toggle, for example ```data://on``` to enable data, and ```data://off``` to disable it. Here is an example:

```java
// turn data on
Intent intent = new Intent();
intent.setAction("batteryfu.intent.action.TOGGLE");
intent.setData(Uri.parse("data://on"));
sendBroadcast(intent);
```
Other schemes you can toggle are:

- ```batteryfu``` - enable/disable BatteryFu
- ```nightmode``` - enable/disable nightmode
- ```travelmode``` - enable travel mode
- ```standardmode``` - enable standard mode
- ```offlinemode``` - enable "always offline" mode
- ```onlinemode``` - enable "always online" mode
- ```sync``` - perform a data sync, turn data on if necessary
- ```data``` - enable/disable data (mobile and/or wifi, depending on settings)

See the 
[Intent filter](https://github.com/tobykurien/BatteryFu/blob/master/AndroidManifest.xml#L69)
in the AndroidManifest.xml file for the full list of intent schemes.


Dependencies
============

BatteryFu depends on the https://github.com/koush/Widgets library, but an older version 
of it. It won't compile with newer versions, so a copy of the library source is 
included in the Widgets/ folder. You can use Gradle to build, or import Widgets as 
a project into Eclipse and add it as an Android library.

Credits
=======

- Italian translation by Emanuele Brown
- Android 5 support added by https://github.com/andyboeh

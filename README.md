BatteryFu
=========

BatteryFu (pronounced Battery-Foo, like in Kung-Fu) is an Android app that extends battery life (and lowers data usage) by changing always-on mobile/wifi data to periodic sync (meaning it disables your mobile data and/or Wifi, then checks your accounts every X minutes).

Google Play Store link:
https://play.google.com/store/apps/details?id=com.tobykurien.batteryfu

Dependencies
============

BatteryFu depends on the https://github.com/koush/Widgets library, but an older version 
of it. It won't compile with newer versions, so a copy of the library source is 
included in the Widgets/ folder. You can use Gradle to build, or import Widgets as 
a project into Eclipse and add it as an Android library.
 
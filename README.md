# Mazda Connector<sup>[[1]](#1)</sup>

[![Join the chat at https://gitter.im/jmgao/mazda-connector](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/jmgao/mazda-connector?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
Mazda Connector is a collection of binaries to augment the functionality of the MZD Connect infotainment system on Mazda's current line of cars.<sup>[[2]](#2)</sup>&nbsp; Currently, there are three components: an Android application (``android``), a server binary which runs on the infotainment system (``connector``), and an input capturing binary (``input_filter``) that does a bunch of gross hacks to capture input from the steering wheel/commander knob before it hits the system.
### Components
##### Android
An Android application which provides a bluetooth server socket to which the car can connect for bidirectional communication. Currently, this only takes the form of triggering the Google Now voice recognition activity, but this can be easily extended.

##### Connector
A daemon that handles connecting to the Android application through the system's native bluetooth stack, and listens to incoming events via D-BUS to tell the phone to do stuff.

##### Input Filter
A daemon that runs before any of the system's input reading services start, which filters out specific button presses (currently only the steering wheel talk button) and triggers events.

### Building
Building the connector and input filter components requires an ARM cross-compilation toolchain with a relatively new version of gcc (C++11 support is required), glibc 2.11, libdbus, boost headers. A toolchain built for Ubuntu 14.10 is available [here](https://github.com/jmgao/m3-toolchain). [ninja](http://martine.github.io/ninja/) is also required, because I'm too lazy to write a makefile. The android app is built with [sbt](http://www.scala-sbt.org/download.html).

### Installation
**WARNING**: DO NOT DO THIS UNLESS YOU KNOW WHAT YOU ARE DOING.<br/>
_**THIS CAN BRICK YOUR INFOTAINMENT SYSTEM IF YOU ARE UNLUCKY OR UNCAREFUL.**_<br />
Here is a checklist of things you should verify (and know how to verify) before even thinking of installing this:

1. ``/dev/input/event1`` exists and is the virtual keyboard device.
2. ``/dev/input/event5`` is the last event device in ``/dev/event``.
3. KEY_G is the voice recognition button on your steering wheel.

There are three steps you should take in the process, each of which is more dangerous than the previous.

##### Bootloop prevention
Several of the services on the infotainment system will cause a reboot of the system if they fail. If this happens during the boot sequence because one of the daemons added by us has broken some invariant expected by the services, it will probably enter an endless bootloop which you won't be able to fix with ssh. To prevent this, both `connector` and `input_filter` are controlled by the `enable_connector` and `enable_input_filter` files in /tmp/mnt/data. The daemons will check these files on startup for '1' to decide whether to do stuff, and set them to '0' before continuing. If they happen to break things and cause a reboot, they'll do nothing on next boot, preventing Bad Things from happening. When you have everything working after a successful boot, setting the files to contain '2' will prevent them from disabling themselves on future boots.

##### Install connector
Installation of ``connector`` and verifying that it works is relatively safe. Add an entry for the service into ``/jci/bds/BdsConfiguration.xml``:
```
<serialPort id="8017" name="Mazda Connector" critical="false" enabled="true" uuidServer="62306C7457064375BB48212331070361" uuidClient="62306C7457064375BB48212331070361" writeDelay="3"/>
```
Then, copy the binary to somewhere like ``/tmp/mnt/data``, and add it to one of the later stage start scripts specificed in ``/jci/sm/sm.conf``, such as ``/jci/scripts/stage_gap2.sh``. (Make sure to background the process!). Running it manually and checking its output to see if it successfully connects to the Android application should be sufficient to verify functionality. (Currently, connector is pretty much completely safe, so you can set ``enable_connector`` to '2' from the start. This may not be true in the future, so on upgrades, you should probably always be setting the values for both daemons to '1')

#### Verify that input_filter works
Copy input_filter onto the system (preferably ``/tmp/mnt/data``), run ``input_filter`` and press the voice recognition button, and you should see logs saying something to that effect. You'll also probably notice that the infotainment interface is broken, and will probably reboot shortly.

#### Add input_filter to the startup manifest
This is the scary part: if you mess up here, you've bricked your car. Edit the ``/jci/sm/sm.conf`` file to add ``input_filter`` to the startup sequence.
```
<service type="process" name="input_filter" path="/tmp/mnt/data/input_filter" autorun="yes" reset_board="no" retry_count="0">
    <dependency type="service" value="settings"/>
</service>
```
We want ``input_filter`` to run immediately before the first process which consumes input, which is ``devices``. Therefore, we need to change it to depend on the ``input_filter`` service. <sup>(FIXME: Is there a race here?)</sup> 
```
<service type="jci_service" name="devices" path="/jci/devices/svc-com-jci-cpp-devices.so" autorun="yes" retry_count="0" args="" reset_board="yes" affinity_mask="0x02">
    <dependency type="service" value="stage_1"/>
    <dependency type="service" value="settings"/>
>>> <dependency type="service" value="input_filter"/>
</service>
```
After modifying sm.conf and triple checking that it's correct and no mistaken changes have been made, reboot with '1' in ``/tmp/mnt/data/enable_input_filter`` (since running it in the previous step would have changed it to '0'), and the voice recognition button should no longer trigger the stock voice prompt. At this point, everything is probably working, so you should be able to enable both services permanently.

### License
AGPLv3

--
<a name="1"/>1. Placeholder name until I can think of something better; Mazda please don't sue me <br />
<a name="2"/>2. Currently only tested with the 2014 Mazda 3


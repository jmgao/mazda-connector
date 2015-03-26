package us.insolit.connector.gps

import android.app.{Notification, NotificationManager}
import android.content.{Context, Intent}
import android.hardware.GeomagneticField
import android.location.{Criteria, Location, LocationListener, LocationManager, LocationProvider}
import android.util.Log
import android.os.{Bundle, SystemClock}

import us.insolit.connector.R

class MockGPSProvider private (ctx: Context, locationManager: LocationManager, notificationManager: NotificationManager) {
  val providerName = LocationManager.GPS_PROVIDER

  private var started = false
  private val notificationID = 1

  def isRunning() = started

  def start() {
    this.synchronized {
      if (started) {
        Log.e("MockGPSProvider", "start called while already started")
        return
      }

      started = true

      locationManager.addTestProvider(
        providerName,           // name
        true,                   // requiresNetwork
        true,                   // requiresSatellite
        false,                  // requiresCell
        false,                  // hasMonetaryCost
        true,                   // supportsAltitude
        true,                   // supportsSpeed
        true,                   // supportsBearing
        Criteria.POWER_HIGH,    // powerRequirement
        Criteria.ACCURACY_FINE  // accuracy
      )

      locationManager.setTestProviderEnabled(providerName, true)

      val extras = new Bundle()
      extras.putInt("satellites", 1)
      locationManager.setTestProviderStatus(providerName, LocationProvider.AVAILABLE, extras, 1000)

      val notification =
        new Notification.Builder(ctx)
          .setContentTitle("Mazda Connector")
          .setContentText("Mazda Connector providing GPS location")
          .setSmallIcon(R.drawable.ic_stat_device_gps_fixed)
          .setOngoing(true)
          .build()

      notificationManager.notify(notificationID, notification)
    }
  }

  def stop() {
    this.synchronized {
      if (!started) {
        Log.e("MockGPSProvider", "stop called while already stopped")
        return
      }

      started = false
      locationManager.setTestProviderEnabled(providerName, false)
      locationManager.removeTestProvider(providerName)

      notificationManager.cancel(notificationID)
    }
  }

  def update(location: Location) {
    this.synchronized {
      if (!started) {
        Log.e("MockGPSProvider", "update called while stopped")
        return
      }

      val fixedLocation = new Location(location)
      fixedLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos())
      fixedLocation.setProvider(providerName)
      fixedLocation.setTime(System.currentTimeMillis())


      // TODO: Figure out if this is android, the car, or my bug
      val correctedBearing = (location.getBearing() + 180.0f) % 360.0f
      fixedLocation.setBearing(correctedBearing)

      // Uncorrect for magnetic declination
      val geomagneticField = new GeomagneticField(location.getLatitude().toFloat, location.getLongitude().toFloat, location.getAltitude().toFloat, System.currentTimeMillis())
      val magneticBearing: Float = (correctedBearing - geomagneticField.getDeclination()) % 360.0f
      val bearingIntent = new Intent("us.insolit.connector.BEARING").putExtra("bearing", magneticBearing)
      ctx.sendBroadcast(bearingIntent)

      Log.d("MockGPSProvider", "Setting mock location to %s".format(fixedLocation))
      locationManager.setTestProviderLocation(providerName, fixedLocation)
    }
  }
}

object MockGPSProvider {
  private var instance: Option[MockGPSProvider] = None

  def getInstance(ctx: Context) = {
    this.synchronized {
      instance match {
        case Some(x) => x
        case None => {
          val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]
          val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
          val newInstance = new MockGPSProvider(ctx, locationManager, notificationManager)
          instance = Some(newInstance)
          newInstance
        }
      }
    }
  }
}

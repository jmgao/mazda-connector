package us.insolit.connector.gps

import android.content.Context
import android.location.{Criteria, Location, LocationListener, LocationManager, LocationProvider}
import android.util.Log
import android.os.{Bundle, SystemClock}

class MockGPSProvider private (locationManager: LocationManager) {
  private val providerName = LocationManager.GPS_PROVIDER

  private var started = false

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
      fixedLocation.setTime(System.currentTimeMillis())
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
          val newInstance = new MockGPSProvider(locationManager)
          instance = Some(newInstance)
          newInstance
        }
      }
    }
  }
}

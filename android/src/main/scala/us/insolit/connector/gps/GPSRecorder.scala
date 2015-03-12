package us.insolit.connector.gps

import java.io.FileOutputStream
import java.util.Date

import android.content.Context
import android.location.{Criteria, Location, LocationListener, LocationManager, LocationProvider}
import android.os.{Looper, Parcel}
import android.util.Log

import us.insolit.connector.util._

class GPSRecorder(val locationManager: LocationManager, val looper: Looper) extends LocationListener {
  private var started: Boolean = false
  private var parcel: Parcel = null

  def start() {
    this.synchronized {
      if (!started) {
        started = true

        val criteria = new Criteria
        criteria.setAccuracy(Criteria.ACCURACY_FINE)
        criteria.setAltitudeRequired(true)
        criteria.setBearingRequired(true)
        criteria.setPowerRequirement(Criteria.POWER_HIGH)

        val bestProvider = locationManager.getBestProvider(criteria, true)
        Log.e("GPSRecorder", "Best provider is: " + bestProvider)

        val provider = locationManager.getProvider(bestProvider)
        Log.e("GPSRecorder", bestProvider + ".getAccuracy() = " + provider.getAccuracy())
        Log.e("GPSRecorder", bestProvider + ".getName() = " + provider.getName())
        Log.e("GPSRecorder", bestProvider + ".getPowerRequirement() = " + provider.getPowerRequirement())
        Log.e("GPSRecorder", bestProvider + ".hasMonetaryCost() = " + provider.hasMonetaryCost())
        Log.e("GPSRecorder", bestProvider + ".requiresCell() = " + provider.requiresCell())
        Log.e("GPSRecorder", bestProvider + ".requiresNetwork() = " + provider.requiresNetwork())
        Log.e("GPSRecorder", bestProvider + ".requiresSatellite() = " + provider.requiresSatellite())
        Log.e("GPSRecorder", bestProvider + ".supportsAltitude() = " + provider.supportsAltitude())
        Log.e("GPSRecorder", bestProvider + ".supportsBearing() = " + provider.supportsBearing())
        Log.e("GPSRecorder", bestProvider + ".supportsSpeed() = " + provider.supportsSpeed())

        parcel = Parcel.obtain()

        locationManager.requestLocationUpdates(bestProvider, 0, 0, this, looper)
      } else {
        Log.e("GPSRecorder", "GPS recorder already started");
      }
    }
  }

  def stop(saveFile: Option[String] = None) {
    val fileName = saveFile match {
      case Some(f) => f
      case None => {
        val dateFormat = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
        "/sdcard/%s.gps".format(dateFormat.format(new Date()))
      }
    }

    this.synchronized {
      if (started) {
        locationManager.removeUpdates(this)
        started = false

        val parcelBytes = parcel.marshall()
        if (parcelBytes.length == 0) {
          Log.e("GPSRecorder", "Not writing output to %s, parcel is empty".format(fileName))
        } else {
          Log.v("GPSRecorder", "Writing %d bytes to %s".format(parcelBytes.length, fileName))
          val os = new FileOutputStream(fileName)
          os.write(parcelBytes, 0, parcelBytes.length)
          os.close()
        }

        parcel.recycle()
      } else {
        Log.e("GPSRecorder", "GPSRecorder alredy stopped")
      }
    }
  }

  def onLocationChanged(location: Location) {
    this.synchronized {
      if (started) {
        Log.e("GPSRecorder", "onLocationChanged: " + location)
        location.writeToParcel(parcel, 0)
      }
    }
  }

  def onProviderDisabled(providerName: String): Unit = {
    this.synchronized {
      Log.e("GPSRecorder", "Provider %s was disabled".format(providerName))
    }
  }

  def onProviderEnabled(providerName: String): Unit = {}
  def onStatusChanged(providerName: String, status: Int, extras: android.os.Bundle): Unit = {}
}

object GPSRecorder {
  lazy val looper = spawnLooper()

  def apply(ctx: Context) = {
    new GPSRecorder(ctx.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager], looper)
  }
}

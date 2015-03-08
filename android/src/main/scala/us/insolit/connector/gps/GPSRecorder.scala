package us.insolit.connector.gps

import java.io.FileOutputStream
import java.util.Date

import android.content.Context
import android.location.{Criteria, Location, LocationListener, LocationManager, LocationProvider}
import android.os.{Looper, Parcel}
import android.util.Log

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.Success

class GPSRecorder(val locationManager: LocationManager, val looper: Looper) extends LocationListener {
  var started: Boolean = false
  var parcel: Parcel = null

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

  def onProviderEnabled(x$1: String): Unit = {}
  def onStatusChanged(x$1: String,x$2: Int,x$3: android.os.Bundle): Unit = {}
}

object GPSRecorder {
  lazy val looper = {
    val promise = Promise[Looper]()
    new Thread() {
      override def run() {
        Looper.prepare()
        promise.complete(Success(Looper.myLooper()))
        Looper.loop()
      }
    }.start()
    Await.result(promise.future, Duration.Inf)
  }

  def apply(ctx: Context) = {
    new GPSRecorder(ctx.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager], looper)
  }
}

package us.insolit.connector.gps

import android.content.Context
import android.location.Location
import android.os.{Looper, Parcel, SystemClock}
import android.util.Log

import scala.concurrent.duration._

import us.insolit.connector.util._

class GPSPlayer private (mockProvider: MockGPSProvider, parcel: Parcel) {
  var started: Boolean = false
  var looper: Looper = null
  var timeOffset: Option[Long] = None

  def start() {
    this.synchronized {
      if (started) {
        Log.e("GPSPlayer", "start called while already started")
        return
      }

      started = true
      looper = spawnLooper()

      looper.run {
        mockProvider.start()

        def tick() {
          if (parcel.dataAvail == 0) {
            Log.i("GPSPlayer", "Done replaying GPS data")
            this.stop()
            return
          }

          val location = Location.CREATOR.createFromParcel(parcel)
          val now = SystemClock.elapsedRealtimeNanos()
          val targetTime = timeOffset match {
            case None => {
              timeOffset = Some(now - location.getElapsedRealtimeNanos())
              now
            }

            case Some(time) => {
              time + location.getElapsedRealtimeNanos()
            }
          }

          Log.i("GPSPlayer", "Updating location with " + location)
          mockProvider.update(location)
          looper.runDelayed(tick, (targetTime - now).nanoseconds)
        }

        looper.run(tick)
      }
    }
  }

  def stop() {
    this.synchronized {
      if (!started) {
        Log.e("GPSPlayer", "stop called while not started")
        return
      }

      started = false
      looper.quit()
      looper = null
      timeOffset = None
      mockProvider.stop()
    }
  }
}

object GPSPlayer {
  def apply(ctx: Context, parcel: Parcel): GPSPlayer = {
    new GPSPlayer(MockGPSProvider.getInstance(ctx), parcel)
  }

  def apply(ctx: Context, bytes: Array[Byte]): GPSPlayer = {
    val parcel = Parcel.obtain()
    parcel.unmarshall(bytes, 0, bytes.length)
    parcel.setDataPosition(0)
    apply(ctx, parcel)
  }

  def apply(ctx: Context, fileName: String): GPSPlayer = {
    val bytes = readFile(fileName)
    apply(ctx, bytes)
  }
}

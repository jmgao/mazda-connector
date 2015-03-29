package us.insolit.connector.hook

import android.app.AndroidAppHelper
import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.hardware.{Sensor, SensorEvent, SensorEventListener, SensorListener, SensorManager, TriggerEventListener}
import android.os.{Handler, SystemClock}

import scala.collection.mutable.HashMap

import de.robv.android.xposed.XposedBridge;

import spire.math.Quaternion
import spire.implicits._

class CompassEventListener(listener: SensorEventListener) extends SensorEventListener {
  XposedBridge.log("Constructed CompassEventListener for listener %s".format(listener))

  override def onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    XposedBridge.log("onAccuracyChanged(%s, %d)".format(sensor, accuracy))
    listener.onAccuracyChanged(sensor, accuracy)
  }

  override def onSensorChanged(event: SensorEvent) {
    CompassEventListener.getBearing() match {
      case Some(bearing) => {
        val angle = (360.0f - bearing) * Math.PI / 180.0f

        // Normalize to (-pi, pi]
        val axisRotation =
          if (angle > Math.PI) {
            angle - 2 * Math.PI
          } else {
            angle
          }


        event.sensor.getType() match {
          case Sensor.TYPE_ROTATION_VECTOR => {
            // Pretend that the device is tilted a bit to avoid a degenerate rotation about [0, 0, 0]
            val tiltAngle = Math.PI / 4
            val tilt = Quaternion(Math.cos(tiltAngle/2), Math.sin(tiltAngle/2), 0.0, 0.0)
            val azimuth = Quaternion(Math.cos(axisRotation/2), 0.0, 0.0, Math.sin(axisRotation/2))

            val result = azimuth * tilt

            event.values(0) = result.i.toFloat
            event.values(1) = result.j.toFloat
            event.values(2) = result.k.toFloat
            if (event.values.length >= 4) {
              event.values(3) = result.r.toFloat
            }
            if (event.values.length >= 5) {
              event.values(4) = -1
            }
          }

          case Sensor.TYPE_ORIENTATION => {
            event.values(0) = bearing.toFloat
            event.values(1) = 0.0f
            event.values(2) = 0.0f

            if (event.values.length >= 6) {
              event.values(4) = bearing.toFloat
              event.values(5) = 0.0f
              event.values(6) = 0.0f
            }
          }

          case Sensor.TYPE_MAGNETIC_FIELD => {
            val origX = event.values(0)
            val origY = event.values(1)
            val origZ = event.values(2)
            val magnitude = Math.sqrt(origX * origX + origY * origY + origZ * origZ)

            // NOTE: These values are rotation (i.e. counterclockwise), not bearing
            // North   (0): x = 0, z = -1
            // West  (90): x = 1, z = 0
            // South (180): x = 0, z = 1
            // East   (270): x = -1, z = 0
            event.values(0) = (magnitude * Math.sin(axisRotation)).toFloat
            event.values(1) = 0.0f
            event.values(2) = (-magnitude * Math.cos(axisRotation)).toFloat
          }

          case unhandledType => {
            XposedBridge.log("onSensorChanged(UNKNOWN: %d)".format(unhandledType))
          }
        }
      }

      case None => {}
    }

    listener.onSensorChanged(event)
  }
}

object CompassEventListener {
  val timeout = 2000 // ms
  var lastReceived: Long = -timeout
  var lastBearing: Option[Float] = None

  def getBearing(): Option[Float] = {
    registerListener()

    this.synchronized {
      val now = SystemClock.elapsedRealtime()
      if (now - lastReceived < timeout) {
        lastBearing
      } else {
        None
      }
    }
  }

  var registered = false
  def registerListener() {
    this.synchronized {
      if (!registered) {
        val ctx = AndroidAppHelper.currentApplication()

        ctx.registerReceiver(
          new BroadcastReceiver {
            override def onReceive(ctx: Context, intent: Intent) = {
              val bearing = intent.getFloatExtra("bearing", -1.0f);
              if (bearing < 0.0f) {
                lastBearing = None
              } else {
                lastBearing = Some(bearing)
              }

              lastReceived = SystemClock.elapsedRealtime()
            }
          },
          new IntentFilter("us.insolit.connector.BEARING")
        )

        registered = true
      }
    }
  }
}

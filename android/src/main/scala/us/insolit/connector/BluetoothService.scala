package us.insolit.connector

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.location.Location
import android.os.{Handler, IBinder, Looper, SystemClock}
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent

import us.insolit.connector.bt._
import us.insolit.connector.gps._
import us.insolit.connector.vr._

import squants.motion.KilometersPerHour
import squants.motion.MetersPerSecond

class BluetoothService extends Service {
  override def onCreate() {
    super.onCreate()
    Log.v("MazdaConnector", "Service started")
    Future {
      val adapter = BluetoothAdapter.getDefaultAdapter();
      val uuid = UUID.fromString(getString(R.string.connector_uuid))
      val serverSocket = adapter.listenUsingRfcommWithServiceRecord("connector", uuid);

      val tts: TextToSpeech = new TextToSpeech(this,
        new TextToSpeech.OnInitListener() {
          override def onInit(status: Int) {
          }
        }
      )

      while (true) {
        val socket = serverSocket.accept()
        Future {
          try {
            val is = socket.getInputStream()
            val os = socket.getOutputStream()

            while (true) {
              val datagram = Datagram.read(is)
              datagram match {
                case InputDatagram(input) => {
                  if (input.tapCount == 1) {
                    if (input.longPress) {
                      WakeUpActivity.start(this)
                    } else {
                      val time = SystemClock.uptimeMillis();
                      val downEvent = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                      val downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null).putExtra(Intent.EXTRA_KEY_EVENT, downEvent)
                      sendOrderedBroadcast(downIntent, null);

                      val upEvent = new KeyEvent(time, time, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
                      val upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null).putExtra(Intent.EXTRA_KEY_EVENT, upEvent)
                      sendOrderedBroadcast(upIntent, null);
                    }
                  } else if (input.tapCount == 2) {
                    if (!input.longPress) {
                      tts.speak("Starting GPS recording", TextToSpeech.QUEUE_ADD, null)
                      GPSRecordReceiver.startRecord(this)
                    } else {
                      tts.speak("Stopping GPS recording", TextToSpeech.QUEUE_ADD, null)
                      GPSRecordReceiver.stopRecord()
                    }
                  } else {
                    val message = "Received keycode %d%s count %d".format(
                      input.keycode,
                      if (input.longPress) "long press " else "",
                      input.tapCount
                    )
                    tts.speak(message, TextToSpeech.QUEUE_ADD, null)
                  }
                }

                case LocationUpdateDatagram(locationUpdate) => {
                  val gpsProvider = MockGPSProvider.getInstance(this)
                  if (gpsProvider.isRunning) {
                    val location = new Location(gpsProvider.providerName)
                    location.setAltitude(locationUpdate.altitude)
                    location.setBearing(locationUpdate.heading.toFloat)
                    location.setLatitude(locationUpdate.latitude)
                    location.setLongitude(locationUpdate.longitude)
                    val velocity = KilometersPerHour(locationUpdate.velocity)
                    location.setSpeed((velocity to MetersPerSecond).toFloat)
                    gpsProvider.update(location)
                  } else {
                    Log.e("MazdaConnector", "Received LocationUpdateDatagram while MockGPSProvider is stopped")
                  }
                }

                case _ => {
                  Log.e("MazdaConnector", "ERROR: received unexpected datagram with tag 0x%x".format(datagram.tag))
                }
              }
            }
          } catch {
            case ex : Throwable => Log.e("MazdaConnector", "Received exception", ex)
            MockGPSProvider.getInstance(this).stop()
          }

          socket.close()
        }
      }
    }
  }

  override def onBind(intent: Intent): IBinder = ???
}


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
import android.os.{Handler, IBinder, Looper, SystemClock}
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent

import us.insolit.connector.gps._
import us.insolit.connector.vr._

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
              // length of the packet
              val header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
              if (is.read(header.array) != header.array.length) {
                throw new IOException("BluetoothSocket returned short when reading header (wanted " +
                  header.array.length + "bytes)")
              }

              val length = header.getInt
              if (length < 0) {
                throw new IOException("BluetoothSocket received length > 2GB, wtf")
              }

              val packet = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
              if (is.read(packet.array) != packet.array.length) {
                throw new IOException("BluetoothSocket returned short when reading data")
              }

              val packetString = new String(packet.array(), Charset.forName("UTF-8"))
              // Expected format: <TAG>:<MSG>
              val fields = packetString.split(":", 2)
              if (fields.length != 2) {
                throw new IOException("Received malformed message: " + packet)
              }

              fields(0) match {
                case "Input" => {
                  val input = fields(1).split(";")
                  if (input.length != 3) {
                    throw new IOException("Received malformed input message: " + fields(1))
                  }

                  val keycode = input(0).toInt
                  val longPress = input(1) == "1"
                  val tapCount = input(2).toInt

                  if (tapCount == 1) {
                    if (longPress) {
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
                  } else if (tapCount == 2) {
                    if (!longPress) {
                      tts.speak("Starting GPS recording", TextToSpeech.QUEUE_ADD, null)
                      GPSRecordReceiver.startRecord(this)
                    } else {
                      tts.speak("Stopping GPS recording", TextToSpeech.QUEUE_ADD, null)
                      GPSRecordReceiver.stopRecord()
                    }
                  } else {
                    var message = "Received keycode " + input(0) + ", "
                    if (longPress) {
                      message += "long press "
                    }
                    message += "count " + input(2)
                    tts.speak(message, TextToSpeech.QUEUE_ADD, null)
                  }
                }

                case tag => {
                  throw new IOException("Unhandled tag: " + tag)
                }
              }
            }
          } catch {
            case ex : Throwable => Log.e("MazdaConnector", "Received exception", ex)
          }

          socket.close()
        }
      }
    }
  }

  override def onBind(intent: Intent): IBinder = ???
}


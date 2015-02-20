package us.insolit.mazdaconnector

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
import android.util.Log
import android.os.{Handler, IBinder, Looper}

class BluetoothService extends Service {
  // FIXME: Move this somewhere else
  def runOnMainThread(f: => Unit) {
    val handler = new Handler(Looper.getMainLooper())
    handler.post(new Runnable {
      override def run() = f
    })
  }

  override def onCreate() {
    super.onCreate()
    Log.v("MazdaConnector", "Service started")
    Future {
      val adapter = BluetoothAdapter.getDefaultAdapter();
      val uuid = UUID.fromString(getString(R.string.google_now_uuid))
      val serverSocket = adapter.listenUsingRfcommWithServiceRecord("google_now", uuid);


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

              // TODO: specify an actual format and use it instead of doing this blindly
              WakeUpActivity.start(this)
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


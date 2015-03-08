package us.insolit.connector.gps

import android.content.{BroadcastReceiver, Context, Intent}

class GPSRecordReceiver extends BroadcastReceiver {
  override def onReceive(ctx: Context, intent: Intent) {
    intent.getAction() match {
      case "us.insolit.connector.START_GPS_RECORD" => {
        GPSRecordReceiver.start(ctx)
      }

      case "us.insolit.connector.STOP_GPS_RECORD" => {
        GPSRecordReceiver.stop()
      }
    }
  }
}

object GPSRecordReceiver {
  private var gpsRecorder: Option[GPSRecorder] = None

  def start(ctx: Context) {
    gpsRecorder = Some(GPSRecorder(ctx))
    gpsRecorder foreach { _.start() }
  }

  def stop() {
    gpsRecorder foreach { _.stop() }
    gpsRecorder = None
  }
}

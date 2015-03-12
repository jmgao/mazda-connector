package us.insolit.connector.gps

import android.content.{BroadcastReceiver, Context, Intent}

class GPSRecordReceiver extends BroadcastReceiver {
  override def onReceive(ctx: Context, intent: Intent) {
    intent.getAction() match {
      case "us.insolit.connector.START_GPS_RECORD" => {
        GPSRecordReceiver.startRecord(ctx)
      }

      case "us.insolit.connector.STOP_GPS_RECORD" => {
        GPSRecordReceiver.stopRecord()
      }

      case "us.insolit.connector.START_GPS_PLAYBACK" => {
        GPSRecordReceiver.startPlayback(ctx)
      }

      case "us.insolit.connector.STOP_GPS_PLAYBACK" => {
        GPSRecordReceiver.stopPlayback()
      }
    }
  }
}

object GPSRecordReceiver {
  private var gpsRecorder: Option[GPSRecorder] = None
  private var gpsPlayer: Option[GPSPlayer] = None

  def startRecord(ctx: Context) {
    gpsRecorder = Some(GPSRecorder(ctx))
    gpsRecorder foreach { _.start() }
  }

  def stopRecord() {
    gpsRecorder foreach { _.stop() }
    gpsRecorder = None
  }

  def startPlayback(ctx: Context) {
    gpsPlayer = Some(GPSPlayer(ctx, "/sdcard/recorded.gps"))
    gpsPlayer foreach { _.start() }
  }

  def stopPlayback() {
    gpsPlayer foreach { _.stop() }
    gpsPlayer = None
  }
}

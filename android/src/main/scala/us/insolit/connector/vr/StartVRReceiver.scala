package us.insolit.connector.vr

import android.content.{BroadcastReceiver, Context, Intent}

class StartVRReceiver extends BroadcastReceiver {
  override def onReceive(ctx: Context, intent: Intent) {
    WakeUpActivity.start(ctx)
  }
}

package us.insolit.mazdaconnector

import android.content.{BroadcastReceiver, Context, Intent}

class StartVRReceiver extends BroadcastReceiver {
  override def onReceive(ctx: Context, intent: Intent) {
    WakeUpActivity.start(ctx)
  }
}

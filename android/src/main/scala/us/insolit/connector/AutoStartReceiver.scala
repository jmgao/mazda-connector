package us.insolit.connector

import android.content.{BroadcastReceiver, Context, Intent}

class AutoStartReceiver extends BroadcastReceiver {
  override def onReceive(ctx: Context, intent: Intent) {
    ctx.startService(new Intent(ctx, classOf[BluetoothService]))
  }
}

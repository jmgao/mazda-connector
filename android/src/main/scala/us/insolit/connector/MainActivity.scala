package us.insolit.connector

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity extends Activity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    startService(new Intent(this, classOf[BluetoothService]))
  }
}

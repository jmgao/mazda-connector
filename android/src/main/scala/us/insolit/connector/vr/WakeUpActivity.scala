package us.insolit.connector.vr

import android.app.Activity
import android.content.{ComponentName, Context, Intent}
import android.os.Bundle
import android.view.WindowManager.LayoutParams

class WakeUpActivity extends Activity {
  var started: Boolean = _

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)

    started = false
  }

  override def onStart() {
    super.onStart()

    val window = getWindow()
    window.addFlags(
      LayoutParams.FLAG_DISMISS_KEYGUARD |
      LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      LayoutParams.FLAG_TURN_SCREEN_ON |
      LayoutParams.FLAG_KEEP_SCREEN_ON
    )
  }

  override def onStop() {
    super.onStop();
    finish()
  }

  override def onWindowFocusChanged(focus: Boolean) {
    if (focus && !started) {
      started = true
      this.startGoogleNow()
    }
  }

  def startGoogleNow() {
    val googleNowComponent = new ComponentName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity")
    val googleNowIntent = new Intent(Intent.ACTION_MAIN)
    googleNowIntent.addCategory(Intent.CATEGORY_DEFAULT)
    googleNowIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    googleNowIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
    googleNowIntent.setComponent(googleNowComponent)

    startActivity(googleNowIntent)
  }
}

object WakeUpActivity {
  def start(ctx: Context) {
    ctx.startActivity(new Intent(ctx, classOf[WakeUpActivity]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP))
  }
}

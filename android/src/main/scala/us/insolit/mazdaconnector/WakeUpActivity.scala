package us.insolit.mazdaconnector

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
                                .addCategory(Intent.CATEGORY_DEFAULT)
                                .addCategory(Intent.CATEGORY_LAUNCHER)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .setComponent(googleNowComponent)

    startActivity(googleNowIntent)
  }
}

object WakeUpActivity {
  def start(ctx: Context) {
    ctx.startActivity(new Intent(ctx, classOf[WakeUpActivity]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP))
  }
}

package us.insolit.mazdaconnector

import android.app.Activity
import android.content.{ComponentName, Context, Intent}
import android.view.WindowManager.LayoutParams

class WakeUpActivity extends Activity {
  var started = false
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

  override def onWindowFocusChanged(focus: Boolean) {
    if (focus) {
      WakeUpActivity.activity = this
      this.startGoogleNow()
    }
  }

  def startGoogleNow() {
    val googleNowComponent = new ComponentName("com.google.android.googlequicksearchbox", "com.google.android.googlequicksearchbox.VoiceSearchActivity")
    val googleNowIntent = new Intent(Intent.ACTION_VOICE_COMMAND).setComponent(googleNowComponent)//.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(googleNowIntent)
  }

  override def onDestroy() {
    super.onDestroy()
    WakeUpActivity.activity = null
    finishAffinity()
  }
}

object WakeUpActivity {
  var activity: WakeUpActivity = null

  def start(ctx: Context) {
    if (activity != null) {
      activity.startGoogleNow()
    } else {
      ctx.startActivity(new Intent(ctx, classOf[WakeUpActivity]).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
  }
}

package us.insolit.connector

import java.io.{IOException, RandomAccessFile}

import android.os.{Handler, Looper}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.Success

package object util {
  def spawnLooper(): Looper = {
    val promise = Promise[Looper]()
    new Thread() {
      override def run() {
        Looper.prepare()
        promise.complete(Success(Looper.myLooper()))
        Looper.loop()
      }
    }.start()
    Await.result(promise.future, Duration.Inf)
  }

  def readFile(fileName: String) = {
    val raf = new RandomAccessFile(fileName, "r");
    try {
      val length = raf.length.toInt
      if (length != raf.length) {
        throw new IOException("File '%s' has length > 2GB (%l)".format(fileName, raf.length))
      }

      val buf = new Array[Byte](length);
      raf.readFully(buf);
      buf
    } finally {
      raf.close()
    }
  }

  def runOnMainThread(f: => Unit) {
    val handler = new Handler(Looper.getMainLooper())
    handler.post(new Runnable {
      override def run() = f
    })
  }

  implicit class FunctionRunnable(function: => Unit) extends Runnable {
    override def run() = function
  }

  implicit class RichLooper(looper: Looper) {
    def run(function: => Unit) {
      val runnable: Runnable = function
      new Handler(looper).post(function)
    }

    def runDelayed(function: => Unit, duration: Duration) {
      new Handler(looper).postDelayed(function, duration.toMillis)
    }

    def runAtTime(function: => Unit, uptime: Long) {
      new Handler(looper).postAtTime(function, uptime)
    }
  }
}

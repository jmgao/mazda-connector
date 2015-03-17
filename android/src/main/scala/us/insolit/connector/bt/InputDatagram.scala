package us.insolit.connector.bt

import java.io.IOException
import java.nio.{ByteBuffer, ByteOrder}

final class InputDatagram private (val keycode: Int, val tapCount: Int, val longPress: Boolean) extends Datagram {
  override def tag = InputDatagram.tag
  override lazy val data: Array[Byte] = {
    val buffer = ByteBuffer.allocate(InputDatagram.length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(keycode)
    buffer.putInt(tapCount)
    if (longPress) {
      buffer.put(1.toByte)
    } else {
      buffer.put(0.toByte)
    }
    buffer.array
  }
}

object InputDatagram {
  val tag = 0x54504e49 // 'INPT'
  val length = 9

  def apply(keycode: Int, tapCount: Int, longPress: Boolean) = {
    new InputDatagram(keycode, tapCount, longPress)
  }

  def unapply(dg: Datagram): Option[InputDatagram] = {
    if (dg.tag != tag) {
      None
    } else if (dg.data.length != length) {
      throw new IOException("Incorrect length for InputDatagram")
    } else {
      val buffer = ByteBuffer.wrap(dg.data).order(ByteOrder.LITTLE_ENDIAN)
      val keycode = buffer.getInt()
      val tapCount = buffer.getInt()

      if (tapCount <= 0) {
        throw new IOException("Received invalid value for tapCount: 0x%x".format(tapCount))
      }

      val longPress = buffer.get() match {
        case 0 => false
        case 1 => true
        case x => throw new IOException("Received invalid value for longPress: 0x%x".format(x))
      }

      Some(new InputDatagram(keycode, tapCount, longPress))
    }
  }
}

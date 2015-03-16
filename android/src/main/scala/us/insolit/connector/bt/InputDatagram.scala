package us.insolit.connector.bt

import java.io.{InputStream, IOException}
import java.nio.{ByteBuffer, ByteOrder}
import com.google.common.io.LittleEndianDataInputStream

final class InputDatagram private (val keycode: Int, val longPress: Boolean, val tapCount: Int) extends
  Datagram(InputDatagram.tag, InputDatagram.serialize(keycode, longPress, tapCount)) {
}

object InputDatagram {
  // 'INPT'
  val tag = 0x54504e49
  val length = 9

  private def serialize(keycode: Int, longPress: Boolean, tapCount: Int): Array[Byte] = {
    val buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(keycode)
    if (longPress) {
      buffer.put(1.toByte)
    } else {
      buffer.put(0.toByte)
    }
    buffer.putInt(tapCount)
    buffer.array
  }

  def apply(keycode: Int, longPress: Boolean, tapCount: Int) = {
    new InputDatagram(keycode, longPress, tapCount)
  }

  def unapply(dg: Datagram): Option[InputDatagram] = {
    if (dg.tag != tag) {
      None
    } else if (dg.data.length != length) {
      throw new IOException("Incorrect length for InputDatagram")
    } else {
      val buffer = ByteBuffer.wrap(dg.data).order(ByteOrder.LITTLE_ENDIAN)
      val keycode = buffer.getInt()
      val longPress = buffer.get() match {
        case 0 => false
        case 1 => true
        case x => throw new IOException("Received invalid value for longPress: 0x%x".format(x))
      }
      val tapCount = buffer.getInt()

      if (tapCount <= 0) {
        throw new IOException("Received invalid value for tapCount: 0x%x".format(tapCount))
      }

      Some(new InputDatagram(keycode, longPress, tapCount))
    }
  }
}

package us.insolit.connector.bt

import java.io.ByteArrayInputStream
import java.nio.{ByteBuffer, ByteOrder}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks

class InputDatagramSpec extends FlatSpec with PropertyChecks {
  "InputDatagram" should "roundtrip" in {
    forAll { (keycode: Int, longPress: Boolean, tapCount: Int) =>
      whenever (tapCount >= 1) {
        val constructed = InputDatagram(keycode, longPress, tapCount)
        val InputDatagram(extracted) = constructed
        constructed.keycode == extracted.keycode &&
        constructed.longPress == extracted.longPress &&
        constructed.tapCount == extracted.tapCount
      }
    }
  }

  it should "decode" in {
    val keycode = 0x12345678
    val tapCount = 0x00ABCDEF // has to be positive

    val buffer = ByteBuffer.allocate(17).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(13)
    buffer.put("INPT".getBytes("UTF-8"))
    buffer.putInt(keycode)
    buffer.put(1.toByte)
    buffer.putInt(tapCount)
    val is = new ByteArrayInputStream(buffer.array)
    val dg = Datagram.read(is)
    val InputDatagram(input) = dg
    assert(input.keycode == keycode)
    assert(input.longPress)
    assert(input.tapCount == tapCount)
  }
}

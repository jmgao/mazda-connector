package us.insolit.connector.bt

import java.io.{InputStream, IOException}
import java.nio.{ByteBuffer, ByteOrder}
import com.google.common.io.LittleEndianDataInputStream

class Datagram protected (var tag: Int, var data: Array[Byte]) {
  def length = 4 + data.length

  def encode(): ByteBuffer = {
    val buffer = ByteBuffer.allocate(length)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(tag)
    buffer.put(data)
    buffer
  }
}

object Datagram {
  def read(inputStream: InputStream): Datagram = {
    val is = new LittleEndianDataInputStream(inputStream)
    val length = is.readInt()

    if (length < 0) {
      throw new IOException("Datagram received length " + length);
    }

    val tag = is.readInt()
    val data = new Array[Byte](length - 4)
    is.readFully(data)
    new Datagram(tag, data)
  }
}

package us.insolit.connector.bt

import java.io.{DataInputStream, InputStream, IOException}
import java.nio.{ByteBuffer, ByteOrder}

trait Datagram {
  def tag: Int
  def data: Array[Byte]

  final def encode(): ByteBuffer = {
    ByteBuffer.allocate(data.length + 4)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putInt(tag)
              .put(data)
  }
}

object Datagram {
  private final class DatagramImpl(override val tag: Int, override val data: Array[Byte]) extends Datagram

  def read(inputStream: InputStream): Datagram = {
    val is = new DataInputStream(inputStream)

    val lengthBuffer = new Array[Byte](4)
    is.readFully(lengthBuffer)

    val length = ByteBuffer.wrap(lengthBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt()
    if (length < 0) {
      throw new IOException("Datagram received length " + length);
    }

    val dataBuffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN)
    is.readFully(dataBuffer.array())

    val tag = dataBuffer.getInt()
    val data = new Array[Byte](length - 4)
    dataBuffer.get(data)
    new DatagramImpl(tag, data)
  }
}

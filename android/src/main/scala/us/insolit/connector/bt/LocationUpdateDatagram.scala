package us.insolit.connector.bt

import java.io.IOException
import java.nio.{ByteBuffer, ByteOrder}

final class LocationUpdateDatagram private (val accuracy: Int,
                                            val timestamp: Long,
                                            val latitude: Double,
                                            val longitude: Double,
                                            val altitude: Int,
                                            val heading: Double,
                                            val velocity: Double,
                                            val horizontalAccuracy: Double,
                                            val verticalAccuracy: Double) extends Datagram {
  override def tag = LocationUpdateDatagram.tag
  override lazy val data: Array[Byte] = {
    val buffer = ByteBuffer.allocate(LocationUpdateDatagram.length).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(accuracy)
          .putLong(timestamp)
          .putDouble(latitude)
          .putDouble(longitude)
          .putInt(altitude)
          .putDouble(heading)
          .putDouble(velocity)
          .putDouble(horizontalAccuracy)
          .putDouble(verticalAccuracy)
    buffer.array
  }
}

object LocationUpdateDatagram {
  // 'GPS!'
  val tag = 0x21535047
  val length = 64

  def apply(accuracy: Int,
            timestamp: Long,
            latitude: Double,
            longitude: Double,
            altitude: Int,
            heading: Double,
            velocity: Double,
            horizontalAccuracy: Double,
            verticalAccuracy: Double) = {
    new LocationUpdateDatagram(
      accuracy,
      timestamp,
      latitude,
      longitude,
      altitude,
      heading,
      velocity,
      horizontalAccuracy,
      verticalAccuracy
    )
  }

  def unapply(dg: Datagram): Option[LocationUpdateDatagram] = {
    if (dg.tag != tag) {
      None
    } else if (dg.data.length != length) {
      throw new IOException("Incorrect length for LocationUpdateDatagram")
    } else {
      val buffer = ByteBuffer.wrap(dg.data).order(ByteOrder.LITTLE_ENDIAN)
      Some(
        new LocationUpdateDatagram(
          buffer.getInt,
          buffer.getLong,
          buffer.getDouble,
          buffer.getDouble,
          buffer.getInt,
          buffer.getDouble,
          buffer.getDouble,
          buffer.getDouble,
          buffer.getDouble
        )
      )
    }
  }
}

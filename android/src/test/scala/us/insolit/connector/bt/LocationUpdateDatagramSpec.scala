package us.insolit.connector.bt

import java.io.ByteArrayInputStream
import java.nio.{ByteBuffer, ByteOrder}
import org.scalatest.FlatSpec
import org.scalatest.prop.PropertyChecks
import org.scalacheck.Arbitrary.arbitrary

class LocationUpdateDatagramSpec extends FlatSpec with PropertyChecks {
  val generator = for {
    accuracy <- arbitrary[Int];
    timestamp <- arbitrary[Long];
    latitude <- arbitrary[Double];
    longitude <- arbitrary[Double];
    altitude <- arbitrary[Int];
    heading <- arbitrary[Double];
    velocity <- arbitrary[Double];
    horizontalAccuracy <- arbitrary[Double];
    verticalAccuracy <- arbitrary[Double]
  } yield (accuracy, timestamp, latitude, longitude, altitude, heading, velocity, horizontalAccuracy, verticalAccuracy)

  "LocationUpdateDatagram" should "roundtrip" in {
    forAll(generator) {
      args => {
        val constructor = (LocationUpdateDatagram.apply _).tupled
        val constructed: LocationUpdateDatagram = constructor(args)
        val LocationUpdateDatagram(extracted) = constructed
        constructed.accuracy == extracted.accuracy &&
        constructed.timestamp == extracted.timestamp &&
        constructed.latitude == extracted.latitude &&
        constructed.longitude == extracted.longitude &&
        constructed.altitude == extracted.altitude &&
        constructed.heading == extracted.heading &&
        constructed.velocity == extracted.velocity &&
        constructed.horizontalAccuracy == extracted.horizontalAccuracy &&
        constructed.verticalAccuracy == extracted.verticalAccuracy
      }
    }
  }
}

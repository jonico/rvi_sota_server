/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.rest

import cats.data.Xor
import io.circe.{Encoder, Decoder, Json}
import Json.{obj, string}

object ErrorCodes {
  val InvalidEntity = new ErrorCode("invalid_entity")
  val DuplicateEntry = new ErrorCode("duplicate_entry")
}

case class ErrorRepresentation( code: ErrorCode, description: String )

object ErrorRepresentation {
  import io.circe.generic.semiauto._
  implicit val encoderInstance = deriveFor[ErrorRepresentation].encoder
  implicit val decoderInstance = deriveFor[ErrorRepresentation].decoder

}

case class ErrorCode(code: String) extends AnyVal

object ErrorCode {
  implicit val encoderInstance : Encoder[ErrorCode] = Encoder[String].contramap( _.code )
  implicit val decoderInstance : Decoder[ErrorCode] = Decoder[String].map( ErrorCode.apply )
}

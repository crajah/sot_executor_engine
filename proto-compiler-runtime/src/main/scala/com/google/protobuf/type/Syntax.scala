// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package com.google.protobuf.`type`

import scala.collection.JavaConverters._

sealed trait Syntax extends _root_.com.trueaccord.scalapb.GeneratedEnum {
  type EnumType = Syntax
  def isSyntaxProto2: Boolean = false
  def isSyntaxProto3: Boolean = false
  def companion: _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[Syntax] = com.google.protobuf.`type`.Syntax
}

object Syntax extends _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[Syntax] {
  implicit def enumCompanion: _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[Syntax] = this
  @SerialVersionUID(0L)
  case object SYNTAX_PROTO2 extends Syntax {
    val value = 0
    val index = 0
    val name = "SYNTAX_PROTO2"
    override def isSyntaxProto2: Boolean = true
  }
  
  @SerialVersionUID(0L)
  case object SYNTAX_PROTO3 extends Syntax {
    val value = 1
    val index = 1
    val name = "SYNTAX_PROTO3"
    override def isSyntaxProto3: Boolean = true
  }
  
  @SerialVersionUID(0L)
  case class Unrecognized(value: Int) extends Syntax with _root_.com.trueaccord.scalapb.UnrecognizedEnum
  
  lazy val values = scala.collection.Seq(SYNTAX_PROTO2, SYNTAX_PROTO3)
  def fromValue(value: Int): Syntax = value match {
    case 0 => SYNTAX_PROTO2
    case 1 => SYNTAX_PROTO3
    case __other => Unrecognized(__other)
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.EnumDescriptor = TypeProto.javaDescriptor.getEnumTypes.get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.EnumDescriptor = TypeProto.scalaDescriptor.enums(0)
  def fromJavaValue(pbJavaSource: com.google.protobuf.Syntax): Syntax = fromValue(pbJavaSource.getNumber)
  def toJavaValue(pbScalaSource: Syntax): com.google.protobuf.Syntax = com.google.protobuf.Syntax.forNumber(pbScalaSource.value)
}
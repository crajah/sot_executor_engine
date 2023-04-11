// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package com.google.protobuf.struct

import scala.collection.JavaConverters._

/** `ListValue` is a wrapper around a repeated field of values.
  *
  * The JSON representation for `ListValue` is JSON array.
  *
  * @param values
  *   Repeated field of dynamically typed values.
  */
@SerialVersionUID(0L)
final case class ListValue(
    values: _root_.scala.collection.Seq[com.google.protobuf.struct.Value] = _root_.scala.collection.Seq.empty
    ) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[ListValue] with com.trueaccord.lenses.Updatable[ListValue] {
    @transient
    private[this] var __serializedSizeCachedValue: Int = 0
    private[this] def __computeSerializedValue(): Int = {
      var __size = 0
      values.foreach(values => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(values.serializedSize) + values.serializedSize)
      __size
    }
    final override def serializedSize: Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): Unit = {
      values.foreach { __v =>
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): com.google.protobuf.struct.ListValue = {
      val __values = (_root_.scala.collection.immutable.Vector.newBuilder[com.google.protobuf.struct.Value] ++= this.values)
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __values += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, com.google.protobuf.struct.Value.defaultInstance)
          case tag => _input__.skipField(tag)
        }
      }
      com.google.protobuf.struct.ListValue(
          values = __values.result()
      )
    }
    def clearValues = copy(values = _root_.scala.collection.Seq.empty)
    def addValues(__vs: com.google.protobuf.struct.Value*): ListValue = addAllValues(__vs)
    def addAllValues(__vs: TraversableOnce[com.google.protobuf.struct.Value]): ListValue = copy(values = values ++ __vs)
    def withValues(__v: _root_.scala.collection.Seq[com.google.protobuf.struct.Value]): ListValue = copy(values = __v)
    def getFieldByNumber(__fieldNumber: Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => values
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(values.map(_.toPMessage)(_root_.scala.collection.breakOut))
      }
    }
    override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
    def companion = com.google.protobuf.struct.ListValue
}

object ListValue extends com.trueaccord.scalapb.GeneratedMessageCompanion[com.google.protobuf.struct.ListValue] with com.trueaccord.scalapb.JavaProtoSupport[com.google.protobuf.struct.ListValue, com.google.protobuf.ListValue] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[com.google.protobuf.struct.ListValue] with com.trueaccord.scalapb.JavaProtoSupport[com.google.protobuf.struct.ListValue, com.google.protobuf.ListValue] = this
  def toJavaProto(scalaPbSource: com.google.protobuf.struct.ListValue): com.google.protobuf.ListValue = {
    val javaPbOut = com.google.protobuf.ListValue.newBuilder
    javaPbOut.addAllValues(scalaPbSource.values.map(com.google.protobuf.struct.Value.toJavaProto)(_root_.scala.collection.breakOut).asJava)
    javaPbOut.build
  }
  def fromJavaProto(javaPbSource: com.google.protobuf.ListValue): com.google.protobuf.struct.ListValue = com.google.protobuf.struct.ListValue(
    values = javaPbSource.getValuesList.asScala.map(com.google.protobuf.struct.Value.fromJavaProto)(_root_.scala.collection.breakOut)
  )
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): com.google.protobuf.struct.ListValue = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    com.google.protobuf.struct.ListValue(
      __fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[_root_.scala.collection.Seq[com.google.protobuf.struct.Value]]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[com.google.protobuf.struct.ListValue] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      com.google.protobuf.struct.ListValue(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.collection.Seq[com.google.protobuf.struct.Value]]).getOrElse(_root_.scala.collection.Seq.empty)
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = StructProto.javaDescriptor.getMessageTypes.get(2)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = StructProto.scalaDescriptor.messages(2)
  def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = com.google.protobuf.struct.Value
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
  def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = com.google.protobuf.struct.ListValue(
  )
  implicit class ListValueLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, com.google.protobuf.struct.ListValue]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, com.google.protobuf.struct.ListValue](_l) {
    def values: _root_.com.trueaccord.lenses.Lens[UpperPB, _root_.scala.collection.Seq[com.google.protobuf.struct.Value]] = field(_.values)((c_, f_) => c_.copy(values = f_))
  }
  final val VALUES_FIELD_NUMBER = 1
}

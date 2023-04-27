package parallelai.sot.engine.taps.pubsub

@SerialVersionUID(0L) final case class MessageProto(user: String, teamName: String, score: Long, eventTime: Long, eventTimeStr: String) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[MessageProto] with com.trueaccord.lenses.Updatable[MessageProto] {
  @transient private[this] var __serializedSizeCachedValue: Int = 0

  private[this] def __computeSerializedValue(): Int = {
    var __size = 0
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, user)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, teamName)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(3, score)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(4, eventTime)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, eventTimeStr)
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

  def writeTo(_output__ : _root_.com.google.protobuf.CodedOutputStream): Unit = {
    _output__.writeString(1, user)
    _output__.writeString(2, teamName)
    _output__.writeInt64(3, score)
    _output__.writeInt64(4, eventTime)
    _output__.writeString(5, eventTimeStr)
  }

  def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.engine.taps.pubsub.MessageProto = {
    var __user = this.user
    var __teamName = this.teamName
    var __score = this.score
    var __eventTime = this.eventTime
    var __eventTimeStr = this.eventTimeStr
    var __requiredFields0: Long = 31L
    var _done__ = false
    while (!_done__) {
      val _tag__ = _input__.readTag()
      _tag__ match {
        case 0 =>
          _done__ = true
        case 10 =>
          __user = _input__.readString()
          __requiredFields0 &= -2L
        case 18 =>
          __teamName = _input__.readString()
          __requiredFields0 &= -3L
        case 24 =>
          __score = _input__.readInt64()
          __requiredFields0 &= -5L
        case 32 =>
          __eventTime = _input__.readInt64()
          __requiredFields0 &= -9L
        case 42 =>
          __eventTimeStr = _input__.readString()
          __requiredFields0 &= -17L
        case tag =>
          _input__.skipField(tag)
      }
    }
    if (__requiredFields0 != 0L) {
      throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
    }
    parallelai.sot.engine.taps.pubsub.MessageProto(user = __user, teamName = __teamName, score = __score, eventTime = __eventTime, eventTimeStr = __eventTimeStr)
  }

  def withUser(__v: String): MessageProto = copy(user = __v)

  def withTeamName(__v: String): MessageProto = copy(teamName = __v)

  def withScore(__v: Long): MessageProto = copy(score = __v)

  def withEventTime(__v: Long): MessageProto = copy(eventTime = __v)

  def withEventTimeStr(__v: String): MessageProto = copy(eventTimeStr = __v)

  def getFieldByNumber(__fieldNumber: Int): scala.Any = {
    (__fieldNumber: @_root_.scala.unchecked) match {
      case 1 => user
      case 2 => teamName
      case 3 => score
      case 4 => eventTime
      case 5 => eventTimeStr
    }
  }

  def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
    require(__field.containingMessage eq companion.scalaDescriptor)
    (__field.number: @_root_.scala.unchecked) match {
      case 1 =>
        _root_.scalapb.descriptors.PString(user)
      case 2 =>
        _root_.scalapb.descriptors.PString(teamName)
      case 3 =>
        _root_.scalapb.descriptors.PLong(score)
      case 4 =>
        _root_.scalapb.descriptors.PLong(eventTime)
      case 5 =>
        _root_.scalapb.descriptors.PString(eventTimeStr)
    }
  }

  override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)

  def companion = parallelai.sot.engine.taps.pubsub.MessageProto
}

object MessageProto extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProto] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProto] = this

  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.engine.taps.pubsub.MessageProto = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    parallelai.sot.engine.taps.pubsub.MessageProto(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap(__fields.get(1)).asInstanceOf[String], __fieldsMap(__fields.get(2)).asInstanceOf[Long], __fieldsMap(__fields.get(3)).asInstanceOf[Long], __fieldsMap(__fields.get(4)).asInstanceOf[String])
  }

  implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.engine.taps.pubsub.MessageProto] = _root_.scalapb.descriptors.Reads({
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      parallelai.sot.engine.taps.pubsub.MessageProto(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).get.as[Long], __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).get.as[Long], __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).get.as[String])
    case _ =>
      throw new RuntimeException("Expected PMessage")
  })

  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(0)

  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(0)

  def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)

  lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty

  def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)

  lazy val defaultInstance = parallelai.sot.engine.taps.pubsub.MessageProto(user = "", teamName = "", score = 0L, eventTime = 0L, eventTimeStr = "")

  implicit class MessageProtoLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProto]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProto](_l) {
    def user: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.user)((c_, f_) => c_.copy(user = f_))

    def teamName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.teamName)((c_, f_) => c_.copy(teamName = f_))

    def score: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.score)((c_, f_) => c_.copy(score = f_))

    def eventTime: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.eventTime)((c_, f_) => c_.copy(eventTime = f_))

    def eventTimeStr: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.eventTimeStr)((c_, f_) => c_.copy(eventTimeStr = f_))
  }

  final val USER_FIELD_NUMBER = 1
  final val TEAMNAME_FIELD_NUMBER = 2
  final val SCORE_FIELD_NUMBER = 3
  final val EVENTTIME_FIELD_NUMBER = 4
  final val EVENTTIMESTR_FIELD_NUMBER = 5
}

object GenProto extends _root_.com.trueaccord.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.com.trueaccord.scalapb.GeneratedFileObject] = Seq(com.trueaccord.scalapb.scalapb.ScalapbProto)
  lazy val messagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq(parallelai.sot.engine.taps.pubsub.MessageProto)
  private lazy val ProtoBytes: Array[Byte] = com.trueaccord.scalapb.Encoding.fromBase64(scala.collection.Seq(
    """CglnZW4ucHJvdG8SBGNvcmUaFXNjYWxhcGIvc2NhbGFwYi5wcm90byKWAQoMTWVzc2FnZVByb3RvEhIKBHVzZXIYASACKAlSB
  HVzZXISGgoIdGVhbU5hbWUYAiACKAlSCHRlYW1OYW1lEhQKBXNjb3JlGAMgAigDUgVzY29yZRIcCglldmVudFRpbWUYBCACKANSC
  WV2ZW50VGltZRIiCgxldmVudFRpbWVTdHIYBSACKAlSDGV2ZW50VGltZVN0ckJSCgxzb3QuZXhlY3V0b3JCCUNvcmVNb2RlbOI/N
  goqcGFyYWxsZWxhaS5zb3QuZXhlY3V0b3IuYnVpbGRlci5TT1RCdWlsZGVyEAAoAUIETGlzdA==""").mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor = {
    val javaProto = com.google.protobuf.DescriptorProtos.FileDescriptorProto.parseFrom(ProtoBytes)
    com.google.protobuf.Descriptors.FileDescriptor.buildFrom(javaProto, Array(com.trueaccord.scalapb.scalapb.ScalapbProto.javaDescriptor))
  }

  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47") def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}

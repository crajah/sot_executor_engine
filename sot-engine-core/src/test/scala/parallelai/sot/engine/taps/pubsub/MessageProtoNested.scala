package parallelai.sot.engine.taps.pubsub

@SerialVersionUID(0L) final case class MessageProtoNested(user: String, teamName: String, score: Long, eventTime: Long, eventTimeStr: String, nestedValue: List[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass] = List.empty) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[MessageProtoNested] with com.trueaccord.lenses.Updatable[MessageProtoNested] {
  @transient private[this] var __serializedSizeCachedValue: Int = 0

  private[this] def __computeSerializedValue(): Int = {
    var __size = 0
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, user)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, teamName)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(3, score)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(4, eventTime)
    __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, eventTimeStr)
    nestedValue.foreach(nestedValue => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(nestedValue.serializedSize) + nestedValue.serializedSize)
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
    nestedValue.foreach { __v =>
      _output__.writeTag(6, 2)
      _output__.writeUInt32NoTag(__v.serializedSize)
      __v.writeTo(_output__)
    }
  }

  def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.engine.taps.pubsub.MessageProtoNested = {
    var __user = this.user
    var __teamName = this.teamName
    var __score = this.score
    var __eventTime = this.eventTime
    var __eventTimeStr = this.eventTimeStr
    val __nestedValue = List.newBuilder[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass] ++= this.nestedValue
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
        case 50 =>
          __nestedValue += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass.defaultInstance)
        case tag =>
          _input__.skipField(tag)
      }
    }
    if (__requiredFields0 != 0L) {
      throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
    }
    parallelai.sot.engine.taps.pubsub.MessageProtoNested(user = __user, teamName = __teamName, score = __score, eventTime = __eventTime, eventTimeStr = __eventTimeStr, nestedValue = __nestedValue.result())
  }

  def withUser(__v: String): MessageProtoNested = copy(user = __v)

  def withTeamName(__v: String): MessageProtoNested = copy(teamName = __v)

  def withScore(__v: Long): MessageProtoNested = copy(score = __v)

  def withEventTime(__v: Long): MessageProtoNested = copy(eventTime = __v)

  def withEventTimeStr(__v: String): MessageProtoNested = copy(eventTimeStr = __v)

  def clearNestedValue = copy(nestedValue = List.empty)

  def addNestedValue(__vs: parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass*): MessageProtoNested = addAllNestedValue(__vs)

  def addAllNestedValue(__vs: TraversableOnce[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]): MessageProtoNested = copy(nestedValue = nestedValue ++ __vs)

  def withNestedValue(__v: List[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]): MessageProtoNested = copy(nestedValue = __v)

  def getFieldByNumber(__fieldNumber: Int): scala.Any = {
    (__fieldNumber: @_root_.scala.unchecked) match {
      case 1 => user
      case 2 => teamName
      case 3 => score
      case 4 => eventTime
      case 5 => eventTimeStr
      case 6 => nestedValue
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
      case 6 =>
        _root_.scalapb.descriptors.PRepeated(nestedValue.map(_.toPMessage)(_root_.scala.collection.breakOut))
    }
  }

  override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)

  def companion = parallelai.sot.engine.taps.pubsub.MessageProtoNested
}

object MessageProtoNested extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProtoNested] {
  implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProtoNested] = this

  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.engine.taps.pubsub.MessageProtoNested = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    parallelai.sot.engine.taps.pubsub.MessageProtoNested(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap(__fields.get(1)).asInstanceOf[String], __fieldsMap(__fields.get(2)).asInstanceOf[Long], __fieldsMap(__fields.get(3)).asInstanceOf[Long], __fieldsMap(__fields.get(4)).asInstanceOf[String], __fieldsMap.getOrElse(__fields.get(5), Nil).asInstanceOf[List[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]])
  }

  implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.engine.taps.pubsub.MessageProtoNested] = _root_.scalapb.descriptors.Reads({
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      parallelai.sot.engine.taps.pubsub.MessageProtoNested(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).get.as[Long], __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).get.as[Long], __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).map(_.as[List[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]]).getOrElse(List.empty))
    case _ =>
      throw new RuntimeException("Expected PMessage")
  })

  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProtoNested.javaDescriptor.getMessageTypes.get(0)

  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProtoNested.scalaDescriptor.messages(0)

  def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 6 =>
        __out = parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass
    }
    __out
  }

  lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass)

  def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)

  lazy val defaultInstance = parallelai.sot.engine.taps.pubsub.MessageProtoNested(user = "", teamName = "", score = 0L, eventTime = 0L, eventTimeStr = "")

  @SerialVersionUID(0L) final case class NestedClass(value: Long) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[NestedClass] with com.trueaccord.lenses.Updatable[NestedClass] {
    @transient private[this] var __serializedSizeCachedValue: Int = 0

    private[this] def __computeSerializedValue(): Int = {
      var __size = 0
      __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(1, value)
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
      _output__.writeInt64(1, value)
    }

    def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass = {
      var __value = this.value
      var __requiredFields0: Long = 1L
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 =>
            _done__ = true
          case 8 =>
            __value = _input__.readInt64()
            __requiredFields0 &= -2L
          case tag =>
            _input__.skipField(tag)
        }
      }
      if (__requiredFields0 != 0L) {
        throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
      }
      parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass(value = __value)
    }

    def withValue(__v: Long): NestedClass = copy(value = __v)

    def getFieldByNumber(__fieldNumber: Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => value
      }
    }

    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 =>
          _root_.scalapb.descriptors.PLong(value)
      }
    }

    override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)

    def companion = parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass
  }

  object NestedClass extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass] {
    implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass] = this

    def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass = {
      require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
      val __fields = javaDescriptor.getFields
      parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass(__fieldsMap(__fields.get(0)).asInstanceOf[Long])
    }

    implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass] = _root_.scalapb.descriptors.Reads({
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
        parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[Long])
      case _ =>
        throw new RuntimeException("Expected PMessage")
    })

    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.engine.taps.pubsub.MessageProtoNested.javaDescriptor.getNestedTypes.get(0)

    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.engine.taps.pubsub.MessageProtoNested.scalaDescriptor.nestedMessages(0)

    def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)

    lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty

    def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)

    lazy val defaultInstance = parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass(value = 0L)

    implicit class NestedClassLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass](_l) {
      def value: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.value)((c_, f_) => c_.copy(value = f_))
    }

    final val VALUE_FIELD_NUMBER = 1
  }

  implicit class MessageProtoNestedLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProtoNested]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.engine.taps.pubsub.MessageProtoNested](_l) {
    def user: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.user)((c_, f_) => c_.copy(user = f_))

    def teamName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.teamName)((c_, f_) => c_.copy(teamName = f_))

    def score: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.score)((c_, f_) => c_.copy(score = f_))

    def eventTime: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.eventTime)((c_, f_) => c_.copy(eventTime = f_))

    def eventTimeStr: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.eventTimeStr)((c_, f_) => c_.copy(eventTimeStr = f_))

    def nestedValue: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.engine.taps.pubsub.MessageProtoNested.NestedClass]] = field(_.nestedValue)((c_, f_) => c_.copy(nestedValue = f_))
  }

  final val USER_FIELD_NUMBER = 1
  final val TEAMNAME_FIELD_NUMBER = 2
  final val SCORE_FIELD_NUMBER = 3
  final val EVENTTIME_FIELD_NUMBER = 4
  final val EVENTTIMESTR_FIELD_NUMBER = 5
  final val NESTEDVALUE_FIELD_NUMBER = 6
}

object GenProtoNested extends _root_.com.trueaccord.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.com.trueaccord.scalapb.GeneratedFileObject] = Seq(com.trueaccord.scalapb.scalapb.ScalapbProto)
  lazy val messagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq(parallelai.sot.engine.taps.pubsub.MessageProtoNested)
  private lazy val ProtoBytes: Array[Byte] = com.trueaccord.scalapb.Encoding.fromBase64(scala.collection.Seq(
    """CglnZW4ucHJvdG8SBGNvcmUaFXNjYWxhcGIvc2NhbGFwYi5wcm90byL9AQoMTWVzc2FnZVByb3RvEhIKBHVzZXIYASACKAlSB
  HVzZXISGgoIdGVhbU5hbWUYAiACKAlSCHRlYW1OYW1lEhQKBXNjb3JlGAMgAigDUgVzY29yZRIcCglldmVudFRpbWUYBCACKANSC
  WV2ZW50VGltZRIiCgxldmVudFRpbWVTdHIYBSACKAlSDGV2ZW50VGltZVN0chJACgtuZXN0ZWRWYWx1ZRgGIAMoCzIeLmNvcmUuT
  WVzc2FnZVByb3RvLk5lc3RlZENsYXNzUgtuZXN0ZWRWYWx1ZRojCgtOZXN0ZWRDbGFzcxIUCgV2YWx1ZRgBIAIoA1IFdmFsdWVCU
  goMc290LmV4ZWN1dG9yQglDb3JlTW9kZWziPzYKKnBhcmFsbGVsYWkuc290LmV4ZWN1dG9yLmJ1aWxkZXIuU09UQnVpbGRlchAAK
  AFCBExpc3Q=""").mkString)
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
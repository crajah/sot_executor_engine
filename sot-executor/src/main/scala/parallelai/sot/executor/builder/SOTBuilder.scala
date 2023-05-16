package parallelai.sot.executor.builder

import java.io.{File, InputStream}
import java.util.TimeZone
import scala.meta.Lit
import parallelai.sot.engine.config.SchemaResourcePath
import com.spotify.scio._
import com.spotify.scio.avro.types.AvroType
import com.spotify.scio.bigquery.BigQueryType
import com.spotify.scio.values.{SCollection, WindowOptions}
import org.apache.beam.sdk.io.gcp.datastore.DatastoreIO
import org.apache.beam.sdk.options.{PipelineOptions, StreamingOptions}
import org.joda.time.{DateTimeZone, Duration, Instant}
import org.joda.time.format.DateTimeFormat
import parallelai.sot.engine.config.gcp.SOTUtils
import parallelai.sot.macros.SOTBuilder
import shapeless._
import syntax.singleton._
import com.google.datastore.v1.{GqlQuery, Query}
import com.spotify.scio.avro.types.AvroType.HasAvroAnnotation
import com.spotify.scio.bigquery.types.BigQueryType.HasAnnotation
import com.spotify.scio.streaming.ACCUMULATING_FIRED_PANES
import com.typesafe.config.ConfigFactory
import org.apache.beam.sdk.extensions.gcp.options.GcpOptions
import org.apache.beam.sdk.transforms.windowing.{AfterProcessingTime, Repeatedly}
import parallelai.sot.executor.model.SOTMacroConfig._
import parallelai.sot.executor.model.SOTMacroJsonConfig
import parallelai.sot.engine.runner.scio.PaiScioContext._
import parallelai.sot.macros.SOTMacroHelper._
import com.trueaccord.scalapb.GeneratedMessage
import parallelai.sot.engine.config.gcp.{SOTOptions, SOTUtils}
import parallelai.sot.engine.serialization.avro.AvroUtils
import parallelai.sot.engine.runner.Reader
import parallelai.sot.engine.runner.Writer
import parallelai.sot.engine.generic.helper.Helper
import parallelai.sot.engine.runner.SCollectionStateMonad._
import parallelai.sot.engine.generic.row.Row
import scalaz.Scalaz.init
import parallelai.sot.engine.io.{SchemalessTapDef, TapDef}
import parallelai.sot.engine.generic.row.Syntax._
import parallelai.sot.engine.generic.row.Nested
import shapeless.record._

// Windowing imports
import org.joda.time.Duration
import com.spotify.scio.values.WindowOptions
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.apache.beam.sdk.transforms.windowing.Window.ClosingBehavior
import org.apache.beam.sdk.transforms.windowing.TimestampCombiner
import org.apache.beam.sdk.transforms.windowing._
/**
  * To run this class with a default configuration of application.conf:
  * <pre>
  *   sbt clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  *
  * If there is no application.conf then compilation will fail, but you can supply your own conf as a Java option e.g. -Dconfig.resource=application-ps2ps-test.conf
  * <pre>
  *   sbt -Dconfig.resource=application-ps2ps-test.conf clean compile "sot-executor/runMain parallelai.sot.executor.builder.SOTBuilder --project=bi-crm-poc --runner=DataflowRunner --region=europe-west1 --zone=europe-west2-a --workerMachineType=n1-standard-1 --diskSizeGb=150 --maxNumWorkers=1 --waitToFinish=false"
  * </pre>
  * NOTE That application configurations can also be set/overridden via system and environment properties.
  */
object SOTBuilder {
  object gen {
    @SerialVersionUID(0L) final case class Batch(uniqueId: String, messages: List[parallelai.sot.executor.builder.SOTBuilder.gen.Activity] = List.empty, monitoring: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Batch] with com.trueaccord.lenses.Updatable[Batch] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, uniqueId)
        messages.foreach(messages => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(messages.serializedSize) + messages.serializedSize)
        if (monitoring.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(monitoring.get.serializedSize) + monitoring.get.serializedSize
        }
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
        _output__.writeString(1, uniqueId)
        messages.foreach { __v =>
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        monitoring.foreach { __v =>
          _output__.writeTag(3, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Batch = {
        var __uniqueId = this.uniqueId
        val __messages = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Activity] ++= this.messages
        var __monitoring = this.monitoring
        var __requiredFields0: Long = 1L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __uniqueId = _input__.readString()
              __requiredFields0 &= -2L
            case 18 =>
              __messages += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Activity.defaultInstance)
            case 26 =>
              __monitoring = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __monitoring.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring.defaultInstance)))
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.Batch(uniqueId = __uniqueId, messages = __messages.result(), monitoring = __monitoring)
      }
      def withUniqueId(__v: String): Batch = copy(uniqueId = __v)
      def clearMessages = copy(messages = List.empty)
      def addMessages(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Activity*): Batch = addAllMessages(__vs)
      def addAllMessages(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Activity]): Batch = copy(messages = messages ++ __vs)
      def withMessages(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Activity]): Batch = copy(messages = __v)
      def getMonitoring: parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring = monitoring.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring.defaultInstance)
      def clearMonitoring: Batch = copy(monitoring = None)
      def withMonitoring(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring): Batch = copy(monitoring = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            uniqueId
          case 2 =>
            messages
          case 3 =>
            monitoring.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PString(uniqueId)
          case 2 =>
            _root_.scalapb.descriptors.PRepeated(messages.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 3 =>
            monitoring.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Batch
    }
    object Batch extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Batch] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Batch] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Batch = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.Batch(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.getOrElse(__fields.get(1), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Activity]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Batch] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.Batch(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Activity]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(0)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(0)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 2 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Activity
          case 3 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Batch(uniqueId = "")
      implicit class BatchLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Batch]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Batch](_l) {
        def uniqueId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.uniqueId)((c_, f_) => c_.copy(uniqueId = f_))
        def messages: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Activity]] = field(_.messages)((c_, f_) => c_.copy(messages = f_))
        def monitoring: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = field(_.getMonitoring)((c_, f_) => c_.copy(monitoring = Some(f_)))
        def optionalMonitoring: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]] = field(_.monitoring)((c_, f_) => c_.copy(monitoring = f_))
      }
      final val UNIQUEID_FIELD_NUMBER = 1
      final val MESSAGES_FIELD_NUMBER = 2
      final val MONITORING_FIELD_NUMBER = 3
    }
    @SerialVersionUID(0L) final case class Activity(header: parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader, state: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetState] = None, supportingState: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Activity] with com.trueaccord.lenses.Updatable[Activity] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(header.serializedSize) + header.serializedSize
        if (state.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(state.get.serializedSize) + state.get.serializedSize
        }
        if (supportingState.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(supportingState.get.serializedSize) + supportingState.get.serializedSize
        }
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
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(header.serializedSize)
        header.writeTo(_output__)
        state.foreach { __v =>
          _output__.writeTag(201, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        supportingState.foreach { __v =>
          _output__.writeTag(211, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Activity = {
        var __header = this.header
        var __state = this.state
        var __supportingState = this.supportingState
        var __requiredFields0: Long = 1L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __header = _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __header)
              __requiredFields0 &= -2L
            case 1610 =>
              __state = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __state.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.BetState.defaultInstance)))
            case 1690 =>
              __supportingState = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __supportingState.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.defaultInstance)))
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.Activity(header = __header, state = __state, supportingState = __supportingState)
      }
      def withHeader(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader): Activity = copy(header = __v)
      def getState: parallelai.sot.executor.builder.SOTBuilder.gen.BetState = state.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.BetState.defaultInstance)
      def clearState: Activity = copy(state = None)
      def withState(__v: parallelai.sot.executor.builder.SOTBuilder.gen.BetState): Activity = copy(state = Some(__v))
      def getSupportingState: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState = supportingState.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.defaultInstance)
      def clearSupportingState: Activity = copy(supportingState = None)
      def withSupportingState(__v: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState): Activity = copy(supportingState = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            header
          case 201 =>
            state.orNull
          case 211 =>
            supportingState.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            header.toPMessage
          case 201 =>
            state.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 211 =>
            supportingState.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Activity
    }
    object Activity extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Activity] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Activity] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Activity = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.Activity(__fieldsMap(__fields.get(0)).asInstanceOf[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetState]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Activity] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.Activity(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader], __fieldsMap.get(scalaDescriptor.findFieldByNumber(201).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetState]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(211).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(1)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(1)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 1 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader
          case 201 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.BetState
          case 211 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader)
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Activity(header = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader.defaultInstance)
      sealed trait CrudType extends _root_.com.trueaccord.scalapb.GeneratedEnum {
        type EnumType = CrudType
        def isCreate: Boolean = false
        def isRead: Boolean = false
        def isUpdate: Boolean = false
        def isDelete: Boolean = false
        def isInsert: Boolean = false
        def isPurge: Boolean = false
        def companion: _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[CrudType] = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType
      }
      object CrudType extends _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[CrudType] {
        implicit def enumCompanion: _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[CrudType] = this
        @SerialVersionUID(0L) case object CREATE extends CrudType {
          val value = 1
          val index = 0
          val name = "CREATE"
          override def isCreate: Boolean = true
        }
        @SerialVersionUID(0L) case object READ extends CrudType {
          val value = 2
          val index = 1
          val name = "READ"
          override def isRead: Boolean = true
        }
        @SerialVersionUID(0L) case object UPDATE extends CrudType {
          val value = 3
          val index = 2
          val name = "UPDATE"
          override def isUpdate: Boolean = true
        }
        @SerialVersionUID(0L) case object DELETE extends CrudType {
          val value = 4
          val index = 3
          val name = "DELETE"
          override def isDelete: Boolean = true
        }
        @SerialVersionUID(0L) case object INSERT extends CrudType {
          val value = 5
          val index = 4
          val name = "INSERT"
          override def isInsert: Boolean = true
        }
        @SerialVersionUID(0L) case object PURGE extends CrudType {
          val value = 6
          val index = 5
          val name = "PURGE"
          override def isPurge: Boolean = true
        }
        @SerialVersionUID(0L) case class Unrecognized(value: Int) extends CrudType with _root_.com.trueaccord.scalapb.UnrecognizedEnum
        lazy val values = scala.collection.Seq(CREATE, READ, UPDATE, DELETE, INSERT, PURGE)
        def fromValue(value: Int): CrudType = value match {
          case 1 =>
            CREATE
          case 2 =>
            READ
          case 3 =>
            UPDATE
          case 4 =>
            DELETE
          case 5 =>
            INSERT
          case 6 =>
            PURGE
          case __other =>
            Unrecognized(__other)
        }
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.EnumDescriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.javaDescriptor.getEnumTypes.get(0)
        def scalaDescriptor: _root_.scalapb.descriptors.EnumDescriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.scalaDescriptor.enums(0)
      }
      @SerialVersionUID(0L) final case class ActivityHeader(activityId: Long, timeStamp: scala.Option[Long] = None, crudType: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType] = None, sequencingKey: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey] = None, queueId: scala.Option[Long] = None, payload: scala.Option[_root_.com.google.protobuf.ByteString] = None, uniqueId: scala.Option[String] = None, activityDescriptor: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor] = None, firstSentTime: scala.Option[Long] = None, sendRetries: scala.Option[Int] = None, contextRef: scala.Option[String] = None, publisherRef: scala.Option[String] = None, operatorRef: scala.Option[String] = None, operatorName: scala.Option[String] = None, monitoring: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = None, correlationId: scala.Option[Long] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[ActivityHeader] with com.trueaccord.lenses.Updatable[ActivityHeader] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(1, activityId)
          if (timeStamp.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(2, timeStamp.get)
          }
          if (crudType.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeEnumSize(3, crudType.get.value)
          }
          if (sequencingKey.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(sequencingKey.get.serializedSize) + sequencingKey.get.serializedSize
          }
          if (queueId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(5, queueId.get)
          }
          if (payload.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeBytesSize(6, payload.get)
          }
          if (uniqueId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(7, uniqueId.get)
          }
          if (activityDescriptor.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(activityDescriptor.get.serializedSize) + activityDescriptor.get.serializedSize
          }
          if (firstSentTime.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(9, firstSentTime.get)
          }
          if (sendRetries.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(10, sendRetries.get)
          }
          if (contextRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(11, contextRef.get)
          }
          if (publisherRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(12, publisherRef.get)
          }
          if (operatorRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(14, operatorRef.get)
          }
          if (operatorName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(16, operatorName.get)
          }
          if (monitoring.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(monitoring.get.serializedSize) + monitoring.get.serializedSize
          }
          if (correlationId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(15, correlationId.get)
          }
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
          _output__.writeInt64(1, activityId)
          timeStamp.foreach {
            __v => _output__.writeInt64(2, __v)
          }
          crudType.foreach {
            __v => _output__.writeEnum(3, __v.value)
          }
          sequencingKey.foreach { __v =>
            _output__.writeTag(4, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          queueId.foreach {
            __v => _output__.writeInt64(5, __v)
          }
          payload.foreach {
            __v => _output__.writeBytes(6, __v)
          }
          uniqueId.foreach {
            __v => _output__.writeString(7, __v)
          }
          activityDescriptor.foreach { __v =>
            _output__.writeTag(8, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          firstSentTime.foreach {
            __v => _output__.writeInt64(9, __v)
          }
          sendRetries.foreach {
            __v => _output__.writeInt32(10, __v)
          }
          contextRef.foreach {
            __v => _output__.writeString(11, __v)
          }
          publisherRef.foreach {
            __v => _output__.writeString(12, __v)
          }
          monitoring.foreach { __v =>
            _output__.writeTag(13, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          operatorRef.foreach {
            __v => _output__.writeString(14, __v)
          }
          correlationId.foreach {
            __v => _output__.writeInt64(15, __v)
          }
          operatorName.foreach {
            __v => _output__.writeString(16, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader = {
          var __activityId = this.activityId
          var __timeStamp = this.timeStamp
          var __crudType = this.crudType
          var __sequencingKey = this.sequencingKey
          var __queueId = this.queueId
          var __payload = this.payload
          var __uniqueId = this.uniqueId
          var __activityDescriptor = this.activityDescriptor
          var __firstSentTime = this.firstSentTime
          var __sendRetries = this.sendRetries
          var __contextRef = this.contextRef
          var __publisherRef = this.publisherRef
          var __operatorRef = this.operatorRef
          var __operatorName = this.operatorName
          var __monitoring = this.monitoring
          var __correlationId = this.correlationId
          var __requiredFields0: Long = 1L
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 8 =>
                __activityId = _input__.readInt64()
                __requiredFields0 &= -2L
              case 16 =>
                __timeStamp = Some(_input__.readInt64())
              case 24 =>
                __crudType = Some(parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType.fromValue(_input__.readEnum()))
              case 34 =>
                __sequencingKey = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __sequencingKey.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey.defaultInstance)))
              case 40 =>
                __queueId = Some(_input__.readInt64())
              case 50 =>
                __payload = Some(_input__.readBytes())
              case 58 =>
                __uniqueId = Some(_input__.readString())
              case 66 =>
                __activityDescriptor = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __activityDescriptor.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor.defaultInstance)))
              case 72 =>
                __firstSentTime = Some(_input__.readInt64())
              case 80 =>
                __sendRetries = Some(_input__.readInt32())
              case 90 =>
                __contextRef = Some(_input__.readString())
              case 98 =>
                __publisherRef = Some(_input__.readString())
              case 114 =>
                __operatorRef = Some(_input__.readString())
              case 130 =>
                __operatorName = Some(_input__.readString())
              case 106 =>
                __monitoring = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __monitoring.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring.defaultInstance)))
              case 120 =>
                __correlationId = Some(_input__.readInt64())
              case tag =>
                _input__.skipField(tag)
            }
          }
          if (__requiredFields0 != 0L) {
            throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader(activityId = __activityId, timeStamp = __timeStamp, crudType = __crudType, sequencingKey = __sequencingKey, queueId = __queueId, payload = __payload, uniqueId = __uniqueId, activityDescriptor = __activityDescriptor, firstSentTime = __firstSentTime, sendRetries = __sendRetries, contextRef = __contextRef, publisherRef = __publisherRef, operatorRef = __operatorRef, operatorName = __operatorName, monitoring = __monitoring, correlationId = __correlationId)
        }
        def withActivityId(__v: Long): ActivityHeader = copy(activityId = __v)
        def getTimeStamp: Long = timeStamp.getOrElse(0L)
        def clearTimeStamp: ActivityHeader = copy(timeStamp = None)
        def withTimeStamp(__v: Long): ActivityHeader = copy(timeStamp = Some(__v))
        def getCrudType: parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType = crudType.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType.CREATE)
        def clearCrudType: ActivityHeader = copy(crudType = None)
        def withCrudType(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType): ActivityHeader = copy(crudType = Some(__v))
        def getSequencingKey: parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey = sequencingKey.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey.defaultInstance)
        def clearSequencingKey: ActivityHeader = copy(sequencingKey = None)
        def withSequencingKey(__v: parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey): ActivityHeader = copy(sequencingKey = Some(__v))
        def getQueueId: Long = queueId.getOrElse(0L)
        def clearQueueId: ActivityHeader = copy(queueId = None)
        def withQueueId(__v: Long): ActivityHeader = copy(queueId = Some(__v))
        def getPayload: _root_.com.google.protobuf.ByteString = payload.getOrElse(_root_.com.google.protobuf.ByteString.EMPTY)
        def clearPayload: ActivityHeader = copy(payload = None)
        def withPayload(__v: _root_.com.google.protobuf.ByteString): ActivityHeader = copy(payload = Some(__v))
        def getUniqueId: String = uniqueId.getOrElse("")
        def clearUniqueId: ActivityHeader = copy(uniqueId = None)
        def withUniqueId(__v: String): ActivityHeader = copy(uniqueId = Some(__v))
        def getActivityDescriptor: parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor = activityDescriptor.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor.defaultInstance)
        def clearActivityDescriptor: ActivityHeader = copy(activityDescriptor = None)
        def withActivityDescriptor(__v: parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor): ActivityHeader = copy(activityDescriptor = Some(__v))
        def getFirstSentTime: Long = firstSentTime.getOrElse(0L)
        def clearFirstSentTime: ActivityHeader = copy(firstSentTime = None)
        def withFirstSentTime(__v: Long): ActivityHeader = copy(firstSentTime = Some(__v))
        def getSendRetries: Int = sendRetries.getOrElse(0)
        def clearSendRetries: ActivityHeader = copy(sendRetries = None)
        def withSendRetries(__v: Int): ActivityHeader = copy(sendRetries = Some(__v))
        def getContextRef: String = contextRef.getOrElse("")
        def clearContextRef: ActivityHeader = copy(contextRef = None)
        def withContextRef(__v: String): ActivityHeader = copy(contextRef = Some(__v))
        def getPublisherRef: String = publisherRef.getOrElse("")
        def clearPublisherRef: ActivityHeader = copy(publisherRef = None)
        def withPublisherRef(__v: String): ActivityHeader = copy(publisherRef = Some(__v))
        def getOperatorRef: String = operatorRef.getOrElse("")
        def clearOperatorRef: ActivityHeader = copy(operatorRef = None)
        def withOperatorRef(__v: String): ActivityHeader = copy(operatorRef = Some(__v))
        def getOperatorName: String = operatorName.getOrElse("")
        def clearOperatorName: ActivityHeader = copy(operatorName = None)
        def withOperatorName(__v: String): ActivityHeader = copy(operatorName = Some(__v))
        def getMonitoring: parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring = monitoring.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring.defaultInstance)
        def clearMonitoring: ActivityHeader = copy(monitoring = None)
        def withMonitoring(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring): ActivityHeader = copy(monitoring = Some(__v))
        def getCorrelationId: Long = correlationId.getOrElse(0L)
        def clearCorrelationId: ActivityHeader = copy(correlationId = None)
        def withCorrelationId(__v: Long): ActivityHeader = copy(correlationId = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              activityId
            case 2 =>
              timeStamp.orNull
            case 3 =>
              crudType.map(_.javaValueDescriptor).orNull
            case 4 =>
              sequencingKey.orNull
            case 5 =>
              queueId.orNull
            case 6 =>
              payload.orNull
            case 7 =>
              uniqueId.orNull
            case 8 =>
              activityDescriptor.orNull
            case 9 =>
              firstSentTime.orNull
            case 10 =>
              sendRetries.orNull
            case 11 =>
              contextRef.orNull
            case 12 =>
              publisherRef.orNull
            case 14 =>
              operatorRef.orNull
            case 16 =>
              operatorName.orNull
            case 13 =>
              monitoring.orNull
            case 15 =>
              correlationId.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              _root_.scalapb.descriptors.PLong(activityId)
            case 2 =>
              timeStamp.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              crudType.map(__e => _root_.scalapb.descriptors.PEnum(__e.scalaValueDescriptor)).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              sequencingKey.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 5 =>
              queueId.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 6 =>
              payload.map(_root_.scalapb.descriptors.PByteString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 7 =>
              uniqueId.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 8 =>
              activityDescriptor.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 9 =>
              firstSentTime.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 10 =>
              sendRetries.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 11 =>
              contextRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 12 =>
              publisherRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 14 =>
              operatorRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 16 =>
              operatorName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 13 =>
              monitoring.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 15 =>
              correlationId.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader
      }
      object ActivityHeader extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader(__fieldsMap(__fields.get(0)).asInstanceOf[Long], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[_root_.com.google.protobuf.Descriptors.EnumValueDescriptor]].map(__e => parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType.fromValue(__e.getNumber)), __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[_root_.com.google.protobuf.ByteString]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(7)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor]], __fieldsMap.get(__fields.get(8)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(9)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(10)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(11)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(12)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(13)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(14)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]], __fieldsMap.get(__fields.get(15)).asInstanceOf[scala.Option[Long]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[Long], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[_root_.scalapb.descriptors.EnumValueDescriptor]]).map(__e => parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType.fromValue(__e.number)), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[_root_.com.google.protobuf.ByteString]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(10).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(11).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(12).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(14).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(16).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(13).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(15).get).flatMap(_.as[scala.Option[Long]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.javaDescriptor.getNestedTypes.get(0)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.scalaDescriptor.nestedMessages(0)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
          var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
          (__number: @_root_.scala.unchecked) match {
            case 4 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey
            case 8 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor
            case 13 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring
          }
          __out
        }
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 3 =>
              parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType
          }
        }
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader(activityId = 0L)
        implicit class ActivityHeaderLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader](_l) {
          def activityId: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.activityId)((c_, f_) => c_.copy(activityId = f_))
          def timeStamp: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getTimeStamp)((c_, f_) => c_.copy(timeStamp = Some(f_)))
          def optionalTimeStamp: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.timeStamp)((c_, f_) => c_.copy(timeStamp = f_))
          def crudType: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType] = field(_.getCrudType)((c_, f_) => c_.copy(crudType = Some(f_)))
          def optionalCrudType: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Activity.CrudType]] = field(_.crudType)((c_, f_) => c_.copy(crudType = f_))
          def sequencingKey: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey] = field(_.getSequencingKey)((c_, f_) => c_.copy(sequencingKey = Some(f_)))
          def optionalSequencingKey: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey]] = field(_.sequencingKey)((c_, f_) => c_.copy(sequencingKey = f_))
          def queueId: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getQueueId)((c_, f_) => c_.copy(queueId = Some(f_)))
          def optionalQueueId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.queueId)((c_, f_) => c_.copy(queueId = f_))
          def payload: _root_.com.trueaccord.lenses.Lens[UpperPB, _root_.com.google.protobuf.ByteString] = field(_.getPayload)((c_, f_) => c_.copy(payload = Some(f_)))
          def optionalPayload: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[_root_.com.google.protobuf.ByteString]] = field(_.payload)((c_, f_) => c_.copy(payload = f_))
          def uniqueId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getUniqueId)((c_, f_) => c_.copy(uniqueId = Some(f_)))
          def optionalUniqueId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.uniqueId)((c_, f_) => c_.copy(uniqueId = f_))
          def activityDescriptor: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor] = field(_.getActivityDescriptor)((c_, f_) => c_.copy(activityDescriptor = Some(f_)))
          def optionalActivityDescriptor: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor]] = field(_.activityDescriptor)((c_, f_) => c_.copy(activityDescriptor = f_))
          def firstSentTime: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getFirstSentTime)((c_, f_) => c_.copy(firstSentTime = Some(f_)))
          def optionalFirstSentTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.firstSentTime)((c_, f_) => c_.copy(firstSentTime = f_))
          def sendRetries: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getSendRetries)((c_, f_) => c_.copy(sendRetries = Some(f_)))
          def optionalSendRetries: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.sendRetries)((c_, f_) => c_.copy(sendRetries = f_))
          def contextRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getContextRef)((c_, f_) => c_.copy(contextRef = Some(f_)))
          def optionalContextRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.contextRef)((c_, f_) => c_.copy(contextRef = f_))
          def publisherRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPublisherRef)((c_, f_) => c_.copy(publisherRef = Some(f_)))
          def optionalPublisherRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.publisherRef)((c_, f_) => c_.copy(publisherRef = f_))
          def operatorRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOperatorRef)((c_, f_) => c_.copy(operatorRef = Some(f_)))
          def optionalOperatorRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.operatorRef)((c_, f_) => c_.copy(operatorRef = f_))
          def operatorName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOperatorName)((c_, f_) => c_.copy(operatorName = Some(f_)))
          def optionalOperatorName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.operatorName)((c_, f_) => c_.copy(operatorName = f_))
          def monitoring: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = field(_.getMonitoring)((c_, f_) => c_.copy(monitoring = Some(f_)))
          def optionalMonitoring: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]] = field(_.monitoring)((c_, f_) => c_.copy(monitoring = f_))
          def correlationId: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getCorrelationId)((c_, f_) => c_.copy(correlationId = Some(f_)))
          def optionalCorrelationId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.correlationId)((c_, f_) => c_.copy(correlationId = f_))
        }
        final val ACTIVITYID_FIELD_NUMBER = 1
        final val TIMESTAMP_FIELD_NUMBER = 2
        final val CRUDTYPE_FIELD_NUMBER = 3
        final val SEQUENCINGKEY_FIELD_NUMBER = 4
        final val QUEUEID_FIELD_NUMBER = 5
        final val PAYLOAD_FIELD_NUMBER = 6
        final val UNIQUEID_FIELD_NUMBER = 7
        final val ACTIVITYDESCRIPTOR_FIELD_NUMBER = 8
        final val FIRSTSENTTIME_FIELD_NUMBER = 9
        final val SENDRETRIES_FIELD_NUMBER = 10
        final val CONTEXTREF_FIELD_NUMBER = 11
        final val PUBLISHERREF_FIELD_NUMBER = 12
        final val OPERATORREF_FIELD_NUMBER = 14
        final val OPERATORNAME_FIELD_NUMBER = 16
        final val MONITORING_FIELD_NUMBER = 13
        final val CORRELATIONID_FIELD_NUMBER = 15
      }
      implicit class ActivityLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity](_l) {
        def header: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Activity.ActivityHeader] = field(_.header)((c_, f_) => c_.copy(header = f_))
        def state: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetState] = field(_.getState)((c_, f_) => c_.copy(state = Some(f_)))
        def optionalState: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetState]] = field(_.state)((c_, f_) => c_.copy(state = f_))
        def supportingState: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState] = field(_.getSupportingState)((c_, f_) => c_.copy(supportingState = Some(f_)))
        def optionalSupportingState: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState]] = field(_.supportingState)((c_, f_) => c_.copy(supportingState = f_))
      }
      final val HEADER_FIELD_NUMBER = 1
      final val STATE_FIELD_NUMBER = 201
      final val SUPPORTINGSTATE_FIELD_NUMBER = 211
    }
    @SerialVersionUID(0L) final case class SequencingKey(sequencingDescriptor: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor] = None, sequencingId: scala.Option[Int] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[SequencingKey] with com.trueaccord.lenses.Updatable[SequencingKey] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        if (sequencingDescriptor.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(sequencingDescriptor.get.serializedSize) + sequencingDescriptor.get.serializedSize
        }
        if (sequencingId.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, sequencingId.get)
        }
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
        sequencingDescriptor.foreach { __v =>
          _output__.writeTag(1, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        sequencingId.foreach {
          __v => _output__.writeInt32(2, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey = {
        var __sequencingDescriptor = this.sequencingDescriptor
        var __sequencingId = this.sequencingId
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __sequencingDescriptor = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __sequencingDescriptor.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor.defaultInstance)))
            case 16 =>
              __sequencingId = Some(_input__.readInt32())
            case tag =>
              _input__.skipField(tag)
          }
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey(sequencingDescriptor = __sequencingDescriptor, sequencingId = __sequencingId)
      }
      def getSequencingDescriptor: parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor = sequencingDescriptor.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor.defaultInstance)
      def clearSequencingDescriptor: SequencingKey = copy(sequencingDescriptor = None)
      def withSequencingDescriptor(__v: parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor): SequencingKey = copy(sequencingDescriptor = Some(__v))
      def getSequencingId: Int = sequencingId.getOrElse(0)
      def clearSequencingId: SequencingKey = copy(sequencingId = None)
      def withSequencingId(__v: Int): SequencingKey = copy(sequencingId = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            sequencingDescriptor.orNull
          case 2 =>
            sequencingId.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            sequencingDescriptor.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 2 =>
            sequencingId.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey
    }
    object SequencingKey extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(2)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(2)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 1 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey()
      implicit class SequencingKeyLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey](_l) {
        def sequencingDescriptor: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor] = field(_.getSequencingDescriptor)((c_, f_) => c_.copy(sequencingDescriptor = Some(f_)))
        def optionalSequencingDescriptor: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor]] = field(_.sequencingDescriptor)((c_, f_) => c_.copy(sequencingDescriptor = f_))
        def sequencingId: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getSequencingId)((c_, f_) => c_.copy(sequencingId = Some(f_)))
        def optionalSequencingId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.sequencingId)((c_, f_) => c_.copy(sequencingId = f_))
      }
      final val SEQUENCINGDESCRIPTOR_FIELD_NUMBER = 1
      final val SEQUENCINGID_FIELD_NUMBER = 2
    }
    @SerialVersionUID(0L) final case class ActivityDescriptor(id: Int, name: String, description: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[ActivityDescriptor] with com.trueaccord.lenses.Updatable[ActivityDescriptor] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, id)
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, name)
        if (description.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, description.get)
        }
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
        _output__.writeInt32(1, id)
        _output__.writeString(2, name)
        description.foreach {
          __v => _output__.writeString(3, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor = {
        var __id = this.id
        var __name = this.name
        var __description = this.description
        var __requiredFields0: Long = 3L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 8 =>
              __id = _input__.readInt32()
              __requiredFields0 &= -2L
            case 18 =>
              __name = _input__.readString()
              __requiredFields0 &= -3L
            case 26 =>
              __description = Some(_input__.readString())
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor(id = __id, name = __name, description = __description)
      }
      def withId(__v: Int): ActivityDescriptor = copy(id = __v)
      def withName(__v: String): ActivityDescriptor = copy(name = __v)
      def getDescription: String = description.getOrElse("")
      def clearDescription: ActivityDescriptor = copy(description = None)
      def withDescription(__v: String): ActivityDescriptor = copy(description = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            id
          case 2 =>
            name
          case 3 =>
            description.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PInt(id)
          case 2 =>
            _root_.scalapb.descriptors.PString(name)
          case 3 =>
            description.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor
    }
    object ActivityDescriptor extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor(__fieldsMap(__fields.get(0)).asInstanceOf[Int], __fieldsMap(__fields.get(1)).asInstanceOf[String], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[Int], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(3)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(3)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor(id = 0, name = "")
      implicit class ActivityDescriptorLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor](_l) {
        def id: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.id)((c_, f_) => c_.copy(id = f_))
        def name: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.name)((c_, f_) => c_.copy(name = f_))
        def description: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getDescription)((c_, f_) => c_.copy(description = Some(f_)))
        def optionalDescription: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.description)((c_, f_) => c_.copy(description = f_))
      }
      final val ID_FIELD_NUMBER = 1
      final val NAME_FIELD_NUMBER = 2
      final val DESCRIPTION_FIELD_NUMBER = 3
    }
    @SerialVersionUID(0L) final case class SequencingDescriptor(id: Int, name: String, description: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[SequencingDescriptor] with com.trueaccord.lenses.Updatable[SequencingDescriptor] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, id)
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, name)
        if (description.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, description.get)
        }
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
        _output__.writeInt32(1, id)
        _output__.writeString(2, name)
        description.foreach {
          __v => _output__.writeString(3, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor = {
        var __id = this.id
        var __name = this.name
        var __description = this.description
        var __requiredFields0: Long = 3L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 8 =>
              __id = _input__.readInt32()
              __requiredFields0 &= -2L
            case 18 =>
              __name = _input__.readString()
              __requiredFields0 &= -3L
            case 26 =>
              __description = Some(_input__.readString())
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor(id = __id, name = __name, description = __description)
      }
      def withId(__v: Int): SequencingDescriptor = copy(id = __v)
      def withName(__v: String): SequencingDescriptor = copy(name = __v)
      def getDescription: String = description.getOrElse("")
      def clearDescription: SequencingDescriptor = copy(description = None)
      def withDescription(__v: String): SequencingDescriptor = copy(description = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            id
          case 2 =>
            name
          case 3 =>
            description.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PInt(id)
          case 2 =>
            _root_.scalapb.descriptors.PString(name)
          case 3 =>
            description.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor
    }
    object SequencingDescriptor extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor(__fieldsMap(__fields.get(0)).asInstanceOf[Int], __fieldsMap(__fields.get(1)).asInstanceOf[String], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[Int], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(4)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(4)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor(id = 0, name = "")
      implicit class SequencingDescriptorLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor](_l) {
        def id: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.id)((c_, f_) => c_.copy(id = f_))
        def name: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.name)((c_, f_) => c_.copy(name = f_))
        def description: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getDescription)((c_, f_) => c_.copy(description = Some(f_)))
        def optionalDescription: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.description)((c_, f_) => c_.copy(description = f_))
      }
      final val ID_FIELD_NUMBER = 1
      final val NAME_FIELD_NUMBER = 2
      final val DESCRIPTION_FIELD_NUMBER = 3
    }
    @SerialVersionUID(0L) final case class Monitoring(initializeAtCollector: scala.Option[Long] = None, beforeSubmitToComposer: scala.Option[Long] = None, startComposerQuery: scala.Option[Long] = None, endComposerQuery: scala.Option[Long] = None, orchestratorReceive: scala.Option[Long] = None, orchestratorSend: scala.Option[Long] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Monitoring] with com.trueaccord.lenses.Updatable[Monitoring] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        if (initializeAtCollector.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(1, initializeAtCollector.get)
        }
        if (beforeSubmitToComposer.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(2, beforeSubmitToComposer.get)
        }
        if (startComposerQuery.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(3, startComposerQuery.get)
        }
        if (endComposerQuery.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(4, endComposerQuery.get)
        }
        if (orchestratorReceive.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(5, orchestratorReceive.get)
        }
        if (orchestratorSend.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeInt64Size(6, orchestratorSend.get)
        }
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
        initializeAtCollector.foreach {
          __v => _output__.writeInt64(1, __v)
        }
        beforeSubmitToComposer.foreach {
          __v => _output__.writeInt64(2, __v)
        }
        startComposerQuery.foreach {
          __v => _output__.writeInt64(3, __v)
        }
        endComposerQuery.foreach {
          __v => _output__.writeInt64(4, __v)
        }
        orchestratorReceive.foreach {
          __v => _output__.writeInt64(5, __v)
        }
        orchestratorSend.foreach {
          __v => _output__.writeInt64(6, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring = {
        var __initializeAtCollector = this.initializeAtCollector
        var __beforeSubmitToComposer = this.beforeSubmitToComposer
        var __startComposerQuery = this.startComposerQuery
        var __endComposerQuery = this.endComposerQuery
        var __orchestratorReceive = this.orchestratorReceive
        var __orchestratorSend = this.orchestratorSend
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 8 =>
              __initializeAtCollector = Some(_input__.readInt64())
            case 16 =>
              __beforeSubmitToComposer = Some(_input__.readInt64())
            case 24 =>
              __startComposerQuery = Some(_input__.readInt64())
            case 32 =>
              __endComposerQuery = Some(_input__.readInt64())
            case 40 =>
              __orchestratorReceive = Some(_input__.readInt64())
            case 48 =>
              __orchestratorSend = Some(_input__.readInt64())
            case tag =>
              _input__.skipField(tag)
          }
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring(initializeAtCollector = __initializeAtCollector, beforeSubmitToComposer = __beforeSubmitToComposer, startComposerQuery = __startComposerQuery, endComposerQuery = __endComposerQuery, orchestratorReceive = __orchestratorReceive, orchestratorSend = __orchestratorSend)
      }
      def getInitializeAtCollector: Long = initializeAtCollector.getOrElse(0L)
      def clearInitializeAtCollector: Monitoring = copy(initializeAtCollector = None)
      def withInitializeAtCollector(__v: Long): Monitoring = copy(initializeAtCollector = Some(__v))
      def getBeforeSubmitToComposer: Long = beforeSubmitToComposer.getOrElse(0L)
      def clearBeforeSubmitToComposer: Monitoring = copy(beforeSubmitToComposer = None)
      def withBeforeSubmitToComposer(__v: Long): Monitoring = copy(beforeSubmitToComposer = Some(__v))
      def getStartComposerQuery: Long = startComposerQuery.getOrElse(0L)
      def clearStartComposerQuery: Monitoring = copy(startComposerQuery = None)
      def withStartComposerQuery(__v: Long): Monitoring = copy(startComposerQuery = Some(__v))
      def getEndComposerQuery: Long = endComposerQuery.getOrElse(0L)
      def clearEndComposerQuery: Monitoring = copy(endComposerQuery = None)
      def withEndComposerQuery(__v: Long): Monitoring = copy(endComposerQuery = Some(__v))
      def getOrchestratorReceive: Long = orchestratorReceive.getOrElse(0L)
      def clearOrchestratorReceive: Monitoring = copy(orchestratorReceive = None)
      def withOrchestratorReceive(__v: Long): Monitoring = copy(orchestratorReceive = Some(__v))
      def getOrchestratorSend: Long = orchestratorSend.getOrElse(0L)
      def clearOrchestratorSend: Monitoring = copy(orchestratorSend = None)
      def withOrchestratorSend(__v: Long): Monitoring = copy(orchestratorSend = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            initializeAtCollector.orNull
          case 2 =>
            beforeSubmitToComposer.orNull
          case 3 =>
            startComposerQuery.orNull
          case 4 =>
            endComposerQuery.orNull
          case 5 =>
            orchestratorReceive.orNull
          case 6 =>
            orchestratorSend.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            initializeAtCollector.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 2 =>
            beforeSubmitToComposer.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 3 =>
            startComposerQuery.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 4 =>
            endComposerQuery.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 5 =>
            orchestratorReceive.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 6 =>
            orchestratorSend.map(_root_.scalapb.descriptors.PLong).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring
    }
    object Monitoring extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[Long]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[Long]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[Long]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[Long]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(5)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(5)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring()
      implicit class MonitoringLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring](_l) {
        def initializeAtCollector: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getInitializeAtCollector)((c_, f_) => c_.copy(initializeAtCollector = Some(f_)))
        def optionalInitializeAtCollector: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.initializeAtCollector)((c_, f_) => c_.copy(initializeAtCollector = f_))
        def beforeSubmitToComposer: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getBeforeSubmitToComposer)((c_, f_) => c_.copy(beforeSubmitToComposer = Some(f_)))
        def optionalBeforeSubmitToComposer: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.beforeSubmitToComposer)((c_, f_) => c_.copy(beforeSubmitToComposer = f_))
        def startComposerQuery: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getStartComposerQuery)((c_, f_) => c_.copy(startComposerQuery = Some(f_)))
        def optionalStartComposerQuery: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.startComposerQuery)((c_, f_) => c_.copy(startComposerQuery = f_))
        def endComposerQuery: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getEndComposerQuery)((c_, f_) => c_.copy(endComposerQuery = Some(f_)))
        def optionalEndComposerQuery: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.endComposerQuery)((c_, f_) => c_.copy(endComposerQuery = f_))
        def orchestratorReceive: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getOrchestratorReceive)((c_, f_) => c_.copy(orchestratorReceive = Some(f_)))
        def optionalOrchestratorReceive: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.orchestratorReceive)((c_, f_) => c_.copy(orchestratorReceive = f_))
        def orchestratorSend: _root_.com.trueaccord.lenses.Lens[UpperPB, Long] = field(_.getOrchestratorSend)((c_, f_) => c_.copy(orchestratorSend = Some(f_)))
        def optionalOrchestratorSend: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Long]] = field(_.orchestratorSend)((c_, f_) => c_.copy(orchestratorSend = f_))
      }
      final val INITIALIZEATCOLLECTOR_FIELD_NUMBER = 1
      final val BEFORESUBMITTOCOMPOSER_FIELD_NUMBER = 2
      final val STARTCOMPOSERQUERY_FIELD_NUMBER = 3
      final val ENDCOMPOSERQUERY_FIELD_NUMBER = 4
      final val ORCHESTRATORRECEIVE_FIELD_NUMBER = 5
      final val ORCHESTRATORSEND_FIELD_NUMBER = 6
    }
    @SerialVersionUID(0L) final case class Source(channelRef: scala.Option[String] = None, siteRef: scala.Option[String] = None, jurisdictionRef: scala.Option[String] = None, productRef: scala.Option[String] = None, platformRef: scala.Option[String] = None, deviceRef: scala.Option[String] = None, ipAddr: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Source] with com.trueaccord.lenses.Updatable[Source] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        if (channelRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, channelRef.get)
        }
        if (siteRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, siteRef.get)
        }
        if (jurisdictionRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, jurisdictionRef.get)
        }
        if (productRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, productRef.get)
        }
        if (platformRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, platformRef.get)
        }
        if (deviceRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, deviceRef.get)
        }
        if (ipAddr.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(7, ipAddr.get)
        }
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
        channelRef.foreach {
          __v => _output__.writeString(1, __v)
        }
        siteRef.foreach {
          __v => _output__.writeString(2, __v)
        }
        jurisdictionRef.foreach {
          __v => _output__.writeString(3, __v)
        }
        productRef.foreach {
          __v => _output__.writeString(4, __v)
        }
        platformRef.foreach {
          __v => _output__.writeString(5, __v)
        }
        deviceRef.foreach {
          __v => _output__.writeString(6, __v)
        }
        ipAddr.foreach {
          __v => _output__.writeString(7, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Source = {
        var __channelRef = this.channelRef
        var __siteRef = this.siteRef
        var __jurisdictionRef = this.jurisdictionRef
        var __productRef = this.productRef
        var __platformRef = this.platformRef
        var __deviceRef = this.deviceRef
        var __ipAddr = this.ipAddr
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __channelRef = Some(_input__.readString())
            case 18 =>
              __siteRef = Some(_input__.readString())
            case 26 =>
              __jurisdictionRef = Some(_input__.readString())
            case 34 =>
              __productRef = Some(_input__.readString())
            case 42 =>
              __platformRef = Some(_input__.readString())
            case 50 =>
              __deviceRef = Some(_input__.readString())
            case 58 =>
              __ipAddr = Some(_input__.readString())
            case tag =>
              _input__.skipField(tag)
          }
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.Source(channelRef = __channelRef, siteRef = __siteRef, jurisdictionRef = __jurisdictionRef, productRef = __productRef, platformRef = __platformRef, deviceRef = __deviceRef, ipAddr = __ipAddr)
      }
      def getChannelRef: String = channelRef.getOrElse("")
      def clearChannelRef: Source = copy(channelRef = None)
      def withChannelRef(__v: String): Source = copy(channelRef = Some(__v))
      def getSiteRef: String = siteRef.getOrElse("")
      def clearSiteRef: Source = copy(siteRef = None)
      def withSiteRef(__v: String): Source = copy(siteRef = Some(__v))
      def getJurisdictionRef: String = jurisdictionRef.getOrElse("")
      def clearJurisdictionRef: Source = copy(jurisdictionRef = None)
      def withJurisdictionRef(__v: String): Source = copy(jurisdictionRef = Some(__v))
      def getProductRef: String = productRef.getOrElse("")
      def clearProductRef: Source = copy(productRef = None)
      def withProductRef(__v: String): Source = copy(productRef = Some(__v))
      def getPlatformRef: String = platformRef.getOrElse("")
      def clearPlatformRef: Source = copy(platformRef = None)
      def withPlatformRef(__v: String): Source = copy(platformRef = Some(__v))
      def getDeviceRef: String = deviceRef.getOrElse("")
      def clearDeviceRef: Source = copy(deviceRef = None)
      def withDeviceRef(__v: String): Source = copy(deviceRef = Some(__v))
      def getIpAddr: String = ipAddr.getOrElse("")
      def clearIpAddr: Source = copy(ipAddr = None)
      def withIpAddr(__v: String): Source = copy(ipAddr = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            channelRef.orNull
          case 2 =>
            siteRef.orNull
          case 3 =>
            jurisdictionRef.orNull
          case 4 =>
            productRef.orNull
          case 5 =>
            platformRef.orNull
          case 6 =>
            deviceRef.orNull
          case 7 =>
            ipAddr.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            channelRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 2 =>
            siteRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 3 =>
            jurisdictionRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 4 =>
            productRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 5 =>
            platformRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 6 =>
            deviceRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 7 =>
            ipAddr.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Source
    }
    object Source extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Source] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Source] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Source = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.Source(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[String]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Source] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.Source(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[String]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(6)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(6)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Source()
      implicit class SourceLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Source]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Source](_l) {
        def channelRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getChannelRef)((c_, f_) => c_.copy(channelRef = Some(f_)))
        def optionalChannelRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.channelRef)((c_, f_) => c_.copy(channelRef = f_))
        def siteRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getSiteRef)((c_, f_) => c_.copy(siteRef = Some(f_)))
        def optionalSiteRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.siteRef)((c_, f_) => c_.copy(siteRef = f_))
        def jurisdictionRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getJurisdictionRef)((c_, f_) => c_.copy(jurisdictionRef = Some(f_)))
        def optionalJurisdictionRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.jurisdictionRef)((c_, f_) => c_.copy(jurisdictionRef = f_))
        def productRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getProductRef)((c_, f_) => c_.copy(productRef = Some(f_)))
        def optionalProductRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.productRef)((c_, f_) => c_.copy(productRef = f_))
        def platformRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPlatformRef)((c_, f_) => c_.copy(platformRef = Some(f_)))
        def optionalPlatformRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.platformRef)((c_, f_) => c_.copy(platformRef = f_))
        def deviceRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getDeviceRef)((c_, f_) => c_.copy(deviceRef = Some(f_)))
        def optionalDeviceRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.deviceRef)((c_, f_) => c_.copy(deviceRef = f_))
        def ipAddr: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getIpAddr)((c_, f_) => c_.copy(ipAddr = Some(f_)))
        def optionalIpAddr: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.ipAddr)((c_, f_) => c_.copy(ipAddr = f_))
      }
      final val CHANNELREF_FIELD_NUMBER = 1
      final val SITEREF_FIELD_NUMBER = 2
      final val JURISDICTIONREF_FIELD_NUMBER = 3
      final val PRODUCTREF_FIELD_NUMBER = 4
      final val PLATFORMREF_FIELD_NUMBER = 5
      final val DEVICEREF_FIELD_NUMBER = 6
      final val IPADDR_FIELD_NUMBER = 7
    }
    @SerialVersionUID(0L) final case class ExternalRef(provider: String, id: String, refType: scala.Option[String] = None, uri: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[ExternalRef] with com.trueaccord.lenses.Updatable[ExternalRef] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, provider)
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, id)
        if (refType.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, refType.get)
        }
        if (uri.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, uri.get)
        }
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
        _output__.writeString(1, provider)
        _output__.writeString(2, id)
        refType.foreach {
          __v => _output__.writeString(3, __v)
        }
        uri.foreach {
          __v => _output__.writeString(4, __v)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef = {
        var __provider = this.provider
        var __id = this.id
        var __refType = this.refType
        var __uri = this.uri
        var __requiredFields0: Long = 3L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __provider = _input__.readString()
              __requiredFields0 &= -2L
            case 18 =>
              __id = _input__.readString()
              __requiredFields0 &= -3L
            case 26 =>
              __refType = Some(_input__.readString())
            case 34 =>
              __uri = Some(_input__.readString())
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef(provider = __provider, id = __id, refType = __refType, uri = __uri)
      }
      def withProvider(__v: String): ExternalRef = copy(provider = __v)
      def withId(__v: String): ExternalRef = copy(id = __v)
      def getRefType: String = refType.getOrElse("")
      def clearRefType: ExternalRef = copy(refType = None)
      def withRefType(__v: String): ExternalRef = copy(refType = Some(__v))
      def getUri: String = uri.getOrElse("")
      def clearUri: ExternalRef = copy(uri = None)
      def withUri(__v: String): ExternalRef = copy(uri = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            provider
          case 2 =>
            id
          case 3 =>
            refType.orNull
          case 4 =>
            uri.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PString(provider)
          case 2 =>
            _root_.scalapb.descriptors.PString(id)
          case 3 =>
            refType.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 4 =>
            uri.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef
    }
    object ExternalRef extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap(__fields.get(1)).asInstanceOf[String], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(7)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(7)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef(provider = "", id = "")
      implicit class ExternalRefLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef](_l) {
        def provider: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.provider)((c_, f_) => c_.copy(provider = f_))
        def id: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.id)((c_, f_) => c_.copy(id = f_))
        def refType: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getRefType)((c_, f_) => c_.copy(refType = Some(f_)))
        def optionalRefType: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.refType)((c_, f_) => c_.copy(refType = f_))
        def uri: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getUri)((c_, f_) => c_.copy(uri = Some(f_)))
        def optionalUri: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.uri)((c_, f_) => c_.copy(uri = f_))
      }
      final val PROVIDER_FIELD_NUMBER = 1
      final val ID_FIELD_NUMBER = 2
      final val REFTYPE_FIELD_NUMBER = 3
      final val URI_FIELD_NUMBER = 4
    }
    @SerialVersionUID(0L) final case class Bet(id: String, creationDate: scala.Option[String] = None, accountRef: scala.Option[String] = None, customerRef: scala.Option[String] = None, source: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Source] = None, externalUID: List[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef] = List.empty, betTypeRef: scala.Option[String] = None, placedAt: scala.Option[String] = None, receipt: scala.Option[String] = None, isSettled: scala.Option[Boolean] = None, isConfirmed: scala.Option[Boolean] = None, isCancelled: scala.Option[Boolean] = None, isCashedOut: scala.Option[Boolean] = None, isPoolBet: scala.Option[Boolean] = None, settledAt: scala.Option[String] = None, settledHow: scala.Option[String] = None, placedByCustomerRef: scala.Option[String] = None, isParked: scala.Option[Boolean] = None, stake: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = None, poolStake: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = None, payout: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout] = None, lines: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines] = None, leg: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg] = List.empty, betTermChange: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange] = List.empty, poolBetSystem: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem] = None, poolBetSubscriptionRef: scala.Option[String] = None, isPending: scala.Option[Boolean] = None, betOverrides: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride] = List.empty) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Bet] with com.trueaccord.lenses.Updatable[Bet] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, id)
        if (creationDate.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, creationDate.get)
        }
        if (accountRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, accountRef.get)
        }
        if (customerRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, customerRef.get)
        }
        if (source.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(source.get.serializedSize) + source.get.serializedSize
        }
        externalUID.foreach(externalUID => __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(externalUID.serializedSize) + externalUID.serializedSize)
        if (betTypeRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, betTypeRef.get)
        }
        if (placedAt.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(7, placedAt.get)
        }
        if (receipt.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(8, receipt.get)
        }
        if (isSettled.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(9, isSettled.get)
        }
        if (isConfirmed.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(10, isConfirmed.get)
        }
        if (isCancelled.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(11, isCancelled.get)
        }
        if (isCashedOut.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(12, isCashedOut.get)
        }
        if (isPoolBet.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(13, isPoolBet.get)
        }
        if (settledAt.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(14, settledAt.get)
        }
        if (settledHow.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(15, settledHow.get)
        }
        if (placedByCustomerRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(20, placedByCustomerRef.get)
        }
        if (isParked.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(21, isParked.get)
        }
        if (stake.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(stake.get.serializedSize) + stake.get.serializedSize
        }
        if (poolStake.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(poolStake.get.serializedSize) + poolStake.get.serializedSize
        }
        if (payout.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(payout.get.serializedSize) + payout.get.serializedSize
        }
        if (lines.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(lines.get.serializedSize) + lines.get.serializedSize
        }
        leg.foreach(leg => __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(leg.serializedSize) + leg.serializedSize)
        betTermChange.foreach(betTermChange => __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(betTermChange.serializedSize) + betTermChange.serializedSize)
        if (poolBetSystem.isDefined) {
          __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(poolBetSystem.get.serializedSize) + poolBetSystem.get.serializedSize
        }
        if (poolBetSubscriptionRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(63, poolBetSubscriptionRef.get)
        }
        if (isPending.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(64, isPending.get)
        }
        betOverrides.foreach(betOverrides => __size += 2 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(betOverrides.serializedSize) + betOverrides.serializedSize)
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
        _output__.writeString(1, id)
        creationDate.foreach {
          __v => _output__.writeString(2, __v)
        }
        accountRef.foreach {
          __v => _output__.writeString(3, __v)
        }
        customerRef.foreach {
          __v => _output__.writeString(4, __v)
        }
        source.foreach { __v =>
          _output__.writeTag(5, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        betTypeRef.foreach {
          __v => _output__.writeString(6, __v)
        }
        placedAt.foreach {
          __v => _output__.writeString(7, __v)
        }
        receipt.foreach {
          __v => _output__.writeString(8, __v)
        }
        isSettled.foreach {
          __v => _output__.writeBool(9, __v)
        }
        isConfirmed.foreach {
          __v => _output__.writeBool(10, __v)
        }
        isCancelled.foreach {
          __v => _output__.writeBool(11, __v)
        }
        isCashedOut.foreach {
          __v => _output__.writeBool(12, __v)
        }
        isPoolBet.foreach {
          __v => _output__.writeBool(13, __v)
        }
        settledAt.foreach {
          __v => _output__.writeString(14, __v)
        }
        settledHow.foreach {
          __v => _output__.writeString(15, __v)
        }
        placedByCustomerRef.foreach {
          __v => _output__.writeString(20, __v)
        }
        isParked.foreach {
          __v => _output__.writeBool(21, __v)
        }
        externalUID.foreach { __v =>
          _output__.writeTag(22, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        stake.foreach { __v =>
          _output__.writeTag(30, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        poolStake.foreach { __v =>
          _output__.writeTag(31, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        payout.foreach { __v =>
          _output__.writeTag(40, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        lines.foreach { __v =>
          _output__.writeTag(50, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        leg.foreach { __v =>
          _output__.writeTag(60, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        betTermChange.foreach { __v =>
          _output__.writeTag(61, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        poolBetSystem.foreach { __v =>
          _output__.writeTag(62, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        poolBetSubscriptionRef.foreach {
          __v => _output__.writeString(63, __v)
        }
        isPending.foreach {
          __v => _output__.writeBool(64, __v)
        }
        betOverrides.foreach { __v =>
          _output__.writeTag(65, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet = {
        var __id = this.id
        var __creationDate = this.creationDate
        var __accountRef = this.accountRef
        var __customerRef = this.customerRef
        var __source = this.source
        val __externalUID = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef] ++= this.externalUID
        var __betTypeRef = this.betTypeRef
        var __placedAt = this.placedAt
        var __receipt = this.receipt
        var __isSettled = this.isSettled
        var __isConfirmed = this.isConfirmed
        var __isCancelled = this.isCancelled
        var __isCashedOut = this.isCashedOut
        var __isPoolBet = this.isPoolBet
        var __settledAt = this.settledAt
        var __settledHow = this.settledHow
        var __placedByCustomerRef = this.placedByCustomerRef
        var __isParked = this.isParked
        var __stake = this.stake
        var __poolStake = this.poolStake
        var __payout = this.payout
        var __lines = this.lines
        val __leg = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg] ++= this.leg
        val __betTermChange = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange] ++= this.betTermChange
        var __poolBetSystem = this.poolBetSystem
        var __poolBetSubscriptionRef = this.poolBetSubscriptionRef
        var __isPending = this.isPending
        val __betOverrides = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride] ++= this.betOverrides
        var __requiredFields0: Long = 1L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __id = _input__.readString()
              __requiredFields0 &= -2L
            case 18 =>
              __creationDate = Some(_input__.readString())
            case 26 =>
              __accountRef = Some(_input__.readString())
            case 34 =>
              __customerRef = Some(_input__.readString())
            case 42 =>
              __source = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __source.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Source.defaultInstance)))
            case 178 =>
              __externalUID += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef.defaultInstance)
            case 50 =>
              __betTypeRef = Some(_input__.readString())
            case 58 =>
              __placedAt = Some(_input__.readString())
            case 66 =>
              __receipt = Some(_input__.readString())
            case 72 =>
              __isSettled = Some(_input__.readBool())
            case 80 =>
              __isConfirmed = Some(_input__.readBool())
            case 88 =>
              __isCancelled = Some(_input__.readBool())
            case 96 =>
              __isCashedOut = Some(_input__.readBool())
            case 104 =>
              __isPoolBet = Some(_input__.readBool())
            case 114 =>
              __settledAt = Some(_input__.readString())
            case 122 =>
              __settledHow = Some(_input__.readString())
            case 162 =>
              __placedByCustomerRef = Some(_input__.readString())
            case 168 =>
              __isParked = Some(_input__.readBool())
            case 242 =>
              __stake = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __stake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)))
            case 250 =>
              __poolStake = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __poolStake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)))
            case 322 =>
              __payout = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __payout.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout.defaultInstance)))
            case 402 =>
              __lines = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __lines.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines.defaultInstance)))
            case 482 =>
              __leg += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.defaultInstance)
            case 490 =>
              __betTermChange += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.defaultInstance)
            case 498 =>
              __poolBetSystem = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __poolBetSystem.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem.defaultInstance)))
            case 506 =>
              __poolBetSubscriptionRef = Some(_input__.readString())
            case 512 =>
              __isPending = Some(_input__.readBool())
            case 522 =>
              __betOverrides += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride.defaultInstance)
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.Bet(id = __id, creationDate = __creationDate, accountRef = __accountRef, customerRef = __customerRef, source = __source, externalUID = __externalUID.result(), betTypeRef = __betTypeRef, placedAt = __placedAt, receipt = __receipt, isSettled = __isSettled, isConfirmed = __isConfirmed, isCancelled = __isCancelled, isCashedOut = __isCashedOut, isPoolBet = __isPoolBet, settledAt = __settledAt, settledHow = __settledHow, placedByCustomerRef = __placedByCustomerRef, isParked = __isParked, stake = __stake, poolStake = __poolStake, payout = __payout, lines = __lines, leg = __leg.result(), betTermChange = __betTermChange.result(), poolBetSystem = __poolBetSystem, poolBetSubscriptionRef = __poolBetSubscriptionRef, isPending = __isPending, betOverrides = __betOverrides.result())
      }
      def withId(__v: String): Bet = copy(id = __v)
      def getCreationDate: String = creationDate.getOrElse("")
      def clearCreationDate: Bet = copy(creationDate = None)
      def withCreationDate(__v: String): Bet = copy(creationDate = Some(__v))
      def getAccountRef: String = accountRef.getOrElse("")
      def clearAccountRef: Bet = copy(accountRef = None)
      def withAccountRef(__v: String): Bet = copy(accountRef = Some(__v))
      def getCustomerRef: String = customerRef.getOrElse("")
      def clearCustomerRef: Bet = copy(customerRef = None)
      def withCustomerRef(__v: String): Bet = copy(customerRef = Some(__v))
      def getSource: parallelai.sot.executor.builder.SOTBuilder.gen.Source = source.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Source.defaultInstance)
      def clearSource: Bet = copy(source = None)
      def withSource(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Source): Bet = copy(source = Some(__v))
      def clearExternalUID = copy(externalUID = List.empty)
      def addExternalUID(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef*): Bet = addAllExternalUID(__vs)
      def addAllExternalUID(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]): Bet = copy(externalUID = externalUID ++ __vs)
      def withExternalUID(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]): Bet = copy(externalUID = __v)
      def getBetTypeRef: String = betTypeRef.getOrElse("")
      def clearBetTypeRef: Bet = copy(betTypeRef = None)
      def withBetTypeRef(__v: String): Bet = copy(betTypeRef = Some(__v))
      def getPlacedAt: String = placedAt.getOrElse("")
      def clearPlacedAt: Bet = copy(placedAt = None)
      def withPlacedAt(__v: String): Bet = copy(placedAt = Some(__v))
      def getReceipt: String = receipt.getOrElse("")
      def clearReceipt: Bet = copy(receipt = None)
      def withReceipt(__v: String): Bet = copy(receipt = Some(__v))
      def getIsSettled: Boolean = isSettled.getOrElse(false)
      def clearIsSettled: Bet = copy(isSettled = None)
      def withIsSettled(__v: Boolean): Bet = copy(isSettled = Some(__v))
      def getIsConfirmed: Boolean = isConfirmed.getOrElse(false)
      def clearIsConfirmed: Bet = copy(isConfirmed = None)
      def withIsConfirmed(__v: Boolean): Bet = copy(isConfirmed = Some(__v))
      def getIsCancelled: Boolean = isCancelled.getOrElse(false)
      def clearIsCancelled: Bet = copy(isCancelled = None)
      def withIsCancelled(__v: Boolean): Bet = copy(isCancelled = Some(__v))
      def getIsCashedOut: Boolean = isCashedOut.getOrElse(false)
      def clearIsCashedOut: Bet = copy(isCashedOut = None)
      def withIsCashedOut(__v: Boolean): Bet = copy(isCashedOut = Some(__v))
      def getIsPoolBet: Boolean = isPoolBet.getOrElse(false)
      def clearIsPoolBet: Bet = copy(isPoolBet = None)
      def withIsPoolBet(__v: Boolean): Bet = copy(isPoolBet = Some(__v))
      def getSettledAt: String = settledAt.getOrElse("")
      def clearSettledAt: Bet = copy(settledAt = None)
      def withSettledAt(__v: String): Bet = copy(settledAt = Some(__v))
      def getSettledHow: String = settledHow.getOrElse("")
      def clearSettledHow: Bet = copy(settledHow = None)
      def withSettledHow(__v: String): Bet = copy(settledHow = Some(__v))
      def getPlacedByCustomerRef: String = placedByCustomerRef.getOrElse("")
      def clearPlacedByCustomerRef: Bet = copy(placedByCustomerRef = None)
      def withPlacedByCustomerRef(__v: String): Bet = copy(placedByCustomerRef = Some(__v))
      def getIsParked: Boolean = isParked.getOrElse(false)
      def clearIsParked: Bet = copy(isParked = None)
      def withIsParked(__v: Boolean): Bet = copy(isParked = Some(__v))
      def getStake: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake = stake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)
      def clearStake: Bet = copy(stake = None)
      def withStake(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake): Bet = copy(stake = Some(__v))
      def getPoolStake: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake = poolStake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)
      def clearPoolStake: Bet = copy(poolStake = None)
      def withPoolStake(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake): Bet = copy(poolStake = Some(__v))
      def getPayout: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout = payout.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout.defaultInstance)
      def clearPayout: Bet = copy(payout = None)
      def withPayout(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout): Bet = copy(payout = Some(__v))
      def getLines: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines = lines.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines.defaultInstance)
      def clearLines: Bet = copy(lines = None)
      def withLines(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines): Bet = copy(lines = Some(__v))
      def clearLeg = copy(leg = List.empty)
      def addLeg(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg*): Bet = addAllLeg(__vs)
      def addAllLeg(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]): Bet = copy(leg = leg ++ __vs)
      def withLeg(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]): Bet = copy(leg = __v)
      def clearBetTermChange = copy(betTermChange = List.empty)
      def addBetTermChange(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange*): Bet = addAllBetTermChange(__vs)
      def addAllBetTermChange(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]): Bet = copy(betTermChange = betTermChange ++ __vs)
      def withBetTermChange(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]): Bet = copy(betTermChange = __v)
      def getPoolBetSystem: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem = poolBetSystem.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem.defaultInstance)
      def clearPoolBetSystem: Bet = copy(poolBetSystem = None)
      def withPoolBetSystem(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem): Bet = copy(poolBetSystem = Some(__v))
      def getPoolBetSubscriptionRef: String = poolBetSubscriptionRef.getOrElse("")
      def clearPoolBetSubscriptionRef: Bet = copy(poolBetSubscriptionRef = None)
      def withPoolBetSubscriptionRef(__v: String): Bet = copy(poolBetSubscriptionRef = Some(__v))
      def getIsPending: Boolean = isPending.getOrElse(false)
      def clearIsPending: Bet = copy(isPending = None)
      def withIsPending(__v: Boolean): Bet = copy(isPending = Some(__v))
      def clearBetOverrides = copy(betOverrides = List.empty)
      def addBetOverrides(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride*): Bet = addAllBetOverrides(__vs)
      def addAllBetOverrides(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]): Bet = copy(betOverrides = betOverrides ++ __vs)
      def withBetOverrides(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]): Bet = copy(betOverrides = __v)
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            id
          case 2 =>
            creationDate.orNull
          case 3 =>
            accountRef.orNull
          case 4 =>
            customerRef.orNull
          case 5 =>
            source.orNull
          case 22 =>
            externalUID
          case 6 =>
            betTypeRef.orNull
          case 7 =>
            placedAt.orNull
          case 8 =>
            receipt.orNull
          case 9 =>
            isSettled.orNull
          case 10 =>
            isConfirmed.orNull
          case 11 =>
            isCancelled.orNull
          case 12 =>
            isCashedOut.orNull
          case 13 =>
            isPoolBet.orNull
          case 14 =>
            settledAt.orNull
          case 15 =>
            settledHow.orNull
          case 20 =>
            placedByCustomerRef.orNull
          case 21 =>
            isParked.orNull
          case 30 =>
            stake.orNull
          case 31 =>
            poolStake.orNull
          case 40 =>
            payout.orNull
          case 50 =>
            lines.orNull
          case 60 =>
            leg
          case 61 =>
            betTermChange
          case 62 =>
            poolBetSystem.orNull
          case 63 =>
            poolBetSubscriptionRef.orNull
          case 64 =>
            isPending.orNull
          case 65 =>
            betOverrides
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PString(id)
          case 2 =>
            creationDate.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 3 =>
            accountRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 4 =>
            customerRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 5 =>
            source.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 22 =>
            _root_.scalapb.descriptors.PRepeated(externalUID.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 6 =>
            betTypeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 7 =>
            placedAt.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 8 =>
            receipt.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 9 =>
            isSettled.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 10 =>
            isConfirmed.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 11 =>
            isCancelled.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 12 =>
            isCashedOut.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 13 =>
            isPoolBet.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 14 =>
            settledAt.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 15 =>
            settledHow.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 20 =>
            placedByCustomerRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 21 =>
            isParked.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 30 =>
            stake.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 31 =>
            poolStake.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 40 =>
            payout.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 50 =>
            lines.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 60 =>
            _root_.scalapb.descriptors.PRepeated(leg.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 61 =>
            _root_.scalapb.descriptors.PRepeated(betTermChange.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 62 =>
            poolBetSystem.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 63 =>
            poolBetSubscriptionRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 64 =>
            isPending.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 65 =>
            _root_.scalapb.descriptors.PRepeated(betOverrides.map(_.toPMessage)(_root_.scala.collection.breakOut))
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet
    }
    object Bet extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.Bet(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Source]], __fieldsMap.getOrElse(__fields.get(5), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(7)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(8)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(9)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(10)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(11)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(12)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(13)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(14)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(15)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(16)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(17)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(18)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]], __fieldsMap.get(__fields.get(19)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]], __fieldsMap.get(__fields.get(20)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout]], __fieldsMap.get(__fields.get(21)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines]], __fieldsMap.getOrElse(__fields.get(22), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]], __fieldsMap.getOrElse(__fields.get(23), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]], __fieldsMap.get(__fields.get(24)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem]], __fieldsMap.get(__fields.get(25)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(26)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.getOrElse(__fields.get(27), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Source]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(22).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(10).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(11).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(12).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(13).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(14).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(15).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(20).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(21).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(30).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(31).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(40).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(50).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(60).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(61).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(62).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(63).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(64).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(65).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]]).getOrElse(List.empty))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(8)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(8)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 5 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Source
          case 22 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef
          case 30 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake
          case 31 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake
          case 40 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout
          case 50 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines
          case 60 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg
          case 61 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange
          case 62 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem
          case 65 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride)
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet(id = "")
      @SerialVersionUID(0L) final case class Stake(amount: scala.Option[String] = None, stakePerLine: scala.Option[String] = None, freeBet: scala.Option[String] = None, currencyRef: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Stake] with com.trueaccord.lenses.Updatable[Stake] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (amount.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, amount.get)
          }
          if (stakePerLine.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, stakePerLine.get)
          }
          if (freeBet.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, freeBet.get)
          }
          if (currencyRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, currencyRef.get)
          }
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
          amount.foreach {
            __v => _output__.writeString(1, __v)
          }
          stakePerLine.foreach {
            __v => _output__.writeString(2, __v)
          }
          freeBet.foreach {
            __v => _output__.writeString(3, __v)
          }
          currencyRef.foreach {
            __v => _output__.writeString(4, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake = {
          var __amount = this.amount
          var __stakePerLine = this.stakePerLine
          var __freeBet = this.freeBet
          var __currencyRef = this.currencyRef
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __amount = Some(_input__.readString())
              case 18 =>
                __stakePerLine = Some(_input__.readString())
              case 26 =>
                __freeBet = Some(_input__.readString())
              case 34 =>
                __currencyRef = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake(amount = __amount, stakePerLine = __stakePerLine, freeBet = __freeBet, currencyRef = __currencyRef)
        }
        def getAmount: String = amount.getOrElse("")
        def clearAmount: Stake = copy(amount = None)
        def withAmount(__v: String): Stake = copy(amount = Some(__v))
        def getStakePerLine: String = stakePerLine.getOrElse("")
        def clearStakePerLine: Stake = copy(stakePerLine = None)
        def withStakePerLine(__v: String): Stake = copy(stakePerLine = Some(__v))
        def getFreeBet: String = freeBet.getOrElse("")
        def clearFreeBet: Stake = copy(freeBet = None)
        def withFreeBet(__v: String): Stake = copy(freeBet = Some(__v))
        def getCurrencyRef: String = currencyRef.getOrElse("")
        def clearCurrencyRef: Stake = copy(currencyRef = None)
        def withCurrencyRef(__v: String): Stake = copy(currencyRef = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              amount.orNull
            case 2 =>
              stakePerLine.orNull
            case 3 =>
              freeBet.orNull
            case 4 =>
              currencyRef.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              amount.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              stakePerLine.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              freeBet.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              currencyRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake
      }
      object Stake extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(0)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(0)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake()
        implicit class StakeLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake](_l) {
          def amount: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getAmount)((c_, f_) => c_.copy(amount = Some(f_)))
          def optionalAmount: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.amount)((c_, f_) => c_.copy(amount = f_))
          def stakePerLine: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getStakePerLine)((c_, f_) => c_.copy(stakePerLine = Some(f_)))
          def optionalStakePerLine: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.stakePerLine)((c_, f_) => c_.copy(stakePerLine = f_))
          def freeBet: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getFreeBet)((c_, f_) => c_.copy(freeBet = Some(f_)))
          def optionalFreeBet: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.freeBet)((c_, f_) => c_.copy(freeBet = f_))
          def currencyRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCurrencyRef)((c_, f_) => c_.copy(currencyRef = Some(f_)))
          def optionalCurrencyRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.currencyRef)((c_, f_) => c_.copy(currencyRef = f_))
        }
        final val AMOUNT_FIELD_NUMBER = 1
        final val STAKEPERLINE_FIELD_NUMBER = 2
        final val FREEBET_FIELD_NUMBER = 3
        final val CURRENCYREF_FIELD_NUMBER = 4
      }
      @SerialVersionUID(0L) final case class Payout(winnings: scala.Option[String] = None, refunds: scala.Option[String] = None, potential: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Payout] with com.trueaccord.lenses.Updatable[Payout] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (winnings.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, winnings.get)
          }
          if (refunds.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, refunds.get)
          }
          if (potential.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, potential.get)
          }
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
          winnings.foreach {
            __v => _output__.writeString(1, __v)
          }
          refunds.foreach {
            __v => _output__.writeString(2, __v)
          }
          potential.foreach {
            __v => _output__.writeString(3, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout = {
          var __winnings = this.winnings
          var __refunds = this.refunds
          var __potential = this.potential
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __winnings = Some(_input__.readString())
              case 18 =>
                __refunds = Some(_input__.readString())
              case 26 =>
                __potential = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout(winnings = __winnings, refunds = __refunds, potential = __potential)
        }
        def getWinnings: String = winnings.getOrElse("")
        def clearWinnings: Payout = copy(winnings = None)
        def withWinnings(__v: String): Payout = copy(winnings = Some(__v))
        def getRefunds: String = refunds.getOrElse("")
        def clearRefunds: Payout = copy(refunds = None)
        def withRefunds(__v: String): Payout = copy(refunds = Some(__v))
        def getPotential: String = potential.getOrElse("")
        def clearPotential: Payout = copy(potential = None)
        def withPotential(__v: String): Payout = copy(potential = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              winnings.orNull
            case 2 =>
              refunds.orNull
            case 3 =>
              potential.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              winnings.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              refunds.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              potential.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout
      }
      object Payout extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(1)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(1)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout()
        implicit class PayoutLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout](_l) {
          def winnings: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getWinnings)((c_, f_) => c_.copy(winnings = Some(f_)))
          def optionalWinnings: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.winnings)((c_, f_) => c_.copy(winnings = f_))
          def refunds: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getRefunds)((c_, f_) => c_.copy(refunds = Some(f_)))
          def optionalRefunds: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.refunds)((c_, f_) => c_.copy(refunds = f_))
          def potential: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPotential)((c_, f_) => c_.copy(potential = Some(f_)))
          def optionalPotential: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.potential)((c_, f_) => c_.copy(potential = f_))
        }
        final val WINNINGS_FIELD_NUMBER = 1
        final val REFUNDS_FIELD_NUMBER = 2
        final val POTENTIAL_FIELD_NUMBER = 3
      }
      @SerialVersionUID(0L) final case class Lines(number: scala.Option[Int] = None, win: scala.Option[Int] = None, lose: scala.Option[Int] = None, voided: scala.Option[Int] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Lines] with com.trueaccord.lenses.Updatable[Lines] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (number.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, number.get)
          }
          if (win.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, win.get)
          }
          if (lose.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(3, lose.get)
          }
          if (voided.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(4, voided.get)
          }
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
          number.foreach {
            __v => _output__.writeInt32(1, __v)
          }
          win.foreach {
            __v => _output__.writeInt32(2, __v)
          }
          lose.foreach {
            __v => _output__.writeInt32(3, __v)
          }
          voided.foreach {
            __v => _output__.writeInt32(4, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines = {
          var __number = this.number
          var __win = this.win
          var __lose = this.lose
          var __voided = this.voided
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 8 =>
                __number = Some(_input__.readInt32())
              case 16 =>
                __win = Some(_input__.readInt32())
              case 24 =>
                __lose = Some(_input__.readInt32())
              case 32 =>
                __voided = Some(_input__.readInt32())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines(number = __number, win = __win, lose = __lose, voided = __voided)
        }
        def getNumber: Int = number.getOrElse(0)
        def clearNumber: Lines = copy(number = None)
        def withNumber(__v: Int): Lines = copy(number = Some(__v))
        def getWin: Int = win.getOrElse(0)
        def clearWin: Lines = copy(win = None)
        def withWin(__v: Int): Lines = copy(win = Some(__v))
        def getLose: Int = lose.getOrElse(0)
        def clearLose: Lines = copy(lose = None)
        def withLose(__v: Int): Lines = copy(lose = Some(__v))
        def getVoided: Int = voided.getOrElse(0)
        def clearVoided: Lines = copy(voided = None)
        def withVoided(__v: Int): Lines = copy(voided = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              number.orNull
            case 2 =>
              win.orNull
            case 3 =>
              lose.orNull
            case 4 =>
              voided.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              number.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              win.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              lose.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              voided.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines
      }
      object Lines extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[Int]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[Int]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(2)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(2)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines()
        implicit class LinesLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines](_l) {
          def number: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getNumber)((c_, f_) => c_.copy(number = Some(f_)))
          def optionalNumber: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.number)((c_, f_) => c_.copy(number = f_))
          def win: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getWin)((c_, f_) => c_.copy(win = Some(f_)))
          def optionalWin: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.win)((c_, f_) => c_.copy(win = f_))
          def lose: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getLose)((c_, f_) => c_.copy(lose = Some(f_)))
          def optionalLose: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.lose)((c_, f_) => c_.copy(lose = f_))
          def voided: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getVoided)((c_, f_) => c_.copy(voided = Some(f_)))
          def optionalVoided: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.voided)((c_, f_) => c_.copy(voided = f_))
        }
        final val NUMBER_FIELD_NUMBER = 1
        final val WIN_FIELD_NUMBER = 2
        final val LOSE_FIELD_NUMBER = 3
        final val VOIDED_FIELD_NUMBER = 4
      }
      @SerialVersionUID(0L) final case class Leg(price: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price] = None, winPlaceRef: scala.Option[String] = None, legParts: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart] = List.empty, result: scala.Option[String] = None, poolId: scala.Option[String] = None, legSort: scala.Option[String] = None, index: scala.Option[Int] = None, handicap: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap] = None, noCombi: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Leg] with com.trueaccord.lenses.Updatable[Leg] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (price.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(price.get.serializedSize) + price.get.serializedSize
          }
          if (winPlaceRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, winPlaceRef.get)
          }
          legParts.foreach(legParts => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(legParts.serializedSize) + legParts.serializedSize)
          if (result.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, result.get)
          }
          if (poolId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, poolId.get)
          }
          if (legSort.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, legSort.get)
          }
          if (index.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(7, index.get)
          }
          if (handicap.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(handicap.get.serializedSize) + handicap.get.serializedSize
          }
          if (noCombi.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(9, noCombi.get)
          }
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
          price.foreach { __v =>
            _output__.writeTag(1, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          winPlaceRef.foreach {
            __v => _output__.writeString(2, __v)
          }
          legParts.foreach { __v =>
            _output__.writeTag(3, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          result.foreach {
            __v => _output__.writeString(4, __v)
          }
          poolId.foreach {
            __v => _output__.writeString(5, __v)
          }
          legSort.foreach {
            __v => _output__.writeString(6, __v)
          }
          index.foreach {
            __v => _output__.writeInt32(7, __v)
          }
          handicap.foreach { __v =>
            _output__.writeTag(8, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
          noCombi.foreach {
            __v => _output__.writeString(9, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg = {
          var __price = this.price
          var __winPlaceRef = this.winPlaceRef
          val __legParts = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart] ++= this.legParts
          var __result = this.result
          var __poolId = this.poolId
          var __legSort = this.legSort
          var __index = this.index
          var __handicap = this.handicap
          var __noCombi = this.noCombi
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __price = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __price.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price.defaultInstance)))
              case 18 =>
                __winPlaceRef = Some(_input__.readString())
              case 26 =>
                __legParts += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.defaultInstance)
              case 34 =>
                __result = Some(_input__.readString())
              case 42 =>
                __poolId = Some(_input__.readString())
              case 50 =>
                __legSort = Some(_input__.readString())
              case 56 =>
                __index = Some(_input__.readInt32())
              case 66 =>
                __handicap = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __handicap.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap.defaultInstance)))
              case 74 =>
                __noCombi = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg(price = __price, winPlaceRef = __winPlaceRef, legParts = __legParts.result(), result = __result, poolId = __poolId, legSort = __legSort, index = __index, handicap = __handicap, noCombi = __noCombi)
        }
        def getPrice: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price = price.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price.defaultInstance)
        def clearPrice: Leg = copy(price = None)
        def withPrice(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price): Leg = copy(price = Some(__v))
        def getWinPlaceRef: String = winPlaceRef.getOrElse("")
        def clearWinPlaceRef: Leg = copy(winPlaceRef = None)
        def withWinPlaceRef(__v: String): Leg = copy(winPlaceRef = Some(__v))
        def clearLegParts = copy(legParts = List.empty)
        def addLegParts(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart*): Leg = addAllLegParts(__vs)
        def addAllLegParts(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]): Leg = copy(legParts = legParts ++ __vs)
        def withLegParts(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]): Leg = copy(legParts = __v)
        def getResult: String = result.getOrElse("")
        def clearResult: Leg = copy(result = None)
        def withResult(__v: String): Leg = copy(result = Some(__v))
        def getPoolId: String = poolId.getOrElse("")
        def clearPoolId: Leg = copy(poolId = None)
        def withPoolId(__v: String): Leg = copy(poolId = Some(__v))
        def getLegSort: String = legSort.getOrElse("")
        def clearLegSort: Leg = copy(legSort = None)
        def withLegSort(__v: String): Leg = copy(legSort = Some(__v))
        def getIndex: Int = index.getOrElse(0)
        def clearIndex: Leg = copy(index = None)
        def withIndex(__v: Int): Leg = copy(index = Some(__v))
        def getHandicap: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap = handicap.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap.defaultInstance)
        def clearHandicap: Leg = copy(handicap = None)
        def withHandicap(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap): Leg = copy(handicap = Some(__v))
        def getNoCombi: String = noCombi.getOrElse("")
        def clearNoCombi: Leg = copy(noCombi = None)
        def withNoCombi(__v: String): Leg = copy(noCombi = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              price.orNull
            case 2 =>
              winPlaceRef.orNull
            case 3 =>
              legParts
            case 4 =>
              result.orNull
            case 5 =>
              poolId.orNull
            case 6 =>
              legSort.orNull
            case 7 =>
              index.orNull
            case 8 =>
              handicap.orNull
            case 9 =>
              noCombi.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              price.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              winPlaceRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              _root_.scalapb.descriptors.PRepeated(legParts.map(_.toPMessage)(_root_.scala.collection.breakOut))
            case 4 =>
              result.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 5 =>
              poolId.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 6 =>
              legSort.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 7 =>
              index.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 8 =>
              handicap.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 9 =>
              noCombi.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg
      }
      object Leg extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.getOrElse(__fields.get(2), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(7)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap]], __fieldsMap.get(__fields.get(8)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(3)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(3)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
          var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
          (__number: @_root_.scala.unchecked) match {
            case 1 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price
            case 3 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart
            case 8 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap
          }
          __out
        }
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap)
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg()
        @SerialVersionUID(0L) final case class Price(num: scala.Option[Int] = None, den: scala.Option[Int] = None, decimal: scala.Option[String] = None, priceTypeRef: scala.Option[String] = None, isEarlyPriceActive: scala.Option[Boolean] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Price] with com.trueaccord.lenses.Updatable[Price] {
          @transient private[this] var __serializedSizeCachedValue: Int = 0
          private[this] def __computeSerializedValue(): Int = {
            var __size = 0
            if (num.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, num.get)
            }
            if (den.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, den.get)
            }
            if (decimal.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, decimal.get)
            }
            if (priceTypeRef.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, priceTypeRef.get)
            }
            if (isEarlyPriceActive.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(5, isEarlyPriceActive.get)
            }
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
            num.foreach {
              __v => _output__.writeInt32(1, __v)
            }
            den.foreach {
              __v => _output__.writeInt32(2, __v)
            }
            decimal.foreach {
              __v => _output__.writeString(3, __v)
            }
            priceTypeRef.foreach {
              __v => _output__.writeString(4, __v)
            }
            isEarlyPriceActive.foreach {
              __v => _output__.writeBool(5, __v)
            }
          }
          def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price = {
            var __num = this.num
            var __den = this.den
            var __decimal = this.decimal
            var __priceTypeRef = this.priceTypeRef
            var __isEarlyPriceActive = this.isEarlyPriceActive
            var _done__ = false
            while (!_done__) {
              val _tag__ = _input__.readTag()
              _tag__ match {
                case 0 =>
                  _done__ = true
                case 8 =>
                  __num = Some(_input__.readInt32())
                case 16 =>
                  __den = Some(_input__.readInt32())
                case 26 =>
                  __decimal = Some(_input__.readString())
                case 34 =>
                  __priceTypeRef = Some(_input__.readString())
                case 40 =>
                  __isEarlyPriceActive = Some(_input__.readBool())
                case tag =>
                  _input__.skipField(tag)
              }
            }
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price(num = __num, den = __den, decimal = __decimal, priceTypeRef = __priceTypeRef, isEarlyPriceActive = __isEarlyPriceActive)
          }
          def getNum: Int = num.getOrElse(0)
          def clearNum: Price = copy(num = None)
          def withNum(__v: Int): Price = copy(num = Some(__v))
          def getDen: Int = den.getOrElse(0)
          def clearDen: Price = copy(den = None)
          def withDen(__v: Int): Price = copy(den = Some(__v))
          def getDecimal: String = decimal.getOrElse("")
          def clearDecimal: Price = copy(decimal = None)
          def withDecimal(__v: String): Price = copy(decimal = Some(__v))
          def getPriceTypeRef: String = priceTypeRef.getOrElse("")
          def clearPriceTypeRef: Price = copy(priceTypeRef = None)
          def withPriceTypeRef(__v: String): Price = copy(priceTypeRef = Some(__v))
          def getIsEarlyPriceActive: Boolean = isEarlyPriceActive.getOrElse(false)
          def clearIsEarlyPriceActive: Price = copy(isEarlyPriceActive = None)
          def withIsEarlyPriceActive(__v: Boolean): Price = copy(isEarlyPriceActive = Some(__v))
          def getFieldByNumber(__fieldNumber: Int): scala.Any = {
            (__fieldNumber: @_root_.scala.unchecked) match {
              case 1 =>
                num.orNull
              case 2 =>
                den.orNull
              case 3 =>
                decimal.orNull
              case 4 =>
                priceTypeRef.orNull
              case 5 =>
                isEarlyPriceActive.orNull
            }
          }
          def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
            require(__field.containingMessage eq companion.scalaDescriptor)
            (__field.number: @_root_.scala.unchecked) match {
              case 1 =>
                num.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 2 =>
                den.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 3 =>
                decimal.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 4 =>
                priceTypeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 5 =>
                isEarlyPriceActive.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
            }
          }
          override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
          def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price
        }
        object Price extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price] {
          implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price] = this
          def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price = {
            require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
            val __fields = javaDescriptor.getFields
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[Boolean]])
          }
          implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price] = _root_.scalapb.descriptors.Reads({
            case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
              require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[Boolean]]))
            case _ =>
              throw new RuntimeException("Expected PMessage")
          })
          def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.javaDescriptor.getNestedTypes.get(0)
          def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.scalaDescriptor.nestedMessages(0)
          def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
          lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
          def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
          lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price()
          implicit class PriceLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price](_l) {
            def num: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getNum)((c_, f_) => c_.copy(num = Some(f_)))
            def optionalNum: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.num)((c_, f_) => c_.copy(num = f_))
            def den: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getDen)((c_, f_) => c_.copy(den = Some(f_)))
            def optionalDen: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.den)((c_, f_) => c_.copy(den = f_))
            def decimal: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getDecimal)((c_, f_) => c_.copy(decimal = Some(f_)))
            def optionalDecimal: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.decimal)((c_, f_) => c_.copy(decimal = f_))
            def priceTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPriceTypeRef)((c_, f_) => c_.copy(priceTypeRef = Some(f_)))
            def optionalPriceTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.priceTypeRef)((c_, f_) => c_.copy(priceTypeRef = f_))
            def isEarlyPriceActive: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsEarlyPriceActive)((c_, f_) => c_.copy(isEarlyPriceActive = Some(f_)))
            def optionalIsEarlyPriceActive: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isEarlyPriceActive)((c_, f_) => c_.copy(isEarlyPriceActive = f_))
          }
          final val NUM_FIELD_NUMBER = 1
          final val DEN_FIELD_NUMBER = 2
          final val DECIMAL_FIELD_NUMBER = 3
          final val PRICETYPEREF_FIELD_NUMBER = 4
          final val ISEARLYPRICEACTIVE_FIELD_NUMBER = 5
        }
        @SerialVersionUID(0L) final case class LegPart(outcomeRef: scala.Option[String] = None, marketRef: scala.Option[String] = None, eventRef: scala.Option[String] = None, places: scala.Option[String] = None, isInRunning: scala.Option[Boolean] = None, bettingSystemPreferred: scala.Option[Boolean] = None, placeTerms: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[LegPart] with com.trueaccord.lenses.Updatable[LegPart] {
          @transient private[this] var __serializedSizeCachedValue: Int = 0
          private[this] def __computeSerializedValue(): Int = {
            var __size = 0
            if (outcomeRef.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, outcomeRef.get)
            }
            if (marketRef.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, marketRef.get)
            }
            if (eventRef.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, eventRef.get)
            }
            if (places.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(8, places.get)
            }
            if (isInRunning.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(9, isInRunning.get)
            }
            if (bettingSystemPreferred.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeBoolSize(10, bettingSystemPreferred.get)
            }
            if (placeTerms.isDefined) {
              __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(placeTerms.get.serializedSize) + placeTerms.get.serializedSize
            }
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
            outcomeRef.foreach {
              __v => _output__.writeString(1, __v)
            }
            marketRef.foreach {
              __v => _output__.writeString(2, __v)
            }
            eventRef.foreach {
              __v => _output__.writeString(3, __v)
            }
            places.foreach {
              __v => _output__.writeString(8, __v)
            }
            isInRunning.foreach {
              __v => _output__.writeBool(9, __v)
            }
            bettingSystemPreferred.foreach {
              __v => _output__.writeBool(10, __v)
            }
            placeTerms.foreach { __v =>
              _output__.writeTag(11, 2)
              _output__.writeUInt32NoTag(__v.serializedSize)
              __v.writeTo(_output__)
            }
          }
          def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart = {
            var __outcomeRef = this.outcomeRef
            var __marketRef = this.marketRef
            var __eventRef = this.eventRef
            var __places = this.places
            var __isInRunning = this.isInRunning
            var __bettingSystemPreferred = this.bettingSystemPreferred
            var __placeTerms = this.placeTerms
            var _done__ = false
            while (!_done__) {
              val _tag__ = _input__.readTag()
              _tag__ match {
                case 0 =>
                  _done__ = true
                case 10 =>
                  __outcomeRef = Some(_input__.readString())
                case 18 =>
                  __marketRef = Some(_input__.readString())
                case 26 =>
                  __eventRef = Some(_input__.readString())
                case 66 =>
                  __places = Some(_input__.readString())
                case 72 =>
                  __isInRunning = Some(_input__.readBool())
                case 80 =>
                  __bettingSystemPreferred = Some(_input__.readBool())
                case 90 =>
                  __placeTerms = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __placeTerms.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms.defaultInstance)))
                case tag =>
                  _input__.skipField(tag)
              }
            }
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart(outcomeRef = __outcomeRef, marketRef = __marketRef, eventRef = __eventRef, places = __places, isInRunning = __isInRunning, bettingSystemPreferred = __bettingSystemPreferred, placeTerms = __placeTerms)
          }
          def getOutcomeRef: String = outcomeRef.getOrElse("")
          def clearOutcomeRef: LegPart = copy(outcomeRef = None)
          def withOutcomeRef(__v: String): LegPart = copy(outcomeRef = Some(__v))
          def getMarketRef: String = marketRef.getOrElse("")
          def clearMarketRef: LegPart = copy(marketRef = None)
          def withMarketRef(__v: String): LegPart = copy(marketRef = Some(__v))
          def getEventRef: String = eventRef.getOrElse("")
          def clearEventRef: LegPart = copy(eventRef = None)
          def withEventRef(__v: String): LegPart = copy(eventRef = Some(__v))
          def getPlaces: String = places.getOrElse("")
          def clearPlaces: LegPart = copy(places = None)
          def withPlaces(__v: String): LegPart = copy(places = Some(__v))
          def getIsInRunning: Boolean = isInRunning.getOrElse(false)
          def clearIsInRunning: LegPart = copy(isInRunning = None)
          def withIsInRunning(__v: Boolean): LegPart = copy(isInRunning = Some(__v))
          def getBettingSystemPreferred: Boolean = bettingSystemPreferred.getOrElse(false)
          def clearBettingSystemPreferred: LegPart = copy(bettingSystemPreferred = None)
          def withBettingSystemPreferred(__v: Boolean): LegPart = copy(bettingSystemPreferred = Some(__v))
          def getPlaceTerms: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms = placeTerms.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms.defaultInstance)
          def clearPlaceTerms: LegPart = copy(placeTerms = None)
          def withPlaceTerms(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms): LegPart = copy(placeTerms = Some(__v))
          def getFieldByNumber(__fieldNumber: Int): scala.Any = {
            (__fieldNumber: @_root_.scala.unchecked) match {
              case 1 =>
                outcomeRef.orNull
              case 2 =>
                marketRef.orNull
              case 3 =>
                eventRef.orNull
              case 8 =>
                places.orNull
              case 9 =>
                isInRunning.orNull
              case 10 =>
                bettingSystemPreferred.orNull
              case 11 =>
                placeTerms.orNull
            }
          }
          def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
            require(__field.containingMessage eq companion.scalaDescriptor)
            (__field.number: @_root_.scala.unchecked) match {
              case 1 =>
                outcomeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 2 =>
                marketRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 3 =>
                eventRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 8 =>
                places.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 9 =>
                isInRunning.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 10 =>
                bettingSystemPreferred.map(_root_.scalapb.descriptors.PBoolean).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 11 =>
                placeTerms.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            }
          }
          override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
          def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart
        }
        object LegPart extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart] {
          implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart] = this
          def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart = {
            require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
            val __fields = javaDescriptor.getFields
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[Boolean]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms]])
          }
          implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart] = _root_.scalapb.descriptors.Reads({
            case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
              require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(10).get).flatMap(_.as[scala.Option[Boolean]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(11).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms]]))
            case _ =>
              throw new RuntimeException("Expected PMessage")
          })
          def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.javaDescriptor.getNestedTypes.get(1)
          def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.scalaDescriptor.nestedMessages(1)
          def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
            var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
            (__number: @_root_.scala.unchecked) match {
              case 11 =>
                __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms
            }
            __out
          }
          lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms)
          def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
          lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart()
          @SerialVersionUID(0L) final case class PlaceTerms(num: scala.Option[Int] = None, den: scala.Option[Int] = None, places: scala.Option[Int] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[PlaceTerms] with com.trueaccord.lenses.Updatable[PlaceTerms] {
            @transient private[this] var __serializedSizeCachedValue: Int = 0
            private[this] def __computeSerializedValue(): Int = {
              var __size = 0
              if (num.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, num.get)
              }
              if (den.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, den.get)
              }
              if (places.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(3, places.get)
              }
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
              num.foreach {
                __v => _output__.writeInt32(1, __v)
              }
              den.foreach {
                __v => _output__.writeInt32(2, __v)
              }
              places.foreach {
                __v => _output__.writeInt32(3, __v)
              }
            }
            def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms = {
              var __num = this.num
              var __den = this.den
              var __places = this.places
              var _done__ = false
              while (!_done__) {
                val _tag__ = _input__.readTag()
                _tag__ match {
                  case 0 =>
                    _done__ = true
                  case 8 =>
                    __num = Some(_input__.readInt32())
                  case 16 =>
                    __den = Some(_input__.readInt32())
                  case 24 =>
                    __places = Some(_input__.readInt32())
                  case tag =>
                    _input__.skipField(tag)
                }
              }
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms(num = __num, den = __den, places = __places)
            }
            def getNum: Int = num.getOrElse(0)
            def clearNum: PlaceTerms = copy(num = None)
            def withNum(__v: Int): PlaceTerms = copy(num = Some(__v))
            def getDen: Int = den.getOrElse(0)
            def clearDen: PlaceTerms = copy(den = None)
            def withDen(__v: Int): PlaceTerms = copy(den = Some(__v))
            def getPlaces: Int = places.getOrElse(0)
            def clearPlaces: PlaceTerms = copy(places = None)
            def withPlaces(__v: Int): PlaceTerms = copy(places = Some(__v))
            def getFieldByNumber(__fieldNumber: Int): scala.Any = {
              (__fieldNumber: @_root_.scala.unchecked) match {
                case 1 =>
                  num.orNull
                case 2 =>
                  den.orNull
                case 3 =>
                  places.orNull
              }
            }
            def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
              require(__field.containingMessage eq companion.scalaDescriptor)
              (__field.number: @_root_.scala.unchecked) match {
                case 1 =>
                  num.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
                case 2 =>
                  den.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
                case 3 =>
                  places.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
              }
            }
            override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
            def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms
          }
          object PlaceTerms extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms] {
            implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms] = this
            def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms = {
              require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
              val __fields = javaDescriptor.getFields
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[Int]])
            }
            implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms] = _root_.scalapb.descriptors.Reads({
              case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
                require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
                parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[Int]]))
              case _ =>
                throw new RuntimeException("Expected PMessage")
            })
            def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.javaDescriptor.getNestedTypes.get(0)
            def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.scalaDescriptor.nestedMessages(0)
            def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
            lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
            def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
            lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms()
            implicit class PlaceTermsLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms](_l) {
              def num: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getNum)((c_, f_) => c_.copy(num = Some(f_)))
              def optionalNum: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.num)((c_, f_) => c_.copy(num = f_))
              def den: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getDen)((c_, f_) => c_.copy(den = Some(f_)))
              def optionalDen: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.den)((c_, f_) => c_.copy(den = f_))
              def places: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getPlaces)((c_, f_) => c_.copy(places = Some(f_)))
              def optionalPlaces: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.places)((c_, f_) => c_.copy(places = f_))
            }
            final val NUM_FIELD_NUMBER = 1
            final val DEN_FIELD_NUMBER = 2
            final val PLACES_FIELD_NUMBER = 3
          }
          implicit class LegPartLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart](_l) {
            def outcomeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOutcomeRef)((c_, f_) => c_.copy(outcomeRef = Some(f_)))
            def optionalOutcomeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.outcomeRef)((c_, f_) => c_.copy(outcomeRef = f_))
            def marketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getMarketRef)((c_, f_) => c_.copy(marketRef = Some(f_)))
            def optionalMarketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.marketRef)((c_, f_) => c_.copy(marketRef = f_))
            def eventRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getEventRef)((c_, f_) => c_.copy(eventRef = Some(f_)))
            def optionalEventRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.eventRef)((c_, f_) => c_.copy(eventRef = f_))
            def places: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPlaces)((c_, f_) => c_.copy(places = Some(f_)))
            def optionalPlaces: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.places)((c_, f_) => c_.copy(places = f_))
            def isInRunning: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsInRunning)((c_, f_) => c_.copy(isInRunning = Some(f_)))
            def optionalIsInRunning: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isInRunning)((c_, f_) => c_.copy(isInRunning = f_))
            def bettingSystemPreferred: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getBettingSystemPreferred)((c_, f_) => c_.copy(bettingSystemPreferred = Some(f_)))
            def optionalBettingSystemPreferred: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.bettingSystemPreferred)((c_, f_) => c_.copy(bettingSystemPreferred = f_))
            def placeTerms: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms] = field(_.getPlaceTerms)((c_, f_) => c_.copy(placeTerms = Some(f_)))
            def optionalPlaceTerms: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart.PlaceTerms]] = field(_.placeTerms)((c_, f_) => c_.copy(placeTerms = f_))
          }
          final val OUTCOMEREF_FIELD_NUMBER = 1
          final val MARKETREF_FIELD_NUMBER = 2
          final val EVENTREF_FIELD_NUMBER = 3
          final val PLACES_FIELD_NUMBER = 8
          final val ISINRUNNING_FIELD_NUMBER = 9
          final val BETTINGSYSTEMPREFERRED_FIELD_NUMBER = 10
          final val PLACETERMS_FIELD_NUMBER = 11
        }
        @SerialVersionUID(0L) final case class Handicap(handicapTypeRef: String, low: scala.Option[String] = None, high: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Handicap] with com.trueaccord.lenses.Updatable[Handicap] {
          @transient private[this] var __serializedSizeCachedValue: Int = 0
          private[this] def __computeSerializedValue(): Int = {
            var __size = 0
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, handicapTypeRef)
            if (low.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, low.get)
            }
            if (high.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, high.get)
            }
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
            _output__.writeString(1, handicapTypeRef)
            low.foreach {
              __v => _output__.writeString(2, __v)
            }
            high.foreach {
              __v => _output__.writeString(3, __v)
            }
          }
          def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap = {
            var __handicapTypeRef = this.handicapTypeRef
            var __low = this.low
            var __high = this.high
            var __requiredFields0: Long = 1L
            var _done__ = false
            while (!_done__) {
              val _tag__ = _input__.readTag()
              _tag__ match {
                case 0 =>
                  _done__ = true
                case 10 =>
                  __handicapTypeRef = _input__.readString()
                  __requiredFields0 &= -2L
                case 18 =>
                  __low = Some(_input__.readString())
                case 26 =>
                  __high = Some(_input__.readString())
                case tag =>
                  _input__.skipField(tag)
              }
            }
            if (__requiredFields0 != 0L) {
              throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
            }
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap(handicapTypeRef = __handicapTypeRef, low = __low, high = __high)
          }
          def withHandicapTypeRef(__v: String): Handicap = copy(handicapTypeRef = __v)
          def getLow: String = low.getOrElse("")
          def clearLow: Handicap = copy(low = None)
          def withLow(__v: String): Handicap = copy(low = Some(__v))
          def getHigh: String = high.getOrElse("")
          def clearHigh: Handicap = copy(high = None)
          def withHigh(__v: String): Handicap = copy(high = Some(__v))
          def getFieldByNumber(__fieldNumber: Int): scala.Any = {
            (__fieldNumber: @_root_.scala.unchecked) match {
              case 1 =>
                handicapTypeRef
              case 2 =>
                low.orNull
              case 3 =>
                high.orNull
            }
          }
          def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
            require(__field.containingMessage eq companion.scalaDescriptor)
            (__field.number: @_root_.scala.unchecked) match {
              case 1 =>
                _root_.scalapb.descriptors.PString(handicapTypeRef)
              case 2 =>
                low.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 3 =>
                high.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            }
          }
          override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
          def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap
        }
        object Handicap extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap] {
          implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap] = this
          def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap = {
            require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
            val __fields = javaDescriptor.getFields
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]])
          }
          implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap] = _root_.scalapb.descriptors.Reads({
            case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
              require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]))
            case _ =>
              throw new RuntimeException("Expected PMessage")
          })
          def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.javaDescriptor.getNestedTypes.get(2)
          def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.scalaDescriptor.nestedMessages(2)
          def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
          lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
          def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
          lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap(handicapTypeRef = "")
          implicit class HandicapLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap](_l) {
            def handicapTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.handicapTypeRef)((c_, f_) => c_.copy(handicapTypeRef = f_))
            def low: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getLow)((c_, f_) => c_.copy(low = Some(f_)))
            def optionalLow: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.low)((c_, f_) => c_.copy(low = f_))
            def high: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getHigh)((c_, f_) => c_.copy(high = Some(f_)))
            def optionalHigh: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.high)((c_, f_) => c_.copy(high = f_))
          }
          final val HANDICAPTYPEREF_FIELD_NUMBER = 1
          final val LOW_FIELD_NUMBER = 2
          final val HIGH_FIELD_NUMBER = 3
        }
        implicit class LegLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg](_l) {
          def price: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price] = field(_.getPrice)((c_, f_) => c_.copy(price = Some(f_)))
          def optionalPrice: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Price]] = field(_.price)((c_, f_) => c_.copy(price = f_))
          def winPlaceRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getWinPlaceRef)((c_, f_) => c_.copy(winPlaceRef = Some(f_)))
          def optionalWinPlaceRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.winPlaceRef)((c_, f_) => c_.copy(winPlaceRef = f_))
          def legParts: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.LegPart]] = field(_.legParts)((c_, f_) => c_.copy(legParts = f_))
          def result: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getResult)((c_, f_) => c_.copy(result = Some(f_)))
          def optionalResult: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.result)((c_, f_) => c_.copy(result = f_))
          def poolId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPoolId)((c_, f_) => c_.copy(poolId = Some(f_)))
          def optionalPoolId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.poolId)((c_, f_) => c_.copy(poolId = f_))
          def legSort: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getLegSort)((c_, f_) => c_.copy(legSort = Some(f_)))
          def optionalLegSort: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.legSort)((c_, f_) => c_.copy(legSort = f_))
          def index: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getIndex)((c_, f_) => c_.copy(index = Some(f_)))
          def optionalIndex: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.index)((c_, f_) => c_.copy(index = f_))
          def handicap: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap] = field(_.getHandicap)((c_, f_) => c_.copy(handicap = Some(f_)))
          def optionalHandicap: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg.Handicap]] = field(_.handicap)((c_, f_) => c_.copy(handicap = f_))
          def noCombi: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getNoCombi)((c_, f_) => c_.copy(noCombi = Some(f_)))
          def optionalNoCombi: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.noCombi)((c_, f_) => c_.copy(noCombi = f_))
        }
        final val PRICE_FIELD_NUMBER = 1
        final val WINPLACEREF_FIELD_NUMBER = 2
        final val LEGPARTS_FIELD_NUMBER = 3
        final val RESULT_FIELD_NUMBER = 4
        final val POOLID_FIELD_NUMBER = 5
        final val LEGSORT_FIELD_NUMBER = 6
        final val INDEX_FIELD_NUMBER = 7
        final val HANDICAP_FIELD_NUMBER = 8
        final val NOCOMBI_FIELD_NUMBER = 9
      }
      @SerialVersionUID(0L) final case class BetTermChange(betId: String, changeTime: scala.Option[String] = None, legNumber: scala.Option[String] = None, legChange: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[BetTermChange] with com.trueaccord.lenses.Updatable[BetTermChange] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, betId)
          if (changeTime.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, changeTime.get)
          }
          if (legNumber.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, legNumber.get)
          }
          if (legChange.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(legChange.get.serializedSize) + legChange.get.serializedSize
          }
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
          _output__.writeString(1, betId)
          changeTime.foreach {
            __v => _output__.writeString(2, __v)
          }
          legNumber.foreach {
            __v => _output__.writeString(3, __v)
          }
          legChange.foreach { __v =>
            _output__.writeTag(4, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange = {
          var __betId = this.betId
          var __changeTime = this.changeTime
          var __legNumber = this.legNumber
          var __legChange = this.legChange
          var __requiredFields0: Long = 1L
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __betId = _input__.readString()
                __requiredFields0 &= -2L
              case 18 =>
                __changeTime = Some(_input__.readString())
              case 26 =>
                __legNumber = Some(_input__.readString())
              case 34 =>
                __legChange = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __legChange.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.defaultInstance)))
              case tag =>
                _input__.skipField(tag)
            }
          }
          if (__requiredFields0 != 0L) {
            throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange(betId = __betId, changeTime = __changeTime, legNumber = __legNumber, legChange = __legChange)
        }
        def withBetId(__v: String): BetTermChange = copy(betId = __v)
        def getChangeTime: String = changeTime.getOrElse("")
        def clearChangeTime: BetTermChange = copy(changeTime = None)
        def withChangeTime(__v: String): BetTermChange = copy(changeTime = Some(__v))
        def getLegNumber: String = legNumber.getOrElse("")
        def clearLegNumber: BetTermChange = copy(legNumber = None)
        def withLegNumber(__v: String): BetTermChange = copy(legNumber = Some(__v))
        def getLegChange: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange = legChange.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.defaultInstance)
        def clearLegChange: BetTermChange = copy(legChange = None)
        def withLegChange(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange): BetTermChange = copy(legChange = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              betId
            case 2 =>
              changeTime.orNull
            case 3 =>
              legNumber.orNull
            case 4 =>
              legChange.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              _root_.scalapb.descriptors.PString(betId)
            case 2 =>
              changeTime.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              legNumber.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              legChange.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange
      }
      object BetTermChange extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(4)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(4)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
          var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
          (__number: @_root_.scala.unchecked) match {
            case 4 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange
          }
          __out
        }
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange)
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange(betId = "")
        @SerialVersionUID(0L) final case class LegChange(price: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[LegChange] with com.trueaccord.lenses.Updatable[LegChange] {
          @transient private[this] var __serializedSizeCachedValue: Int = 0
          private[this] def __computeSerializedValue(): Int = {
            var __size = 0
            if (price.isDefined) {
              __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(price.get.serializedSize) + price.get.serializedSize
            }
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
            price.foreach { __v =>
              _output__.writeTag(1, 2)
              _output__.writeUInt32NoTag(__v.serializedSize)
              __v.writeTo(_output__)
            }
          }
          def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange = {
            var __price = this.price
            var _done__ = false
            while (!_done__) {
              val _tag__ = _input__.readTag()
              _tag__ match {
                case 0 =>
                  _done__ = true
                case 10 =>
                  __price = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __price.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price.defaultInstance)))
                case tag =>
                  _input__.skipField(tag)
              }
            }
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange(price = __price)
          }
          def getPrice: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price = price.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price.defaultInstance)
          def clearPrice: LegChange = copy(price = None)
          def withPrice(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price): LegChange = copy(price = Some(__v))
          def getFieldByNumber(__fieldNumber: Int): scala.Any = {
            (__fieldNumber: @_root_.scala.unchecked) match {
              case 1 =>
                price.orNull
            }
          }
          def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
            require(__field.containingMessage eq companion.scalaDescriptor)
            (__field.number: @_root_.scala.unchecked) match {
              case 1 =>
                price.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
            }
          }
          override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
          def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange
        }
        object LegChange extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange] {
          implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange] = this
          def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange = {
            require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
            val __fields = javaDescriptor.getFields
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price]])
          }
          implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange] = _root_.scalapb.descriptors.Reads({
            case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
              require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price]]))
            case _ =>
              throw new RuntimeException("Expected PMessage")
          })
          def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.javaDescriptor.getNestedTypes.get(0)
          def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.scalaDescriptor.nestedMessages(0)
          def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
            var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
            (__number: @_root_.scala.unchecked) match {
              case 1 =>
                __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price
            }
            __out
          }
          lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price)
          def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
          lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange()
          @SerialVersionUID(0L) final case class Price(num: scala.Option[Int] = None, den: scala.Option[Int] = None, priceTypeRef: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Price] with com.trueaccord.lenses.Updatable[Price] {
            @transient private[this] var __serializedSizeCachedValue: Int = 0
            private[this] def __computeSerializedValue(): Int = {
              var __size = 0
              if (num.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, num.get)
              }
              if (den.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, den.get)
              }
              if (priceTypeRef.isDefined) {
                __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, priceTypeRef.get)
              }
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
              num.foreach {
                __v => _output__.writeInt32(1, __v)
              }
              den.foreach {
                __v => _output__.writeInt32(2, __v)
              }
              priceTypeRef.foreach {
                __v => _output__.writeString(3, __v)
              }
            }
            def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price = {
              var __num = this.num
              var __den = this.den
              var __priceTypeRef = this.priceTypeRef
              var _done__ = false
              while (!_done__) {
                val _tag__ = _input__.readTag()
                _tag__ match {
                  case 0 =>
                    _done__ = true
                  case 8 =>
                    __num = Some(_input__.readInt32())
                  case 16 =>
                    __den = Some(_input__.readInt32())
                  case 26 =>
                    __priceTypeRef = Some(_input__.readString())
                  case tag =>
                    _input__.skipField(tag)
                }
              }
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price(num = __num, den = __den, priceTypeRef = __priceTypeRef)
            }
            def getNum: Int = num.getOrElse(0)
            def clearNum: Price = copy(num = None)
            def withNum(__v: Int): Price = copy(num = Some(__v))
            def getDen: Int = den.getOrElse(0)
            def clearDen: Price = copy(den = None)
            def withDen(__v: Int): Price = copy(den = Some(__v))
            def getPriceTypeRef: String = priceTypeRef.getOrElse("")
            def clearPriceTypeRef: Price = copy(priceTypeRef = None)
            def withPriceTypeRef(__v: String): Price = copy(priceTypeRef = Some(__v))
            def getFieldByNumber(__fieldNumber: Int): scala.Any = {
              (__fieldNumber: @_root_.scala.unchecked) match {
                case 1 =>
                  num.orNull
                case 2 =>
                  den.orNull
                case 3 =>
                  priceTypeRef.orNull
              }
            }
            def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
              require(__field.containingMessage eq companion.scalaDescriptor)
              (__field.number: @_root_.scala.unchecked) match {
                case 1 =>
                  num.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
                case 2 =>
                  den.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
                case 3 =>
                  priceTypeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
              }
            }
            override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
            def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price
          }
          object Price extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price] {
            implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price] = this
            def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price = {
              require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
              val __fields = javaDescriptor.getFields
              parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]])
            }
            implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price] = _root_.scalapb.descriptors.Reads({
              case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
                require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
                parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]))
              case _ =>
                throw new RuntimeException("Expected PMessage")
            })
            def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.javaDescriptor.getNestedTypes.get(0)
            def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.scalaDescriptor.nestedMessages(0)
            def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
            lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
            def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
            lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price()
            implicit class PriceLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price](_l) {
              def num: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getNum)((c_, f_) => c_.copy(num = Some(f_)))
              def optionalNum: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.num)((c_, f_) => c_.copy(num = f_))
              def den: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getDen)((c_, f_) => c_.copy(den = Some(f_)))
              def optionalDen: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.den)((c_, f_) => c_.copy(den = f_))
              def priceTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPriceTypeRef)((c_, f_) => c_.copy(priceTypeRef = Some(f_)))
              def optionalPriceTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.priceTypeRef)((c_, f_) => c_.copy(priceTypeRef = f_))
            }
            final val NUM_FIELD_NUMBER = 1
            final val DEN_FIELD_NUMBER = 2
            final val PRICETYPEREF_FIELD_NUMBER = 3
          }
          implicit class LegChangeLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange](_l) {
            def price: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price] = field(_.getPrice)((c_, f_) => c_.copy(price = Some(f_)))
            def optionalPrice: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange.Price]] = field(_.price)((c_, f_) => c_.copy(price = f_))
          }
          final val PRICE_FIELD_NUMBER = 1
        }
        implicit class BetTermChangeLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange](_l) {
          def betId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.betId)((c_, f_) => c_.copy(betId = f_))
          def changeTime: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getChangeTime)((c_, f_) => c_.copy(changeTime = Some(f_)))
          def optionalChangeTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.changeTime)((c_, f_) => c_.copy(changeTime = f_))
          def legNumber: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getLegNumber)((c_, f_) => c_.copy(legNumber = Some(f_)))
          def optionalLegNumber: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.legNumber)((c_, f_) => c_.copy(legNumber = f_))
          def legChange: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange] = field(_.getLegChange)((c_, f_) => c_.copy(legChange = Some(f_)))
          def optionalLegChange: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange.LegChange]] = field(_.legChange)((c_, f_) => c_.copy(legChange = f_))
        }
        final val BETID_FIELD_NUMBER = 1
        final val CHANGETIME_FIELD_NUMBER = 2
        final val LEGNUMBER_FIELD_NUMBER = 3
        final val LEGCHANGE_FIELD_NUMBER = 4
      }
      @SerialVersionUID(0L) final case class PoolBetSystem(betSystemRef: scala.Option[String] = None, customLineRef: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[PoolBetSystem] with com.trueaccord.lenses.Updatable[PoolBetSystem] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (betSystemRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, betSystemRef.get)
          }
          if (customLineRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, customLineRef.get)
          }
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
          betSystemRef.foreach {
            __v => _output__.writeString(1, __v)
          }
          customLineRef.foreach {
            __v => _output__.writeString(2, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem = {
          var __betSystemRef = this.betSystemRef
          var __customLineRef = this.customLineRef
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __betSystemRef = Some(_input__.readString())
              case 18 =>
                __customLineRef = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem(betSystemRef = __betSystemRef, customLineRef = __customLineRef)
        }
        def getBetSystemRef: String = betSystemRef.getOrElse("")
        def clearBetSystemRef: PoolBetSystem = copy(betSystemRef = None)
        def withBetSystemRef(__v: String): PoolBetSystem = copy(betSystemRef = Some(__v))
        def getCustomLineRef: String = customLineRef.getOrElse("")
        def clearCustomLineRef: PoolBetSystem = copy(customLineRef = None)
        def withCustomLineRef(__v: String): PoolBetSystem = copy(customLineRef = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              betSystemRef.orNull
            case 2 =>
              customLineRef.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              betSystemRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              customLineRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem
      }
      object PoolBetSystem extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(5)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(5)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem()
        implicit class PoolBetSystemLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem](_l) {
          def betSystemRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getBetSystemRef)((c_, f_) => c_.copy(betSystemRef = Some(f_)))
          def optionalBetSystemRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.betSystemRef)((c_, f_) => c_.copy(betSystemRef = f_))
          def customLineRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCustomLineRef)((c_, f_) => c_.copy(customLineRef = Some(f_)))
          def optionalCustomLineRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.customLineRef)((c_, f_) => c_.copy(customLineRef = f_))
        }
        final val BETSYSTEMREF_FIELD_NUMBER = 1
        final val CUSTOMLINEREF_FIELD_NUMBER = 2
      }
      @SerialVersionUID(0L) final case class BetOverride(id: String, operatorRef: scala.Option[String] = None, creationDate: scala.Option[String] = None, action: scala.Option[String] = None, callRef: scala.Option[String] = None, reason: scala.Option[String] = None, legNo: scala.Option[Int] = None, partNo: scala.Option[Int] = None, refId: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[BetOverride] with com.trueaccord.lenses.Updatable[BetOverride] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, id)
          if (operatorRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, operatorRef.get)
          }
          if (creationDate.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, creationDate.get)
          }
          if (action.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, action.get)
          }
          if (callRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, callRef.get)
          }
          if (reason.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, reason.get)
          }
          if (legNo.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(7, legNo.get)
          }
          if (partNo.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(8, partNo.get)
          }
          if (refId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(9, refId.get)
          }
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
          _output__.writeString(1, id)
          operatorRef.foreach {
            __v => _output__.writeString(2, __v)
          }
          creationDate.foreach {
            __v => _output__.writeString(3, __v)
          }
          action.foreach {
            __v => _output__.writeString(4, __v)
          }
          callRef.foreach {
            __v => _output__.writeString(5, __v)
          }
          reason.foreach {
            __v => _output__.writeString(6, __v)
          }
          legNo.foreach {
            __v => _output__.writeInt32(7, __v)
          }
          partNo.foreach {
            __v => _output__.writeInt32(8, __v)
          }
          refId.foreach {
            __v => _output__.writeString(9, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride = {
          var __id = this.id
          var __operatorRef = this.operatorRef
          var __creationDate = this.creationDate
          var __action = this.action
          var __callRef = this.callRef
          var __reason = this.reason
          var __legNo = this.legNo
          var __partNo = this.partNo
          var __refId = this.refId
          var __requiredFields0: Long = 1L
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __id = _input__.readString()
                __requiredFields0 &= -2L
              case 18 =>
                __operatorRef = Some(_input__.readString())
              case 26 =>
                __creationDate = Some(_input__.readString())
              case 34 =>
                __action = Some(_input__.readString())
              case 42 =>
                __callRef = Some(_input__.readString())
              case 50 =>
                __reason = Some(_input__.readString())
              case 56 =>
                __legNo = Some(_input__.readInt32())
              case 64 =>
                __partNo = Some(_input__.readInt32())
              case 74 =>
                __refId = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          if (__requiredFields0 != 0L) {
            throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride(id = __id, operatorRef = __operatorRef, creationDate = __creationDate, action = __action, callRef = __callRef, reason = __reason, legNo = __legNo, partNo = __partNo, refId = __refId)
        }
        def withId(__v: String): BetOverride = copy(id = __v)
        def getOperatorRef: String = operatorRef.getOrElse("")
        def clearOperatorRef: BetOverride = copy(operatorRef = None)
        def withOperatorRef(__v: String): BetOverride = copy(operatorRef = Some(__v))
        def getCreationDate: String = creationDate.getOrElse("")
        def clearCreationDate: BetOverride = copy(creationDate = None)
        def withCreationDate(__v: String): BetOverride = copy(creationDate = Some(__v))
        def getAction: String = action.getOrElse("")
        def clearAction: BetOverride = copy(action = None)
        def withAction(__v: String): BetOverride = copy(action = Some(__v))
        def getCallRef: String = callRef.getOrElse("")
        def clearCallRef: BetOverride = copy(callRef = None)
        def withCallRef(__v: String): BetOverride = copy(callRef = Some(__v))
        def getReason: String = reason.getOrElse("")
        def clearReason: BetOverride = copy(reason = None)
        def withReason(__v: String): BetOverride = copy(reason = Some(__v))
        def getLegNo: Int = legNo.getOrElse(0)
        def clearLegNo: BetOverride = copy(legNo = None)
        def withLegNo(__v: Int): BetOverride = copy(legNo = Some(__v))
        def getPartNo: Int = partNo.getOrElse(0)
        def clearPartNo: BetOverride = copy(partNo = None)
        def withPartNo(__v: Int): BetOverride = copy(partNo = Some(__v))
        def getRefId: String = refId.getOrElse("")
        def clearRefId: BetOverride = copy(refId = None)
        def withRefId(__v: String): BetOverride = copy(refId = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              id
            case 2 =>
              operatorRef.orNull
            case 3 =>
              creationDate.orNull
            case 4 =>
              action.orNull
            case 5 =>
              callRef.orNull
            case 6 =>
              reason.orNull
            case 7 =>
              legNo.orNull
            case 8 =>
              partNo.orNull
            case 9 =>
              refId.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              _root_.scalapb.descriptors.PString(id)
            case 2 =>
              operatorRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              creationDate.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              action.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 5 =>
              callRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 6 =>
              reason.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 7 =>
              legNo.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 8 =>
              partNo.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 9 =>
              refId.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride
      }
      object BetOverride extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(7)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(8)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.javaDescriptor.getNestedTypes.get(6)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.scalaDescriptor.nestedMessages(6)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride(id = "")
        implicit class BetOverrideLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride](_l) {
          def id: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.id)((c_, f_) => c_.copy(id = f_))
          def operatorRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOperatorRef)((c_, f_) => c_.copy(operatorRef = Some(f_)))
          def optionalOperatorRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.operatorRef)((c_, f_) => c_.copy(operatorRef = f_))
          def creationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCreationDate)((c_, f_) => c_.copy(creationDate = Some(f_)))
          def optionalCreationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.creationDate)((c_, f_) => c_.copy(creationDate = f_))
          def action: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getAction)((c_, f_) => c_.copy(action = Some(f_)))
          def optionalAction: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.action)((c_, f_) => c_.copy(action = f_))
          def callRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCallRef)((c_, f_) => c_.copy(callRef = Some(f_)))
          def optionalCallRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.callRef)((c_, f_) => c_.copy(callRef = f_))
          def reason: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getReason)((c_, f_) => c_.copy(reason = Some(f_)))
          def optionalReason: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.reason)((c_, f_) => c_.copy(reason = f_))
          def legNo: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getLegNo)((c_, f_) => c_.copy(legNo = Some(f_)))
          def optionalLegNo: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.legNo)((c_, f_) => c_.copy(legNo = f_))
          def partNo: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getPartNo)((c_, f_) => c_.copy(partNo = Some(f_)))
          def optionalPartNo: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.partNo)((c_, f_) => c_.copy(partNo = f_))
          def refId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getRefId)((c_, f_) => c_.copy(refId = Some(f_)))
          def optionalRefId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.refId)((c_, f_) => c_.copy(refId = f_))
        }
        final val ID_FIELD_NUMBER = 1
        final val OPERATORREF_FIELD_NUMBER = 2
        final val CREATIONDATE_FIELD_NUMBER = 3
        final val ACTION_FIELD_NUMBER = 4
        final val CALLREF_FIELD_NUMBER = 5
        final val REASON_FIELD_NUMBER = 6
        final val LEGNO_FIELD_NUMBER = 7
        final val PARTNO_FIELD_NUMBER = 8
        final val REFID_FIELD_NUMBER = 9
      }
      implicit class BetLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet](_l) {
        def id: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.id)((c_, f_) => c_.copy(id = f_))
        def creationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCreationDate)((c_, f_) => c_.copy(creationDate = Some(f_)))
        def optionalCreationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.creationDate)((c_, f_) => c_.copy(creationDate = f_))
        def accountRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getAccountRef)((c_, f_) => c_.copy(accountRef = Some(f_)))
        def optionalAccountRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.accountRef)((c_, f_) => c_.copy(accountRef = f_))
        def customerRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCustomerRef)((c_, f_) => c_.copy(customerRef = Some(f_)))
        def optionalCustomerRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.customerRef)((c_, f_) => c_.copy(customerRef = f_))
        def source: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Source] = field(_.getSource)((c_, f_) => c_.copy(source = Some(f_)))
        def optionalSource: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Source]] = field(_.source)((c_, f_) => c_.copy(source = f_))
        def externalUID: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef]] = field(_.externalUID)((c_, f_) => c_.copy(externalUID = f_))
        def betTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getBetTypeRef)((c_, f_) => c_.copy(betTypeRef = Some(f_)))
        def optionalBetTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.betTypeRef)((c_, f_) => c_.copy(betTypeRef = f_))
        def placedAt: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPlacedAt)((c_, f_) => c_.copy(placedAt = Some(f_)))
        def optionalPlacedAt: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.placedAt)((c_, f_) => c_.copy(placedAt = f_))
        def receipt: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getReceipt)((c_, f_) => c_.copy(receipt = Some(f_)))
        def optionalReceipt: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.receipt)((c_, f_) => c_.copy(receipt = f_))
        def isSettled: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsSettled)((c_, f_) => c_.copy(isSettled = Some(f_)))
        def optionalIsSettled: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isSettled)((c_, f_) => c_.copy(isSettled = f_))
        def isConfirmed: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsConfirmed)((c_, f_) => c_.copy(isConfirmed = Some(f_)))
        def optionalIsConfirmed: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isConfirmed)((c_, f_) => c_.copy(isConfirmed = f_))
        def isCancelled: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsCancelled)((c_, f_) => c_.copy(isCancelled = Some(f_)))
        def optionalIsCancelled: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isCancelled)((c_, f_) => c_.copy(isCancelled = f_))
        def isCashedOut: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsCashedOut)((c_, f_) => c_.copy(isCashedOut = Some(f_)))
        def optionalIsCashedOut: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isCashedOut)((c_, f_) => c_.copy(isCashedOut = f_))
        def isPoolBet: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsPoolBet)((c_, f_) => c_.copy(isPoolBet = Some(f_)))
        def optionalIsPoolBet: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isPoolBet)((c_, f_) => c_.copy(isPoolBet = f_))
        def settledAt: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getSettledAt)((c_, f_) => c_.copy(settledAt = Some(f_)))
        def optionalSettledAt: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.settledAt)((c_, f_) => c_.copy(settledAt = f_))
        def settledHow: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getSettledHow)((c_, f_) => c_.copy(settledHow = Some(f_)))
        def optionalSettledHow: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.settledHow)((c_, f_) => c_.copy(settledHow = f_))
        def placedByCustomerRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPlacedByCustomerRef)((c_, f_) => c_.copy(placedByCustomerRef = Some(f_)))
        def optionalPlacedByCustomerRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.placedByCustomerRef)((c_, f_) => c_.copy(placedByCustomerRef = f_))
        def isParked: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsParked)((c_, f_) => c_.copy(isParked = Some(f_)))
        def optionalIsParked: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isParked)((c_, f_) => c_.copy(isParked = f_))
        def stake: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = field(_.getStake)((c_, f_) => c_.copy(stake = Some(f_)))
        def optionalStake: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]] = field(_.stake)((c_, f_) => c_.copy(stake = f_))
        def poolStake: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = field(_.getPoolStake)((c_, f_) => c_.copy(poolStake = Some(f_)))
        def optionalPoolStake: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]] = field(_.poolStake)((c_, f_) => c_.copy(poolStake = f_))
        def payout: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout] = field(_.getPayout)((c_, f_) => c_.copy(payout = Some(f_)))
        def optionalPayout: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Payout]] = field(_.payout)((c_, f_) => c_.copy(payout = f_))
        def lines: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines] = field(_.getLines)((c_, f_) => c_.copy(lines = Some(f_)))
        def optionalLines: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Lines]] = field(_.lines)((c_, f_) => c_.copy(lines = f_))
        def leg: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Leg]] = field(_.leg)((c_, f_) => c_.copy(leg = f_))
        def betTermChange: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetTermChange]] = field(_.betTermChange)((c_, f_) => c_.copy(betTermChange = f_))
        def poolBetSystem: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem] = field(_.getPoolBetSystem)((c_, f_) => c_.copy(poolBetSystem = Some(f_)))
        def optionalPoolBetSystem: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.PoolBetSystem]] = field(_.poolBetSystem)((c_, f_) => c_.copy(poolBetSystem = f_))
        def poolBetSubscriptionRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPoolBetSubscriptionRef)((c_, f_) => c_.copy(poolBetSubscriptionRef = Some(f_)))
        def optionalPoolBetSubscriptionRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.poolBetSubscriptionRef)((c_, f_) => c_.copy(poolBetSubscriptionRef = f_))
        def isPending: _root_.com.trueaccord.lenses.Lens[UpperPB, Boolean] = field(_.getIsPending)((c_, f_) => c_.copy(isPending = Some(f_)))
        def optionalIsPending: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Boolean]] = field(_.isPending)((c_, f_) => c_.copy(isPending = f_))
        def betOverrides: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.BetOverride]] = field(_.betOverrides)((c_, f_) => c_.copy(betOverrides = f_))
      }
      final val ID_FIELD_NUMBER = 1
      final val CREATIONDATE_FIELD_NUMBER = 2
      final val ACCOUNTREF_FIELD_NUMBER = 3
      final val CUSTOMERREF_FIELD_NUMBER = 4
      final val SOURCE_FIELD_NUMBER = 5
      final val EXTERNALUID_FIELD_NUMBER = 22
      final val BETTYPEREF_FIELD_NUMBER = 6
      final val PLACEDAT_FIELD_NUMBER = 7
      final val RECEIPT_FIELD_NUMBER = 8
      final val ISSETTLED_FIELD_NUMBER = 9
      final val ISCONFIRMED_FIELD_NUMBER = 10
      final val ISCANCELLED_FIELD_NUMBER = 11
      final val ISCASHEDOUT_FIELD_NUMBER = 12
      final val ISPOOLBET_FIELD_NUMBER = 13
      final val SETTLEDAT_FIELD_NUMBER = 14
      final val SETTLEDHOW_FIELD_NUMBER = 15
      final val PLACEDBYCUSTOMERREF_FIELD_NUMBER = 20
      final val ISPARKED_FIELD_NUMBER = 21
      final val STAKE_FIELD_NUMBER = 30
      final val POOLSTAKE_FIELD_NUMBER = 31
      final val PAYOUT_FIELD_NUMBER = 40
      final val LINES_FIELD_NUMBER = 50
      final val LEG_FIELD_NUMBER = 60
      final val BETTERMCHANGE_FIELD_NUMBER = 61
      final val POOLBETSYSTEM_FIELD_NUMBER = 62
      final val POOLBETSUBSCRIPTIONREF_FIELD_NUMBER = 63
      final val ISPENDING_FIELD_NUMBER = 64
      final val BETOVERRIDES_FIELD_NUMBER = 65
    }
    @SerialVersionUID(0L) final case class BetSlip(id: String, accountRef: scala.Option[String] = None, creationDate: scala.Option[String] = None, totalStake: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[BetSlip] with com.trueaccord.lenses.Updatable[BetSlip] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, id)
        if (accountRef.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, accountRef.get)
        }
        if (creationDate.isDefined) {
          __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, creationDate.get)
        }
        if (totalStake.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(totalStake.get.serializedSize) + totalStake.get.serializedSize
        }
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
        _output__.writeString(1, id)
        accountRef.foreach {
          __v => _output__.writeString(2, __v)
        }
        creationDate.foreach {
          __v => _output__.writeString(3, __v)
        }
        totalStake.foreach { __v =>
          _output__.writeTag(4, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip = {
        var __id = this.id
        var __accountRef = this.accountRef
        var __creationDate = this.creationDate
        var __totalStake = this.totalStake
        var __requiredFields0: Long = 1L
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __id = _input__.readString()
              __requiredFields0 &= -2L
            case 18 =>
              __accountRef = Some(_input__.readString())
            case 26 =>
              __creationDate = Some(_input__.readString())
            case 34 =>
              __totalStake = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __totalStake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)))
            case tag =>
              _input__.skipField(tag)
          }
        }
        if (__requiredFields0 != 0L) {
          throw new _root_.com.google.protobuf.InvalidProtocolBufferException("Message missing required fields.")
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip(id = __id, accountRef = __accountRef, creationDate = __creationDate, totalStake = __totalStake)
      }
      def withId(__v: String): BetSlip = copy(id = __v)
      def getAccountRef: String = accountRef.getOrElse("")
      def clearAccountRef: BetSlip = copy(accountRef = None)
      def withAccountRef(__v: String): BetSlip = copy(accountRef = Some(__v))
      def getCreationDate: String = creationDate.getOrElse("")
      def clearCreationDate: BetSlip = copy(creationDate = None)
      def withCreationDate(__v: String): BetSlip = copy(creationDate = Some(__v))
      def getTotalStake: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake = totalStake.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake.defaultInstance)
      def clearTotalStake: BetSlip = copy(totalStake = None)
      def withTotalStake(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake): BetSlip = copy(totalStake = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            id
          case 2 =>
            accountRef.orNull
          case 3 =>
            creationDate.orNull
          case 4 =>
            totalStake.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PString(id)
          case 2 =>
            accountRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 3 =>
            creationDate.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 4 =>
            totalStake.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip
    }
    object BetSlip extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip(__fieldsMap(__fields.get(0)).asInstanceOf[String], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).get.as[String], __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(9)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(9)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 4 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip(id = "")
      implicit class BetSlipLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip](_l) {
        def id: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.id)((c_, f_) => c_.copy(id = f_))
        def accountRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getAccountRef)((c_, f_) => c_.copy(accountRef = Some(f_)))
        def optionalAccountRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.accountRef)((c_, f_) => c_.copy(accountRef = f_))
        def creationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCreationDate)((c_, f_) => c_.copy(creationDate = Some(f_)))
        def optionalCreationDate: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.creationDate)((c_, f_) => c_.copy(creationDate = f_))
        def totalStake: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake] = field(_.getTotalStake)((c_, f_) => c_.copy(totalStake = Some(f_)))
        def optionalTotalStake: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet.Stake]] = field(_.totalStake)((c_, f_) => c_.copy(totalStake = f_))
      }
      final val ID_FIELD_NUMBER = 1
      final val ACCOUNTREF_FIELD_NUMBER = 2
      final val CREATIONDATE_FIELD_NUMBER = 3
      final val TOTALSTAKE_FIELD_NUMBER = 4
    }
    @SerialVersionUID(0L) final case class BetState(bet: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] = None, bets: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] = List.empty, betSlip: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[BetState] with com.trueaccord.lenses.Updatable[BetState] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        if (bet.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(bet.get.serializedSize) + bet.get.serializedSize
        }
        bets.foreach(bets => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(bets.serializedSize) + bets.serializedSize)
        if (betSlip.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(betSlip.get.serializedSize) + betSlip.get.serializedSize
        }
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
        bet.foreach { __v =>
          _output__.writeTag(1, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        bets.foreach { __v =>
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        betSlip.foreach { __v =>
          _output__.writeTag(3, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.BetState = {
        var __bet = this.bet
        val __bets = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.Bet] ++= this.bets
        var __betSlip = this.betSlip
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __bet = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __bet.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.defaultInstance)))
            case 18 =>
              __bets += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.Bet.defaultInstance)
            case 26 =>
              __betSlip = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __betSlip.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip.defaultInstance)))
            case tag =>
              _input__.skipField(tag)
          }
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.BetState(bet = __bet, bets = __bets.result(), betSlip = __betSlip)
      }
      def getBet: parallelai.sot.executor.builder.SOTBuilder.gen.Bet = bet.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.Bet.defaultInstance)
      def clearBet: BetState = copy(bet = None)
      def withBet(__v: parallelai.sot.executor.builder.SOTBuilder.gen.Bet): BetState = copy(bet = Some(__v))
      def clearBets = copy(bets = List.empty)
      def addBets(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.Bet*): BetState = addAllBets(__vs)
      def addAllBets(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]): BetState = copy(bets = bets ++ __vs)
      def withBets(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]): BetState = copy(bets = __v)
      def getBetSlip: parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip = betSlip.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip.defaultInstance)
      def clearBetSlip: BetState = copy(betSlip = None)
      def withBetSlip(__v: parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip): BetState = copy(betSlip = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            bet.orNull
          case 2 =>
            bets
          case 3 =>
            betSlip.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            bet.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          case 2 =>
            _root_.scalapb.descriptors.PRepeated(bets.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 3 =>
            betSlip.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.BetState
    }
    object BetState extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.BetState] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.BetState] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.BetState = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.BetState(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]], __fieldsMap.getOrElse(__fields.get(1), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.BetState] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.BetState(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(10)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(10)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 1 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet
          case 2 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.Bet
          case 3 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.BetState()
      implicit class BetStateLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetState]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetState](_l) {
        def bet: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.Bet] = field(_.getBet)((c_, f_) => c_.copy(bet = Some(f_)))
        def optionalBet: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]] = field(_.bet)((c_, f_) => c_.copy(bet = f_))
        def bets: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.Bet]] = field(_.bets)((c_, f_) => c_.copy(bets = f_))
        def betSlip: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip] = field(_.getBetSlip)((c_, f_) => c_.copy(betSlip = Some(f_)))
        def optionalBetSlip: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip]] = field(_.betSlip)((c_, f_) => c_.copy(betSlip = f_))
      }
      final val BET_FIELD_NUMBER = 1
      final val BETS_FIELD_NUMBER = 2
      final val BETSLIP_FIELD_NUMBER = 3
    }
    @SerialVersionUID(0L) final case class SupportingState(outcome: List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome] = List.empty, pool: List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool] = List.empty, customer: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[SupportingState] with com.trueaccord.lenses.Updatable[SupportingState] {
      @transient private[this] var __serializedSizeCachedValue: Int = 0
      private[this] def __computeSerializedValue(): Int = {
        var __size = 0
        outcome.foreach(outcome => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(outcome.serializedSize) + outcome.serializedSize)
        pool.foreach(pool => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(pool.serializedSize) + pool.serializedSize)
        if (customer.isDefined) {
          __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(customer.get.serializedSize) + customer.get.serializedSize
        }
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
        outcome.foreach { __v =>
          _output__.writeTag(1, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        pool.foreach { __v =>
          _output__.writeTag(2, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
        customer.foreach { __v =>
          _output__.writeTag(3, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        }
      }
      def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState = {
        val __outcome = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome] ++= this.outcome
        val __pool = List.newBuilder[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool] ++= this.pool
        var __customer = this.customer
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 =>
              _done__ = true
            case 10 =>
              __outcome += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.defaultInstance)
            case 18 =>
              __pool += _root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool.defaultInstance)
            case 26 =>
              __customer = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __customer.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer.defaultInstance)))
            case tag =>
              _input__.skipField(tag)
          }
        }
        parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState(outcome = __outcome.result(), pool = __pool.result(), customer = __customer)
      }
      def clearOutcome = copy(outcome = List.empty)
      def addOutcome(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome*): SupportingState = addAllOutcome(__vs)
      def addAllOutcome(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]): SupportingState = copy(outcome = outcome ++ __vs)
      def withOutcome(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]): SupportingState = copy(outcome = __v)
      def clearPool = copy(pool = List.empty)
      def addPool(__vs: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool*): SupportingState = addAllPool(__vs)
      def addAllPool(__vs: TraversableOnce[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]): SupportingState = copy(pool = pool ++ __vs)
      def withPool(__v: List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]): SupportingState = copy(pool = __v)
      def getCustomer: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer = customer.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer.defaultInstance)
      def clearCustomer: SupportingState = copy(customer = None)
      def withCustomer(__v: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer): SupportingState = copy(customer = Some(__v))
      def getFieldByNumber(__fieldNumber: Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 =>
            outcome
          case 2 =>
            pool
          case 3 =>
            customer.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 =>
            _root_.scalapb.descriptors.PRepeated(outcome.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 2 =>
            _root_.scalapb.descriptors.PRepeated(pool.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 3 =>
            customer.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
      def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState
    }
    object SupportingState extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState] {
      implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState] = this
      def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState = {
        require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
        val __fields = javaDescriptor.getFields
        parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState(__fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]], __fieldsMap.getOrElse(__fields.get(1), Nil).asInstanceOf[List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer]])
      }
      implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState] = _root_.scalapb.descriptors.Reads({
        case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
          require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]]).getOrElse(List.empty), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer]]))
        case _ =>
          throw new RuntimeException("Expected PMessage")
      })
      def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = GenProto.javaDescriptor.getMessageTypes.get(11)
      def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = GenProto.scalaDescriptor.messages(11)
      def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
        var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
        (__number: @_root_.scala.unchecked) match {
          case 1 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome
          case 2 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool
          case 3 =>
            __out = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer
        }
        __out
      }
      lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool, _root_.parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer)
      def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
      lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState()
      @SerialVersionUID(0L) final case class Outcome(outcomeRef: scala.Option[String] = None, marketRef: scala.Option[String] = None, eventRef: scala.Option[String] = None, marketTemplateRef: scala.Option[String] = None, templateMarketRef: scala.Option[String] = None, typeRef: scala.Option[String] = None, classRef: scala.Option[String] = None, categoryRef: scala.Option[String] = None, outcomeName: scala.Option[String] = None, marketName: scala.Option[String] = None, marketSort: scala.Option[String] = None, eventName: scala.Option[String] = None, eventStartTime: scala.Option[String] = None, classSort: scala.Option[String] = None, marketPlaceTerms: scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Outcome] with com.trueaccord.lenses.Updatable[Outcome] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (outcomeRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, outcomeRef.get)
          }
          if (marketRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, marketRef.get)
          }
          if (eventRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, eventRef.get)
          }
          if (marketTemplateRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(12, marketTemplateRef.get)
          }
          if (templateMarketRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(8, templateMarketRef.get)
          }
          if (typeRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(9, typeRef.get)
          }
          if (classRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(10, classRef.get)
          }
          if (categoryRef.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(11, categoryRef.get)
          }
          if (outcomeName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, outcomeName.get)
          }
          if (marketName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, marketName.get)
          }
          if (marketSort.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(13, marketSort.get)
          }
          if (eventName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, eventName.get)
          }
          if (eventStartTime.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(7, eventStartTime.get)
          }
          if (classSort.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(14, classSort.get)
          }
          if (marketPlaceTerms.isDefined) {
            __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(marketPlaceTerms.get.serializedSize) + marketPlaceTerms.get.serializedSize
          }
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
          outcomeRef.foreach {
            __v => _output__.writeString(1, __v)
          }
          marketRef.foreach {
            __v => _output__.writeString(2, __v)
          }
          eventRef.foreach {
            __v => _output__.writeString(3, __v)
          }
          outcomeName.foreach {
            __v => _output__.writeString(4, __v)
          }
          marketName.foreach {
            __v => _output__.writeString(5, __v)
          }
          eventName.foreach {
            __v => _output__.writeString(6, __v)
          }
          eventStartTime.foreach {
            __v => _output__.writeString(7, __v)
          }
          templateMarketRef.foreach {
            __v => _output__.writeString(8, __v)
          }
          typeRef.foreach {
            __v => _output__.writeString(9, __v)
          }
          classRef.foreach {
            __v => _output__.writeString(10, __v)
          }
          categoryRef.foreach {
            __v => _output__.writeString(11, __v)
          }
          marketTemplateRef.foreach {
            __v => _output__.writeString(12, __v)
          }
          marketSort.foreach {
            __v => _output__.writeString(13, __v)
          }
          classSort.foreach {
            __v => _output__.writeString(14, __v)
          }
          marketPlaceTerms.foreach { __v =>
            _output__.writeTag(15, 2)
            _output__.writeUInt32NoTag(__v.serializedSize)
            __v.writeTo(_output__)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome = {
          var __outcomeRef = this.outcomeRef
          var __marketRef = this.marketRef
          var __eventRef = this.eventRef
          var __marketTemplateRef = this.marketTemplateRef
          var __templateMarketRef = this.templateMarketRef
          var __typeRef = this.typeRef
          var __classRef = this.classRef
          var __categoryRef = this.categoryRef
          var __outcomeName = this.outcomeName
          var __marketName = this.marketName
          var __marketSort = this.marketSort
          var __eventName = this.eventName
          var __eventStartTime = this.eventStartTime
          var __classSort = this.classSort
          var __marketPlaceTerms = this.marketPlaceTerms
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __outcomeRef = Some(_input__.readString())
              case 18 =>
                __marketRef = Some(_input__.readString())
              case 26 =>
                __eventRef = Some(_input__.readString())
              case 98 =>
                __marketTemplateRef = Some(_input__.readString())
              case 66 =>
                __templateMarketRef = Some(_input__.readString())
              case 74 =>
                __typeRef = Some(_input__.readString())
              case 82 =>
                __classRef = Some(_input__.readString())
              case 90 =>
                __categoryRef = Some(_input__.readString())
              case 34 =>
                __outcomeName = Some(_input__.readString())
              case 42 =>
                __marketName = Some(_input__.readString())
              case 106 =>
                __marketSort = Some(_input__.readString())
              case 50 =>
                __eventName = Some(_input__.readString())
              case 58 =>
                __eventStartTime = Some(_input__.readString())
              case 114 =>
                __classSort = Some(_input__.readString())
              case 122 =>
                __marketPlaceTerms = Some(_root_.com.trueaccord.scalapb.LiteParser.readMessage(_input__, __marketPlaceTerms.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms.defaultInstance)))
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome(outcomeRef = __outcomeRef, marketRef = __marketRef, eventRef = __eventRef, marketTemplateRef = __marketTemplateRef, templateMarketRef = __templateMarketRef, typeRef = __typeRef, classRef = __classRef, categoryRef = __categoryRef, outcomeName = __outcomeName, marketName = __marketName, marketSort = __marketSort, eventName = __eventName, eventStartTime = __eventStartTime, classSort = __classSort, marketPlaceTerms = __marketPlaceTerms)
        }
        def getOutcomeRef: String = outcomeRef.getOrElse("")
        def clearOutcomeRef: Outcome = copy(outcomeRef = None)
        def withOutcomeRef(__v: String): Outcome = copy(outcomeRef = Some(__v))
        def getMarketRef: String = marketRef.getOrElse("")
        def clearMarketRef: Outcome = copy(marketRef = None)
        def withMarketRef(__v: String): Outcome = copy(marketRef = Some(__v))
        def getEventRef: String = eventRef.getOrElse("")
        def clearEventRef: Outcome = copy(eventRef = None)
        def withEventRef(__v: String): Outcome = copy(eventRef = Some(__v))
        def getMarketTemplateRef: String = marketTemplateRef.getOrElse("")
        def clearMarketTemplateRef: Outcome = copy(marketTemplateRef = None)
        def withMarketTemplateRef(__v: String): Outcome = copy(marketTemplateRef = Some(__v))
        def getTemplateMarketRef: String = templateMarketRef.getOrElse("")
        def clearTemplateMarketRef: Outcome = copy(templateMarketRef = None)
        def withTemplateMarketRef(__v: String): Outcome = copy(templateMarketRef = Some(__v))
        def getTypeRef: String = typeRef.getOrElse("")
        def clearTypeRef: Outcome = copy(typeRef = None)
        def withTypeRef(__v: String): Outcome = copy(typeRef = Some(__v))
        def getClassRef: String = classRef.getOrElse("")
        def clearClassRef: Outcome = copy(classRef = None)
        def withClassRef(__v: String): Outcome = copy(classRef = Some(__v))
        def getCategoryRef: String = categoryRef.getOrElse("")
        def clearCategoryRef: Outcome = copy(categoryRef = None)
        def withCategoryRef(__v: String): Outcome = copy(categoryRef = Some(__v))
        def getOutcomeName: String = outcomeName.getOrElse("")
        def clearOutcomeName: Outcome = copy(outcomeName = None)
        def withOutcomeName(__v: String): Outcome = copy(outcomeName = Some(__v))
        def getMarketName: String = marketName.getOrElse("")
        def clearMarketName: Outcome = copy(marketName = None)
        def withMarketName(__v: String): Outcome = copy(marketName = Some(__v))
        def getMarketSort: String = marketSort.getOrElse("")
        def clearMarketSort: Outcome = copy(marketSort = None)
        def withMarketSort(__v: String): Outcome = copy(marketSort = Some(__v))
        def getEventName: String = eventName.getOrElse("")
        def clearEventName: Outcome = copy(eventName = None)
        def withEventName(__v: String): Outcome = copy(eventName = Some(__v))
        def getEventStartTime: String = eventStartTime.getOrElse("")
        def clearEventStartTime: Outcome = copy(eventStartTime = None)
        def withEventStartTime(__v: String): Outcome = copy(eventStartTime = Some(__v))
        def getClassSort: String = classSort.getOrElse("")
        def clearClassSort: Outcome = copy(classSort = None)
        def withClassSort(__v: String): Outcome = copy(classSort = Some(__v))
        def getMarketPlaceTerms: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms = marketPlaceTerms.getOrElse(parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms.defaultInstance)
        def clearMarketPlaceTerms: Outcome = copy(marketPlaceTerms = None)
        def withMarketPlaceTerms(__v: parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms): Outcome = copy(marketPlaceTerms = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              outcomeRef.orNull
            case 2 =>
              marketRef.orNull
            case 3 =>
              eventRef.orNull
            case 12 =>
              marketTemplateRef.orNull
            case 8 =>
              templateMarketRef.orNull
            case 9 =>
              typeRef.orNull
            case 10 =>
              classRef.orNull
            case 11 =>
              categoryRef.orNull
            case 4 =>
              outcomeName.orNull
            case 5 =>
              marketName.orNull
            case 13 =>
              marketSort.orNull
            case 6 =>
              eventName.orNull
            case 7 =>
              eventStartTime.orNull
            case 14 =>
              classSort.orNull
            case 15 =>
              marketPlaceTerms.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              outcomeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              marketRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              eventRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 12 =>
              marketTemplateRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 8 =>
              templateMarketRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 9 =>
              typeRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 10 =>
              classRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 11 =>
              categoryRef.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              outcomeName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 5 =>
              marketName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 13 =>
              marketSort.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 6 =>
              eventName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 7 =>
              eventStartTime.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 14 =>
              classSort.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 15 =>
              marketPlaceTerms.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome
      }
      object Outcome extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(6)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(7)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(8)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(9)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(10)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(11)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(12)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(13)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(14)).asInstanceOf[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(12).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(8).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(9).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(10).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(11).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(13).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(7).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(14).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(15).get).flatMap(_.as[scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.javaDescriptor.getNestedTypes.get(0)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.scalaDescriptor.nestedMessages(0)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = {
          var __out: _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = null
          (__number: @_root_.scala.unchecked) match {
            case 15 =>
              __out = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms
          }
          __out
        }
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]](_root_.parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms)
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome()
        @SerialVersionUID(0L) final case class PlaceTerms(num: scala.Option[Int] = None, den: scala.Option[Int] = None, places: scala.Option[Int] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[PlaceTerms] with com.trueaccord.lenses.Updatable[PlaceTerms] {
          @transient private[this] var __serializedSizeCachedValue: Int = 0
          private[this] def __computeSerializedValue(): Int = {
            var __size = 0
            if (num.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(1, num.get)
            }
            if (den.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(2, den.get)
            }
            if (places.isDefined) {
              __size += _root_.com.google.protobuf.CodedOutputStream.computeInt32Size(3, places.get)
            }
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
            num.foreach {
              __v => _output__.writeInt32(1, __v)
            }
            den.foreach {
              __v => _output__.writeInt32(2, __v)
            }
            places.foreach {
              __v => _output__.writeInt32(3, __v)
            }
          }
          def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms = {
            var __num = this.num
            var __den = this.den
            var __places = this.places
            var _done__ = false
            while (!_done__) {
              val _tag__ = _input__.readTag()
              _tag__ match {
                case 0 =>
                  _done__ = true
                case 8 =>
                  __num = Some(_input__.readInt32())
                case 16 =>
                  __den = Some(_input__.readInt32())
                case 24 =>
                  __places = Some(_input__.readInt32())
                case tag =>
                  _input__.skipField(tag)
              }
            }
            parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms(num = __num, den = __den, places = __places)
          }
          def getNum: Int = num.getOrElse(0)
          def clearNum: PlaceTerms = copy(num = None)
          def withNum(__v: Int): PlaceTerms = copy(num = Some(__v))
          def getDen: Int = den.getOrElse(0)
          def clearDen: PlaceTerms = copy(den = None)
          def withDen(__v: Int): PlaceTerms = copy(den = Some(__v))
          def getPlaces: Int = places.getOrElse(0)
          def clearPlaces: PlaceTerms = copy(places = None)
          def withPlaces(__v: Int): PlaceTerms = copy(places = Some(__v))
          def getFieldByNumber(__fieldNumber: Int): scala.Any = {
            (__fieldNumber: @_root_.scala.unchecked) match {
              case 1 =>
                num.orNull
              case 2 =>
                den.orNull
              case 3 =>
                places.orNull
            }
          }
          def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
            require(__field.containingMessage eq companion.scalaDescriptor)
            (__field.number: @_root_.scala.unchecked) match {
              case 1 =>
                num.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 2 =>
                den.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
              case 3 =>
                places.map(_root_.scalapb.descriptors.PInt).getOrElse(_root_.scalapb.descriptors.PEmpty)
            }
          }
          override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
          def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms
        }
        object PlaceTerms extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms] {
          implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms] = this
          def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms = {
            require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
            val __fields = javaDescriptor.getFields
            parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[Int]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[Int]])
          }
          implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms] = _root_.scalapb.descriptors.Reads({
            case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
              require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
              parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[Int]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[Int]]))
            case _ =>
              throw new RuntimeException("Expected PMessage")
          })
          def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.javaDescriptor.getNestedTypes.get(0)
          def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.scalaDescriptor.nestedMessages(0)
          def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
          lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
          def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
          lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms()
          implicit class PlaceTermsLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms](_l) {
            def num: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getNum)((c_, f_) => c_.copy(num = Some(f_)))
            def optionalNum: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.num)((c_, f_) => c_.copy(num = f_))
            def den: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getDen)((c_, f_) => c_.copy(den = Some(f_)))
            def optionalDen: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.den)((c_, f_) => c_.copy(den = f_))
            def places: _root_.com.trueaccord.lenses.Lens[UpperPB, Int] = field(_.getPlaces)((c_, f_) => c_.copy(places = Some(f_)))
            def optionalPlaces: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[Int]] = field(_.places)((c_, f_) => c_.copy(places = f_))
          }
          final val NUM_FIELD_NUMBER = 1
          final val DEN_FIELD_NUMBER = 2
          final val PLACES_FIELD_NUMBER = 3
        }
        implicit class OutcomeLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome](_l) {
          def outcomeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOutcomeRef)((c_, f_) => c_.copy(outcomeRef = Some(f_)))
          def optionalOutcomeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.outcomeRef)((c_, f_) => c_.copy(outcomeRef = f_))
          def marketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getMarketRef)((c_, f_) => c_.copy(marketRef = Some(f_)))
          def optionalMarketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.marketRef)((c_, f_) => c_.copy(marketRef = f_))
          def eventRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getEventRef)((c_, f_) => c_.copy(eventRef = Some(f_)))
          def optionalEventRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.eventRef)((c_, f_) => c_.copy(eventRef = f_))
          def marketTemplateRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getMarketTemplateRef)((c_, f_) => c_.copy(marketTemplateRef = Some(f_)))
          def optionalMarketTemplateRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.marketTemplateRef)((c_, f_) => c_.copy(marketTemplateRef = f_))
          def templateMarketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getTemplateMarketRef)((c_, f_) => c_.copy(templateMarketRef = Some(f_)))
          def optionalTemplateMarketRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.templateMarketRef)((c_, f_) => c_.copy(templateMarketRef = f_))
          def typeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getTypeRef)((c_, f_) => c_.copy(typeRef = Some(f_)))
          def optionalTypeRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.typeRef)((c_, f_) => c_.copy(typeRef = f_))
          def classRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getClassRef)((c_, f_) => c_.copy(classRef = Some(f_)))
          def optionalClassRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.classRef)((c_, f_) => c_.copy(classRef = f_))
          def categoryRef: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getCategoryRef)((c_, f_) => c_.copy(categoryRef = Some(f_)))
          def optionalCategoryRef: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.categoryRef)((c_, f_) => c_.copy(categoryRef = f_))
          def outcomeName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getOutcomeName)((c_, f_) => c_.copy(outcomeName = Some(f_)))
          def optionalOutcomeName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.outcomeName)((c_, f_) => c_.copy(outcomeName = f_))
          def marketName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getMarketName)((c_, f_) => c_.copy(marketName = Some(f_)))
          def optionalMarketName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.marketName)((c_, f_) => c_.copy(marketName = f_))
          def marketSort: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getMarketSort)((c_, f_) => c_.copy(marketSort = Some(f_)))
          def optionalMarketSort: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.marketSort)((c_, f_) => c_.copy(marketSort = f_))
          def eventName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getEventName)((c_, f_) => c_.copy(eventName = Some(f_)))
          def optionalEventName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.eventName)((c_, f_) => c_.copy(eventName = f_))
          def eventStartTime: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getEventStartTime)((c_, f_) => c_.copy(eventStartTime = Some(f_)))
          def optionalEventStartTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.eventStartTime)((c_, f_) => c_.copy(eventStartTime = f_))
          def classSort: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getClassSort)((c_, f_) => c_.copy(classSort = Some(f_)))
          def optionalClassSort: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.classSort)((c_, f_) => c_.copy(classSort = f_))
          def marketPlaceTerms: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms] = field(_.getMarketPlaceTerms)((c_, f_) => c_.copy(marketPlaceTerms = Some(f_)))
          def optionalMarketPlaceTerms: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome.PlaceTerms]] = field(_.marketPlaceTerms)((c_, f_) => c_.copy(marketPlaceTerms = f_))
        }
        final val OUTCOMEREF_FIELD_NUMBER = 1
        final val MARKETREF_FIELD_NUMBER = 2
        final val EVENTREF_FIELD_NUMBER = 3
        final val MARKETTEMPLATEREF_FIELD_NUMBER = 12
        final val TEMPLATEMARKETREF_FIELD_NUMBER = 8
        final val TYPEREF_FIELD_NUMBER = 9
        final val CLASSREF_FIELD_NUMBER = 10
        final val CATEGORYREF_FIELD_NUMBER = 11
        final val OUTCOMENAME_FIELD_NUMBER = 4
        final val MARKETNAME_FIELD_NUMBER = 5
        final val MARKETSORT_FIELD_NUMBER = 13
        final val EVENTNAME_FIELD_NUMBER = 6
        final val EVENTSTARTTIME_FIELD_NUMBER = 7
        final val CLASSSORT_FIELD_NUMBER = 14
        final val MARKETPLACETERMS_FIELD_NUMBER = 15
      }
      @SerialVersionUID(0L) final case class Pool(poolName: scala.Option[String] = None, poolTypeName: scala.Option[String] = None, poolTypeId: scala.Option[String] = None, closingTime: scala.Option[String] = None, jackpotName: scala.Option[String] = None, rakePercentage: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Pool] with com.trueaccord.lenses.Updatable[Pool] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (poolName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, poolName.get)
          }
          if (poolTypeName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, poolTypeName.get)
          }
          if (poolTypeId.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, poolTypeId.get)
          }
          if (closingTime.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(4, closingTime.get)
          }
          if (jackpotName.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(5, jackpotName.get)
          }
          if (rakePercentage.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(6, rakePercentage.get)
          }
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
          poolName.foreach {
            __v => _output__.writeString(1, __v)
          }
          poolTypeName.foreach {
            __v => _output__.writeString(2, __v)
          }
          poolTypeId.foreach {
            __v => _output__.writeString(3, __v)
          }
          closingTime.foreach {
            __v => _output__.writeString(4, __v)
          }
          jackpotName.foreach {
            __v => _output__.writeString(5, __v)
          }
          rakePercentage.foreach {
            __v => _output__.writeString(6, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool = {
          var __poolName = this.poolName
          var __poolTypeName = this.poolTypeName
          var __poolTypeId = this.poolTypeId
          var __closingTime = this.closingTime
          var __jackpotName = this.jackpotName
          var __rakePercentage = this.rakePercentage
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __poolName = Some(_input__.readString())
              case 18 =>
                __poolTypeName = Some(_input__.readString())
              case 26 =>
                __poolTypeId = Some(_input__.readString())
              case 34 =>
                __closingTime = Some(_input__.readString())
              case 42 =>
                __jackpotName = Some(_input__.readString())
              case 50 =>
                __rakePercentage = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool(poolName = __poolName, poolTypeName = __poolTypeName, poolTypeId = __poolTypeId, closingTime = __closingTime, jackpotName = __jackpotName, rakePercentage = __rakePercentage)
        }
        def getPoolName: String = poolName.getOrElse("")
        def clearPoolName: Pool = copy(poolName = None)
        def withPoolName(__v: String): Pool = copy(poolName = Some(__v))
        def getPoolTypeName: String = poolTypeName.getOrElse("")
        def clearPoolTypeName: Pool = copy(poolTypeName = None)
        def withPoolTypeName(__v: String): Pool = copy(poolTypeName = Some(__v))
        def getPoolTypeId: String = poolTypeId.getOrElse("")
        def clearPoolTypeId: Pool = copy(poolTypeId = None)
        def withPoolTypeId(__v: String): Pool = copy(poolTypeId = Some(__v))
        def getClosingTime: String = closingTime.getOrElse("")
        def clearClosingTime: Pool = copy(closingTime = None)
        def withClosingTime(__v: String): Pool = copy(closingTime = Some(__v))
        def getJackpotName: String = jackpotName.getOrElse("")
        def clearJackpotName: Pool = copy(jackpotName = None)
        def withJackpotName(__v: String): Pool = copy(jackpotName = Some(__v))
        def getRakePercentage: String = rakePercentage.getOrElse("")
        def clearRakePercentage: Pool = copy(rakePercentage = None)
        def withRakePercentage(__v: String): Pool = copy(rakePercentage = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              poolName.orNull
            case 2 =>
              poolTypeName.orNull
            case 3 =>
              poolTypeId.orNull
            case 4 =>
              closingTime.orNull
            case 5 =>
              jackpotName.orNull
            case 6 =>
              rakePercentage.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              poolName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 2 =>
              poolTypeName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 3 =>
              poolTypeId.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 4 =>
              closingTime.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 5 =>
              jackpotName.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
            case 6 =>
              rakePercentage.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool
      }
      object Pool extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(2)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(3)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[String]], __fieldsMap.get(__fields.get(5)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[String]]), __fieldsMap.get(scalaDescriptor.findFieldByNumber(6).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.javaDescriptor.getNestedTypes.get(1)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.scalaDescriptor.nestedMessages(1)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool()
        implicit class PoolLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool](_l) {
          def poolName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPoolName)((c_, f_) => c_.copy(poolName = Some(f_)))
          def optionalPoolName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.poolName)((c_, f_) => c_.copy(poolName = f_))
          def poolTypeName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPoolTypeName)((c_, f_) => c_.copy(poolTypeName = Some(f_)))
          def optionalPoolTypeName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.poolTypeName)((c_, f_) => c_.copy(poolTypeName = f_))
          def poolTypeId: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getPoolTypeId)((c_, f_) => c_.copy(poolTypeId = Some(f_)))
          def optionalPoolTypeId: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.poolTypeId)((c_, f_) => c_.copy(poolTypeId = f_))
          def closingTime: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getClosingTime)((c_, f_) => c_.copy(closingTime = Some(f_)))
          def optionalClosingTime: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.closingTime)((c_, f_) => c_.copy(closingTime = f_))
          def jackpotName: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getJackpotName)((c_, f_) => c_.copy(jackpotName = Some(f_)))
          def optionalJackpotName: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.jackpotName)((c_, f_) => c_.copy(jackpotName = f_))
          def rakePercentage: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getRakePercentage)((c_, f_) => c_.copy(rakePercentage = Some(f_)))
          def optionalRakePercentage: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.rakePercentage)((c_, f_) => c_.copy(rakePercentage = f_))
        }
        final val POOLNAME_FIELD_NUMBER = 1
        final val POOLTYPENAME_FIELD_NUMBER = 2
        final val POOLTYPEID_FIELD_NUMBER = 3
        final val CLOSINGTIME_FIELD_NUMBER = 4
        final val JACKPOTNAME_FIELD_NUMBER = 5
        final val RAKEPERCENTAGE_FIELD_NUMBER = 6
      }
      @SerialVersionUID(0L) final case class Customer(accountNumber: scala.Option[String] = None) extends com.trueaccord.scalapb.GeneratedMessage with com.trueaccord.scalapb.Message[Customer] with com.trueaccord.lenses.Updatable[Customer] {
        @transient private[this] var __serializedSizeCachedValue: Int = 0
        private[this] def __computeSerializedValue(): Int = {
          var __size = 0
          if (accountNumber.isDefined) {
            __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, accountNumber.get)
          }
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
          accountNumber.foreach {
            __v => _output__.writeString(1, __v)
          }
        }
        def mergeFrom(_input__ : _root_.com.google.protobuf.CodedInputStream): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer = {
          var __accountNumber = this.accountNumber
          var _done__ = false
          while (!_done__) {
            val _tag__ = _input__.readTag()
            _tag__ match {
              case 0 =>
                _done__ = true
              case 10 =>
                __accountNumber = Some(_input__.readString())
              case tag =>
                _input__.skipField(tag)
            }
          }
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer(accountNumber = __accountNumber)
        }
        def getAccountNumber: String = accountNumber.getOrElse("")
        def clearAccountNumber: Customer = copy(accountNumber = None)
        def withAccountNumber(__v: String): Customer = copy(accountNumber = Some(__v))
        def getFieldByNumber(__fieldNumber: Int): scala.Any = {
          (__fieldNumber: @_root_.scala.unchecked) match {
            case 1 =>
              accountNumber.orNull
          }
        }
        def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
          require(__field.containingMessage eq companion.scalaDescriptor)
          (__field.number: @_root_.scala.unchecked) match {
            case 1 =>
              accountNumber.map(_root_.scalapb.descriptors.PString).getOrElse(_root_.scalapb.descriptors.PEmpty)
          }
        }
        override def toString: String = _root_.com.trueaccord.scalapb.TextFormat.printToUnicodeString(this)
        def companion = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer
      }
      object Customer extends com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer] {
        implicit def messageCompanion: com.trueaccord.scalapb.GeneratedMessageCompanion[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer] = this
        def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer = {
          require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
          val __fields = javaDescriptor.getFields
          parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer(__fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[String]])
        }
        implicit def messageReads: _root_.scalapb.descriptors.Reads[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer] = _root_.scalapb.descriptors.Reads({
          case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
            require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
            parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer(__fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[String]]))
          case _ =>
            throw new RuntimeException("Expected PMessage")
        })
        def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.javaDescriptor.getNestedTypes.get(2)
        def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.scalaDescriptor.nestedMessages(2)
        def messageCompanionForFieldNumber(__number: Int): _root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
        lazy val nestedMessagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
        def enumCompanionForFieldNumber(__fieldNumber: Int): _root_.com.trueaccord.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
        lazy val defaultInstance = parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer()
        implicit class CustomerLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer](_l) {
          def accountNumber: _root_.com.trueaccord.lenses.Lens[UpperPB, String] = field(_.getAccountNumber)((c_, f_) => c_.copy(accountNumber = Some(f_)))
          def optionalAccountNumber: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[String]] = field(_.accountNumber)((c_, f_) => c_.copy(accountNumber = f_))
        }
        final val ACCOUNTNUMBER_FIELD_NUMBER = 1
      }
      implicit class SupportingStateLens[UpperPB](_l: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState]) extends _root_.com.trueaccord.lenses.ObjectLens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState](_l) {
        def outcome: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Outcome]] = field(_.outcome)((c_, f_) => c_.copy(outcome = f_))
        def pool: _root_.com.trueaccord.lenses.Lens[UpperPB, List[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Pool]] = field(_.pool)((c_, f_) => c_.copy(pool = f_))
        def customer: _root_.com.trueaccord.lenses.Lens[UpperPB, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer] = field(_.getCustomer)((c_, f_) => c_.copy(customer = Some(f_)))
        def optionalCustomer: _root_.com.trueaccord.lenses.Lens[UpperPB, scala.Option[parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState.Customer]] = field(_.customer)((c_, f_) => c_.copy(customer = f_))
      }
      final val OUTCOME_FIELD_NUMBER = 1
      final val POOL_FIELD_NUMBER = 2
      final val CUSTOMER_FIELD_NUMBER = 3
    }
    object GenProto extends _root_.com.trueaccord.scalapb.GeneratedFileObject {
      lazy val dependencies: Seq[_root_.com.trueaccord.scalapb.GeneratedFileObject] = Seq(com.trueaccord.scalapb.scalapb.ScalapbProto)
      lazy val messagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq(parallelai.sot.executor.builder.SOTBuilder.gen.Batch, parallelai.sot.executor.builder.SOTBuilder.gen.Activity, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingKey, parallelai.sot.executor.builder.SOTBuilder.gen.ActivityDescriptor, parallelai.sot.executor.builder.SOTBuilder.gen.SequencingDescriptor, parallelai.sot.executor.builder.SOTBuilder.gen.Monitoring, parallelai.sot.executor.builder.SOTBuilder.gen.Source, parallelai.sot.executor.builder.SOTBuilder.gen.ExternalRef, parallelai.sot.executor.builder.SOTBuilder.gen.Bet, parallelai.sot.executor.builder.SOTBuilder.gen.BetSlip, parallelai.sot.executor.builder.SOTBuilder.gen.BetState, parallelai.sot.executor.builder.SOTBuilder.gen.SupportingState)
      private lazy val ProtoBytes: Array[Byte] = com.trueaccord.scalapb.Encoding.fromBase64(scala.collection.Seq("""CglnZW4ucHJvdG8SBGNvcmUaFXNjYWxhcGIvc2NhbGFwYi5wcm90byKBAQoFQmF0Y2gSGgoIdW5pcXVlSWQYASACKAlSCHVua
  XF1ZUlkEioKCG1lc3NhZ2VzGAIgAygLMg4uY29yZS5BY3Rpdml0eVIIbWVzc2FnZXMSMAoKbW9uaXRvcmluZxgDIAEoCzIQLmNvc
  mUuTW9uaXRvcmluZ1IKbW9uaXRvcmluZyKABwoIQWN0aXZpdHkSNQoGaGVhZGVyGAEgAigLMh0uY29yZS5BY3Rpdml0eS5BY3Rpd
  ml0eUhlYWRlclIGaGVhZGVyEiUKBXN0YXRlGMkBIAEoCzIOLmNvcmUuQmV0U3RhdGVSBXN0YXRlEkAKD3N1cHBvcnRpbmdTdGF0Z
  RjTASABKAsyFS5jb3JlLlN1cHBvcnRpbmdTdGF0ZVIPc3VwcG9ydGluZ1N0YXRlGoIFCg5BY3Rpdml0eUhlYWRlchIeCgphY3Rpd
  ml0eUlkGAEgAigDUgphY3Rpdml0eUlkEhwKCXRpbWVTdGFtcBgCIAEoA1IJdGltZVN0YW1wEjMKCGNydWRUeXBlGAMgASgOMhcuY
  29yZS5BY3Rpdml0eS5DcnVkVHlwZVIIY3J1ZFR5cGUSOQoNc2VxdWVuY2luZ0tleRgEIAEoCzITLmNvcmUuU2VxdWVuY2luZ0tle
  VINc2VxdWVuY2luZ0tleRIYCgdxdWV1ZUlkGAUgASgDUgdxdWV1ZUlkEhgKB3BheWxvYWQYBiABKAxSB3BheWxvYWQSGgoIdW5pc
  XVlSWQYByABKAlSCHVuaXF1ZUlkEkgKEmFjdGl2aXR5RGVzY3JpcHRvchgIIAEoCzIYLmNvcmUuQWN0aXZpdHlEZXNjcmlwdG9yU
  hJhY3Rpdml0eURlc2NyaXB0b3ISJAoNZmlyc3RTZW50VGltZRgJIAEoA1INZmlyc3RTZW50VGltZRIgCgtzZW5kUmV0cmllcxgKI
  AEoBVILc2VuZFJldHJpZXMSHgoKY29udGV4dFJlZhgLIAEoCVIKY29udGV4dFJlZhIiCgxwdWJsaXNoZXJSZWYYDCABKAlSDHB1Y
  mxpc2hlclJlZhIgCgtvcGVyYXRvclJlZhgOIAEoCVILb3BlcmF0b3JSZWYSIgoMb3BlcmF0b3JOYW1lGBAgASgJUgxvcGVyYXRvc
  k5hbWUSMAoKbW9uaXRvcmluZxgNIAEoCzIQLmNvcmUuTW9uaXRvcmluZ1IKbW9uaXRvcmluZxIkCg1jb3JyZWxhdGlvbklkGA8gA
  SgDUg1jb3JyZWxhdGlvbklkIk8KCENydWRUeXBlEgoKBkNSRUFURRABEggKBFJFQUQQAhIKCgZVUERBVEUQAxIKCgZERUxFVEUQB
  BIKCgZJTlNFUlQQBRIJCgVQVVJHRRAGIoMBCg1TZXF1ZW5jaW5nS2V5Ek4KFHNlcXVlbmNpbmdEZXNjcmlwdG9yGAEgASgLMhouY
  29yZS5TZXF1ZW5jaW5nRGVzY3JpcHRvclIUc2VxdWVuY2luZ0Rlc2NyaXB0b3ISIgoMc2VxdWVuY2luZ0lkGAIgASgFUgxzZXF1Z
  W5jaW5nSWQiWgoSQWN0aXZpdHlEZXNjcmlwdG9yEg4KAmlkGAEgAigFUgJpZBISCgRuYW1lGAIgAigJUgRuYW1lEiAKC2Rlc2Nya
  XB0aW9uGAMgASgJUgtkZXNjcmlwdGlvbiJcChRTZXF1ZW5jaW5nRGVzY3JpcHRvchIOCgJpZBgBIAIoBVICaWQSEgoEbmFtZRgCI
  AIoCVIEbmFtZRIgCgtkZXNjcmlwdGlvbhgDIAEoCVILZGVzY3JpcHRpb24itAIKCk1vbml0b3JpbmcSNAoVaW5pdGlhbGl6ZUF0Q
  29sbGVjdG9yGAEgASgDUhVpbml0aWFsaXplQXRDb2xsZWN0b3ISNgoWYmVmb3JlU3VibWl0VG9Db21wb3NlchgCIAEoA1IWYmVmb
  3JlU3VibWl0VG9Db21wb3NlchIuChJzdGFydENvbXBvc2VyUXVlcnkYAyABKANSEnN0YXJ0Q29tcG9zZXJRdWVyeRIqChBlbmRDb
  21wb3NlclF1ZXJ5GAQgASgDUhBlbmRDb21wb3NlclF1ZXJ5EjAKE29yY2hlc3RyYXRvclJlY2VpdmUYBSABKANSE29yY2hlc3RyY
  XRvclJlY2VpdmUSKgoQb3JjaGVzdHJhdG9yU2VuZBgGIAEoA1IQb3JjaGVzdHJhdG9yU2VuZCLkAQoGU291cmNlEh4KCmNoYW5uZ
  WxSZWYYASABKAlSCmNoYW5uZWxSZWYSGAoHc2l0ZVJlZhgCIAEoCVIHc2l0ZVJlZhIoCg9qdXJpc2RpY3Rpb25SZWYYAyABKAlSD
  2p1cmlzZGljdGlvblJlZhIeCgpwcm9kdWN0UmVmGAQgASgJUgpwcm9kdWN0UmVmEiAKC3BsYXRmb3JtUmVmGAUgASgJUgtwbGF0Z
  m9ybVJlZhIcCglkZXZpY2VSZWYYBiABKAlSCWRldmljZVJlZhIWCgZpcEFkZHIYByABKAlSBmlwQWRkciJlCgtFeHRlcm5hbFJlZ
  hIaCghwcm92aWRlchgBIAIoCVIIcHJvdmlkZXISDgoCaWQYAiACKAlSAmlkEhgKB3JlZlR5cGUYAyABKAlSB3JlZlR5cGUSEAoDd
  XJpGAQgASgJUgN1cmkilRcKA0JldBIOCgJpZBgBIAIoCVICaWQSIgoMY3JlYXRpb25EYXRlGAIgASgJUgxjcmVhdGlvbkRhdGUSH
  goKYWNjb3VudFJlZhgDIAEoCVIKYWNjb3VudFJlZhIgCgtjdXN0b21lclJlZhgEIAEoCVILY3VzdG9tZXJSZWYSJAoGc291cmNlG
  AUgASgLMgwuY29yZS5Tb3VyY2VSBnNvdXJjZRIzCgtleHRlcm5hbFVJRBgWIAMoCzIRLmNvcmUuRXh0ZXJuYWxSZWZSC2V4dGVyb
  mFsVUlEEh4KCmJldFR5cGVSZWYYBiABKAlSCmJldFR5cGVSZWYSGgoIcGxhY2VkQXQYByABKAlSCHBsYWNlZEF0EhgKB3JlY2Vpc
  HQYCCABKAlSB3JlY2VpcHQSHAoJaXNTZXR0bGVkGAkgASgIUglpc1NldHRsZWQSIAoLaXNDb25maXJtZWQYCiABKAhSC2lzQ29uZ
  mlybWVkEiAKC2lzQ2FuY2VsbGVkGAsgASgIUgtpc0NhbmNlbGxlZBIgCgtpc0Nhc2hlZE91dBgMIAEoCFILaXNDYXNoZWRPdXQSH
  AoJaXNQb29sQmV0GA0gASgIUglpc1Bvb2xCZXQSHAoJc2V0dGxlZEF0GA4gASgJUglzZXR0bGVkQXQSHgoKc2V0dGxlZEhvdxgPI
  AEoCVIKc2V0dGxlZEhvdxIwChNwbGFjZWRCeUN1c3RvbWVyUmVmGBQgASgJUhNwbGFjZWRCeUN1c3RvbWVyUmVmEhoKCGlzUGFya
  2VkGBUgASgIUghpc1BhcmtlZBIlCgVzdGFrZRgeIAEoCzIPLmNvcmUuQmV0LlN0YWtlUgVzdGFrZRItCglwb29sU3Rha2UYHyABK
  AsyDy5jb3JlLkJldC5TdGFrZVIJcG9vbFN0YWtlEigKBnBheW91dBgoIAEoCzIQLmNvcmUuQmV0LlBheW91dFIGcGF5b3V0EiUKB
  WxpbmVzGDIgASgLMg8uY29yZS5CZXQuTGluZXNSBWxpbmVzEh8KA2xlZxg8IAMoCzINLmNvcmUuQmV0LkxlZ1IDbGVnEj0KDWJld
  FRlcm1DaGFuZ2UYPSADKAsyFy5jb3JlLkJldC5CZXRUZXJtQ2hhbmdlUg1iZXRUZXJtQ2hhbmdlEj0KDXBvb2xCZXRTeXN0ZW0YP
  iABKAsyFy5jb3JlLkJldC5Qb29sQmV0U3lzdGVtUg1wb29sQmV0U3lzdGVtEjYKFnBvb2xCZXRTdWJzY3JpcHRpb25SZWYYPyABK
  AlSFnBvb2xCZXRTdWJzY3JpcHRpb25SZWYSHAoJaXNQZW5kaW5nGEAgASgIUglpc1BlbmRpbmcSOQoMYmV0T3ZlcnJpZGVzGEEgA
  ygLMhUuY29yZS5CZXQuQmV0T3ZlcnJpZGVSDGJldE92ZXJyaWRlcxp/CgVTdGFrZRIWCgZhbW91bnQYASABKAlSBmFtb3VudBIiC
  gxzdGFrZVBlckxpbmUYAiABKAlSDHN0YWtlUGVyTGluZRIYCgdmcmVlQmV0GAMgASgJUgdmcmVlQmV0EiAKC2N1cnJlbmN5UmVmG
  AQgASgJUgtjdXJyZW5jeVJlZhpcCgZQYXlvdXQSGgoId2lubmluZ3MYASABKAlSCHdpbm5pbmdzEhgKB3JlZnVuZHMYAiABKAlSB
  3JlZnVuZHMSHAoJcG90ZW50aWFsGAMgASgJUglwb3RlbnRpYWwaXQoFTGluZXMSFgoGbnVtYmVyGAEgASgFUgZudW1iZXISEAoDd
  2luGAIgASgFUgN3aW4SEgoEbG9zZRgDIAEoBVIEbG9zZRIWCgZ2b2lkZWQYBCABKAVSBnZvaWRlZBqPBwoDTGVnEikKBXByaWNlG
  AEgASgLMhMuY29yZS5CZXQuTGVnLlByaWNlUgVwcmljZRIgCgt3aW5QbGFjZVJlZhgCIAEoCVILd2luUGxhY2VSZWYSMQoIbGVnU
  GFydHMYAyADKAsyFS5jb3JlLkJldC5MZWcuTGVnUGFydFIIbGVnUGFydHMSFgoGcmVzdWx0GAQgASgJUgZyZXN1bHQSFgoGcG9vb
  ElkGAUgASgJUgZwb29sSWQSGAoHbGVnU29ydBgGIAEoCVIHbGVnU29ydBIUCgVpbmRleBgHIAEoBVIFaW5kZXgSMgoIaGFuZGljY
  XAYCCABKAsyFi5jb3JlLkJldC5MZWcuSGFuZGljYXBSCGhhbmRpY2FwEhgKB25vQ29tYmkYCSABKAlSB25vQ29tYmkamQEKBVBya
  WNlEhAKA251bRgBIAEoBVIDbnVtEhAKA2RlbhgCIAEoBVIDZGVuEhgKB2RlY2ltYWwYAyABKAlSB2RlY2ltYWwSIgoMcHJpY2VUe
  XBlUmVmGAQgASgJUgxwcmljZVR5cGVSZWYSLgoSaXNFYXJseVByaWNlQWN0aXZlGAUgASgIUhJpc0Vhcmx5UHJpY2VBY3RpdmUa4
  QIKB0xlZ1BhcnQSHgoKb3V0Y29tZVJlZhgBIAEoCVIKb3V0Y29tZVJlZhIcCgltYXJrZXRSZWYYAiABKAlSCW1hcmtldFJlZhIaC
  ghldmVudFJlZhgDIAEoCVIIZXZlbnRSZWYSFgoGcGxhY2VzGAggASgJUgZwbGFjZXMSIAoLaXNJblJ1bm5pbmcYCSABKAhSC2lzS
  W5SdW5uaW5nEjYKFmJldHRpbmdTeXN0ZW1QcmVmZXJyZWQYCiABKAhSFmJldHRpbmdTeXN0ZW1QcmVmZXJyZWQSQAoKcGxhY2VUZ
  XJtcxgLIAEoCzIgLmNvcmUuQmV0LkxlZy5MZWdQYXJ0LlBsYWNlVGVybXNSCnBsYWNlVGVybXMaSAoKUGxhY2VUZXJtcxIQCgNud
  W0YASABKAVSA251bRIQCgNkZW4YAiABKAVSA2RlbhIWCgZwbGFjZXMYAyABKAVSBnBsYWNlcxpaCghIYW5kaWNhcBIoCg9oYW5ka
  WNhcFR5cGVSZWYYASACKAlSD2hhbmRpY2FwVHlwZVJlZhIQCgNsb3cYAiABKAlSA2xvdxISCgRoaWdoGAMgASgJUgRoaWdoGsICC
  g1CZXRUZXJtQ2hhbmdlEhQKBWJldElkGAEgAigJUgViZXRJZBIeCgpjaGFuZ2VUaW1lGAIgASgJUgpjaGFuZ2VUaW1lEhwKCWxlZ
  051bWJlchgDIAEoCVIJbGVnTnVtYmVyEj8KCWxlZ0NoYW5nZRgEIAEoCzIhLmNvcmUuQmV0LkJldFRlcm1DaGFuZ2UuTGVnQ2hhb
  mdlUglsZWdDaGFuZ2UamwEKCUxlZ0NoYW5nZRI9CgVwcmljZRgBIAEoCzInLmNvcmUuQmV0LkJldFRlcm1DaGFuZ2UuTGVnQ2hhb
  mdlLlByaWNlUgVwcmljZRpPCgVQcmljZRIQCgNudW0YASABKAVSA251bRIQCgNkZW4YAiABKAVSA2RlbhIiCgxwcmljZVR5cGVSZ
  WYYAyABKAlSDHByaWNlVHlwZVJlZhpZCg1Qb29sQmV0U3lzdGVtEiIKDGJldFN5c3RlbVJlZhgBIAEoCVIMYmV0U3lzdGVtUmVmE
  iQKDWN1c3RvbUxpbmVSZWYYAiABKAlSDWN1c3RvbUxpbmVSZWYa8QEKC0JldE92ZXJyaWRlEg4KAmlkGAEgAigJUgJpZBIgCgtvc
  GVyYXRvclJlZhgCIAEoCVILb3BlcmF0b3JSZWYSIgoMY3JlYXRpb25EYXRlGAMgASgJUgxjcmVhdGlvbkRhdGUSFgoGYWN0aW9uG
  AQgASgJUgZhY3Rpb24SGAoHY2FsbFJlZhgFIAEoCVIHY2FsbFJlZhIWCgZyZWFzb24YBiABKAlSBnJlYXNvbhIUCgVsZWdObxgHI
  AEoBVIFbGVnTm8SFgoGcGFydE5vGAggASgFUgZwYXJ0Tm8SFAoFcmVmSWQYCSABKAlSBXJlZklkIo4BCgdCZXRTbGlwEg4KAmlkG
  AEgAigJUgJpZBIeCgphY2NvdW50UmVmGAIgASgJUgphY2NvdW50UmVmEiIKDGNyZWF0aW9uRGF0ZRgDIAEoCVIMY3JlYXRpb25EY
  XRlEi8KCnRvdGFsU3Rha2UYBCABKAsyDy5jb3JlLkJldC5TdGFrZVIKdG90YWxTdGFrZSJvCghCZXRTdGF0ZRIbCgNiZXQYASABK
  AsyCS5jb3JlLkJldFIDYmV0Eh0KBGJldHMYAiADKAsyCS5jb3JlLkJldFIEYmV0cxInCgdiZXRTbGlwGAMgASgLMg0uY29yZS5CZ
  XRTbGlwUgdiZXRTbGlwIsEICg9TdXBwb3J0aW5nU3RhdGUSNwoHb3V0Y29tZRgBIAMoCzIdLmNvcmUuU3VwcG9ydGluZ1N0YXRlL
  k91dGNvbWVSB291dGNvbWUSLgoEcG9vbBgCIAMoCzIaLmNvcmUuU3VwcG9ydGluZ1N0YXRlLlBvb2xSBHBvb2wSOgoIY3VzdG9tZ
  XIYAyABKAsyHi5jb3JlLlN1cHBvcnRpbmdTdGF0ZS5DdXN0b21lclIIY3VzdG9tZXIagQUKB091dGNvbWUSHgoKb3V0Y29tZVJlZ
  hgBIAEoCVIKb3V0Y29tZVJlZhIcCgltYXJrZXRSZWYYAiABKAlSCW1hcmtldFJlZhIaCghldmVudFJlZhgDIAEoCVIIZXZlbnRSZ
  WYSLAoRbWFya2V0VGVtcGxhdGVSZWYYDCABKAlSEW1hcmtldFRlbXBsYXRlUmVmEjAKEXRlbXBsYXRlTWFya2V0UmVmGAggASgJQ
  gIYAVIRdGVtcGxhdGVNYXJrZXRSZWYSGAoHdHlwZVJlZhgJIAEoCVIHdHlwZVJlZhIaCghjbGFzc1JlZhgKIAEoCVIIY2xhc3NSZ
  WYSIAoLY2F0ZWdvcnlSZWYYCyABKAlSC2NhdGVnb3J5UmVmEiAKC291dGNvbWVOYW1lGAQgASgJUgtvdXRjb21lTmFtZRIeCgptY
  XJrZXROYW1lGAUgASgJUgptYXJrZXROYW1lEh4KCm1hcmtldFNvcnQYDSABKAlSCm1hcmtldFNvcnQSHAoJZXZlbnROYW1lGAYgA
  SgJUglldmVudE5hbWUSJgoOZXZlbnRTdGFydFRpbWUYByABKAlSDmV2ZW50U3RhcnRUaW1lEhwKCWNsYXNzU29ydBgOIAEoCVIJY
  2xhc3NTb3J0ElQKEG1hcmtldFBsYWNlVGVybXMYDyABKAsyKC5jb3JlLlN1cHBvcnRpbmdTdGF0ZS5PdXRjb21lLlBsYWNlVGVyb
  XNSEG1hcmtldFBsYWNlVGVybXMaSAoKUGxhY2VUZXJtcxIQCgNudW0YASABKAVSA251bRIQCgNkZW4YAiABKAVSA2RlbhIWCgZwb
  GFjZXMYAyABKAVSBnBsYWNlcxrSAQoEUG9vbBIaCghwb29sTmFtZRgBIAEoCVIIcG9vbE5hbWUSIgoMcG9vbFR5cGVOYW1lGAIgA
  SgJUgxwb29sVHlwZU5hbWUSHgoKcG9vbFR5cGVJZBgDIAEoCVIKcG9vbFR5cGVJZBIgCgtjbG9zaW5nVGltZRgEIAEoCVILY2xvc
  2luZ1RpbWUSIAoLamFja3BvdE5hbWUYBSABKAlSC2phY2twb3ROYW1lEiYKDnJha2VQZXJjZW50YWdlGAYgASgJUg5yYWtlUGVyY
  2VudGFnZRowCghDdXN0b21lchIkCg1hY2NvdW50TnVtYmVyGAEgASgJUg1hY2NvdW50TnVtYmVyQnMKLWNvbS5vcGVuYmV0LnBsY
  XRmb3JtLmFjdGl2aXR5ZmVlZHMubW9kZWwuY29yZUIJQ29yZU1vZGVs4j82CipwYXJhbGxlbGFpLnNvdC5leGVjdXRvci5idWlsZ
  GVyLlNPVEJ1aWxkZXIQACgBQgRMaXN0""").mkString)
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
  }
  import SOTBuilder.gen._
  object conf {
    val jobConfig = SOTMacroJsonConfig(SchemaResourcePath().value)
    val sourceTap = getSource(jobConfig)._2
    val sinkTaps = getSinks(jobConfig)
    val source = TapDef[parallelai.sot.executor.model.SOTMacroConfig.PubSubTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.trueaccord.scalapb.GeneratedMessage, Activity](conf.sourceTap)
    val sinks = SchemalessTapDef[parallelai.sot.executor.model.SOTMacroConfig.BigQueryTapDefinition, parallelai.sot.engine.config.gcp.SOTUtils, com.google.api.client.json.GenericJson](conf.sinkTaps(0)._2) :: HNil
  }
  class Builder extends Serializable() {
    def execute(sotUtils: SOTUtils, sc: ScioContext, args: Args): Unit = {
      val job = init[ScioContext].flatMap(a => read(conf.source, sotUtils)).flatMap(sColl => map(m => {
        val winnings = Col('state) ->: Col('bets) ->: Col('payout) ->: Col('winnings)
        val customerRef = Col('state) ->: Col('bets) ->: Col('customerRef)
        val outComeName = Col('supportingState) ->: Col('outcome) ->: (Col('outcomeName) ** Col('outComeName))
        val marketName = Col('supportingState) ->: Col('outcome) ->: Col('marketName)
        val eventName = Col('supportingState) ->: Col('outcome) ->: Col('eventName)
        val catRef = Col('supportingState) ->: Col('outcome) ->: Col('categoryRef)
        val stateBetsId = Col('state) ->: Col('bets) ->: (Col('id) ** Col('stateBetsId))
        val currency = Col('state) ->: Col('bets) ->: Col('stake) ->: (Col('currencyRef) ** Col('currency))
        val stake = Col('state) ->: Col('bets) ->: Col('stake) ->: (Col('amount) ** Col('stake))
        m.project(projector(customerRef, winnings, outComeName, marketName, eventName, catRef, stateBetsId, currency, stake))
      })).flatMap(sColl => map(m => {
        val message = "Congratulations. You won " + m.get('winnings) + " " + m.get('currency) + ". On a bet placed on " + m.get('eventName) + " for " + m.get('outComeName) + " to " + m.get('marketName) + "."
        m.append('message, message).append('ts, Helper.fmt.print(Instant.now()))
      })).flatMap(sColl => withFixedWindows(Duration.millis(100), Duration.millis(0),
        WindowOptions(trigger =  Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()
          .plusDelayOf(Duration.standardMinutes(2)))
          , accumulationMode = AccumulationMode.ACCUMULATING_FIRED_PANES,
          allowedLateness =  Duration.standardMinutes(args.int("allowedLateness", 120))

        )))
//        .flatMap(sColl => groupByKeyRow(r => r.get('customerRef)))
        .flatMap(sColl => aggregate(Row(('events ->> List[String]()) :: HNil))({
        (aggr, m) => Row(('events ->> (aggr.get('events) :+ m.get('eventName))) :: HNil)
      }, {
        (aggr1, aggr2) => Row(('events ->> (aggr1.get('events) ++ aggr2.get('events))) :: HNil)
      })).flatMap(a => writeToSinks(conf.sinks, sotUtils))
      job.run(sc)._1
      val result = sc.close()
      if (args.getOrElse("waitToFinish", "true").toBoolean) sotUtils.waitToFinish(result.internal)
    }
  }
  val genericBuilder = new Builder()
  def main(cmdArg: Array[String]): Unit = {
    val (sotOptions, sotArgs) = ScioContext.parseArguments[SOTOptions](cmdArg)
    val sotUtils = new SOTUtils(sotOptions)
    val sc = ScioContext(sotOptions)
    val builder = genericBuilder
    builder.execute(sotUtils, sc, sotArgs)
  }
}
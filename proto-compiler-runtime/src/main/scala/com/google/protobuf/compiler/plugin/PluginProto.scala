// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO2

package com.google.protobuf.compiler.plugin

object PluginProto extends _root_.com.trueaccord.scalapb.GeneratedFileObject {
  lazy val dependencies: Seq[_root_.com.trueaccord.scalapb.GeneratedFileObject] = Seq(
    com.google.protobuf.descriptor.DescriptorProtoCompanion
  )
  lazy val messagesCompanions: Seq[_root_.com.trueaccord.scalapb.GeneratedMessageCompanion[_]] = Seq(
    com.google.protobuf.compiler.plugin.Version,
    com.google.protobuf.compiler.plugin.CodeGeneratorRequest,
    com.google.protobuf.compiler.plugin.CodeGeneratorResponse
  )
  private lazy val ProtoBytes: Array[Byte] =
      com.trueaccord.scalapb.Encoding.fromBase64(scala.collection.Seq(
  """CiVnb29nbGUvcHJvdG9idWYvY29tcGlsZXIvcGx1Z2luLnByb3RvEhhnb29nbGUucHJvdG9idWYuY29tcGlsZXIaIGdvb2dsZ
  S9wcm90b2J1Zi9kZXNjcmlwdG9yLnByb3RvImMKB1ZlcnNpb24SFAoFbWFqb3IYASABKAVSBW1ham9yEhQKBW1pbm9yGAIgASgFU
  gVtaW5vchIUCgVwYXRjaBgDIAEoBVIFcGF0Y2gSFgoGc3VmZml4GAQgASgJUgZzdWZmaXgi8QEKFENvZGVHZW5lcmF0b3JSZXF1Z
  XN0EigKEGZpbGVfdG9fZ2VuZXJhdGUYASADKAlSDmZpbGVUb0dlbmVyYXRlEhwKCXBhcmFtZXRlchgCIAEoCVIJcGFyYW1ldGVyE
  kMKCnByb3RvX2ZpbGUYDyADKAsyJC5nb29nbGUucHJvdG9idWYuRmlsZURlc2NyaXB0b3JQcm90b1IJcHJvdG9GaWxlEkwKEGNvb
  XBpbGVyX3ZlcnNpb24YAyABKAsyIS5nb29nbGUucHJvdG9idWYuY29tcGlsZXIuVmVyc2lvblIPY29tcGlsZXJWZXJzaW9uItYBC
  hVDb2RlR2VuZXJhdG9yUmVzcG9uc2USFAoFZXJyb3IYASABKAlSBWVycm9yEkgKBGZpbGUYDyADKAsyNC5nb29nbGUucHJvdG9id
  WYuY29tcGlsZXIuQ29kZUdlbmVyYXRvclJlc3BvbnNlLkZpbGVSBGZpbGUaXQoERmlsZRISCgRuYW1lGAEgASgJUgRuYW1lEicKD
  2luc2VydGlvbl9wb2ludBgCIAEoCVIOaW5zZXJ0aW9uUG9pbnQSGAoHY29udGVudBgPIAEoCVIHY29udGVudEJnChxjb20uZ29vZ
  2xlLnByb3RvYnVmLmNvbXBpbGVyQgxQbHVnaW5Qcm90b3NaOWdpdGh1Yi5jb20vZ29sYW5nL3Byb3RvYnVmL3Byb3RvYy1nZW4tZ
  28vcGx1Z2luO3BsdWdpbl9nbw=="""
      ).mkString)
  lazy val scalaDescriptor: _root_.scalapb.descriptors.FileDescriptor = {
    val scalaProto = com.google.protobuf.descriptor.FileDescriptorProto.parseFrom(ProtoBytes)
    _root_.scalapb.descriptors.FileDescriptor.buildFrom(scalaProto, dependencies.map(_.scalaDescriptor))
  }
  lazy val javaDescriptor: com.google.protobuf.Descriptors.FileDescriptor =
    com.google.protobuf.compiler.PluginProtos.getDescriptor()
  @deprecated("Use javaDescriptor instead. In a future version this will refer to scalaDescriptor.", "ScalaPB 0.5.47")
  def descriptor: com.google.protobuf.Descriptors.FileDescriptor = javaDescriptor
}
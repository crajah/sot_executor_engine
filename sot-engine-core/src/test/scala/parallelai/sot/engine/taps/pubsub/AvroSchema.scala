package parallelai.sot.engine.taps.pubsub

import com.spotify.scio.avro.types.AvroType

object AvroSchema {

  @AvroType.fromSchema(
    """
      |{
      |  "name": "Message",
      |  "doc": "A basic schema for storing user records",
      |  "fields": [
      |    {
      |      "name": "user",
      |      "type": "string",
      |      "doc": "Name of the user"
      |    },
      |    {
      |      "name": "teamName",
      |      "type": "string",
      |      "doc": "Name of the team"
      |    },
      |    {
      |      "name": "score",
      |      "type": "long",
      |      "doc": "User score"
      |    },
      |    {
      |      "name": "eventTime",
      |      "type": "long",
      |      "doc": "time when event created"
      |    },
      |    {
      |      "name": "eventTimeStr",
      |      "type": "string",
      |      "doc": "event time string for debugging"
      |    }
      |  ],
      |  "type": "record",
      |  "namespace": "parallelai.sot.avro"
      |}
    """.stripMargin) class MessageAvro

  @AvroType.fromSchema(
    """
      |  {
      |    "type": "record",
      |    "namespace": "parallelai.sot.avro",
      |    "name": "MessageAvroNested",
      |    "doc": "A basic schema for storing user records",
      |    "fields": [
      |      {
      |        "name": "user",
      |        "type": "string",
      |        "doc": "Name of the user"
      |      },
      |      {
      |        "name": "teamName",
      |        "type": "string",
      |        "doc": "Name of the team"
      |      },
      |      {
      |        "name": "score",
      |        "type": "long",
      |        "doc": "User score"
      |      },
      |      {
      |        "name": "eventTime",
      |        "type": "long",
      |        "doc": "time when event created"
      |      },
      |      {
      |        "name": "eventTimeStr",
      |        "type": "string",
      |        "doc": "event time string for debugging"
      |      },
      |      {
      |        "name": "nestedValue",
      |        "type": {
      |          "type": "array",
      |          "items": {
      |              "name": "NestedClass",
      |              "type": "record",
      |              "fields": [
      |                 {"name": "value","type": "long"}
      |              ]
      |          }
      |        }
      |      }
      |    ]
      |  }
    """.stripMargin) class MessageAvroNested


}

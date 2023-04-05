package parallelai.sot.executor.example

import com.google.api.services.pubsub.Pubsub
import com.google.api.services.pubsub.model.PublishRequest
import com.google.api.services.pubsub.model.PubsubMessage
import com.google.common.collect.ImmutableMap
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import com.google.common.base.Throwables
import org.apache.avro.io._
import java.io._

import com.spotify.scio.avro.types.AvroType
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.specific.SpecificDatumWriter
import parallelai.sot.executor.utils.AvroUtils
import parallelai.sot.types.{ProtobufType, HasProtoAnnotation}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

/**
  * This is a generator that simulates usage data from a mobile game, and either publishes the data
  * to a pubsub topic or writes it to a file.
  *
  * <p>The general model used by the generator is the following. There is a set of teams with team
  * members. Each member is scoring points for their team. After some period, a team will dissolve
  * and a new one will be created in its place. There is also a set of 'Robots', or spammer users.
  * They hop from team to team. The robots are set to have a higher 'click rate' (generate more
  * events) than the regular team members.
  *
  * <p>Each generated line of data has the following form:
  * username,teamname,score,timestamp_in_ms,readable_time
  * e.g.:
  * user2_AsparagusPig,AsparagusPig,10,1445230923951,2015-11-02 09:09:28.224
  *
  * <p>The Injector writes either to a PubSub topic, or a file. It will use the PubSub topic if
  * specified. It takes the following arguments:
  * {@code Injector project-name (topic-name|none) (filename|none)}.
  *
  * <p>To run the Injector in the mode where it publishes to PubSub, you will need to authenticate
  * locally using project-based service account credentials to avoid running over PubSub
  * quota.
  * See https://developers.google.com/identity/protocols/application-default-credentials
  * for more information on using service account credentials. Set the GOOGLE_APPLICATION_CREDENTIALS
  * environment variable to point to your downloaded service account credentials before starting the
  * program, e.g.:
  * {@code export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/credentials-key.json}.
  * If you do not do this, then your injector will only run for a few minutes on your
  * 'user account' credentials before you will start to see quota error messages like:
  * "Request throttled due to user QPS limit being reached", and see this exception:
  * ".com.google.api.client.googleapis.json.GoogleJsonResponseException: 429 Too Many Requests".
  * Once you've set up your credentials, run the Injector like this":
  * <pre>{@code
  * Injector <project-name> <topic-name> none
 * }
  * </pre>
  * The pubsub topic will be created if it does not exist.
  *
  * <p>To run the injector in write-to-file-mode, set the topic name to "none" and specify the
  * filename:
  * <pre>{@code
  * Injector <project-name> none <filename>
 * }
  * </pre>
  */

@AvroType.fromSchema("{\"name\":\"Message\",\"doc\":\"A basic schema for storing user records\",\"fields\":[{\"name\":\"user\",\"type\":\"string\",\"doc\":\"Name of the user\"},{\"name\":\"teamName\",\"type\":\"string\",\"doc\":\"Name of the team\"},{\"name\":\"score\",\"type\":\"long\",\"doc\":\"User score\"},{\"name\":\"eventTime\",\"type\":\"long\",\"doc\":\"time when event created\"},{\"name\":\"eventTimeStr\",\"type\":\"string\",\"doc\":\"event time string for debugging\"}],\"type\":\"record\",\"namespace\":\"parallelai.sot.avro\"}")
class MessageAvro

@ProtobufType.fromSchema("{\"type\":\"protobufdefinition\",\"name\":\"NestedClass\",\"fields\":[{\"mode\":\"required\",\"type\":\"Long\",\"name\":\"value\"}]}")
class NestedClass

@ProtobufType.fromSchema("{\"type\":\"protobufdefinition\",\"name\":\"MessageProto\",\"fields\":[{\"mode\":\"required\",\"type\":\"String\",\"name\":\"user\"},{\"mode\":\"required\",\"type\":\"String\",\"name\":\"teamName\"},{\"mode\":\"required\",\"type\":\"Long\",\"name\":\"score\"},{\"mode\":\"required\",\"type\":\"Long\",\"name\":\"eventTime\"},{\"mode\":\"required\",\"type\":\"String\",\"name\":\"eventTimeStr\"},{\"mode\":\"required\",\"type\":\"NestedClass\",\"name\":\"nestedValue\"}]}")
class MessageProto

class Injector(project: String, topicName: Option[String], fileName: Option[String], serialiser: String) {

  val r = scala.util.Random

  require(topicName.isDefined ^ fileName.isDefined)

  val avroT = AvroType[MessageAvro]
  val schemaStr = avroT.schema.toString

  private var pubsub: Pubsub = _
  private var topic: String = _
  private val TIMESTAMP_ATTRIBUTE = "timestamp_ms"

  // QPS ranges from 100 to 50.
  private val MIN_QPS = 100
  private val QPS_RANGE = 50
  // How long to sleep, in ms, between creation of the threads that make API requests to PubSub.
  private val THREAD_SLEEP_MS = 1000

  // The list of live teams.
  private var liveTeams: ListBuffer[Injector.TeamInfo] = new ListBuffer[Injector.TeamInfo]()

  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone("PST")))

  if (topicName.isDefined) {
    // Create the PubSub client.
    pubsub = InjectorUtils.getClient()
    // Create the PubSub topic as necessary.
    topic = InjectorUtils.getFullyQualifiedTopicName(project, topicName.get)
    InjectorUtils.createTopic(pubsub, topic)
    println("Injecting to topic: " + topic)
  } else {
    println("Writing to file: " + fileName)
  }

  // Start off with some random live teams.
  while (liveTeams.length < Injector.NUM_LIVE_TEAMS) {
    addLiveTeam()
  }

  /**
    * Get and return a random team. If the selected team is too old w.r.t its expiration, remove
    * it, replacing it with a new team.
    */
  private def randomTeam() = {
    val index = Injector.random.nextInt(liveTeams.size)
    val team = liveTeams(index)
    // If the selected team is expired, remove it and return a new team.
    val currTime = System.currentTimeMillis
    if ((team.endTimeInMillis < currTime) || team.numMembers == 0) {
      println("\nteam " + team + " is too old; replacing.")
      println("start time: " + team.startTimeInMillis + ", end time: " + team.endTimeInMillis + ", current time:" + currTime)
      removeTeam(index)
      // Add a new team in its stead.
      addLiveTeam
    }
    else team
  }

  /**
    * Create and add a team. Possibly add a robot to the team.
    */
  def addLiveTeam() = synchronized {
    val teamName = Injector.randomElement(Injector.COLORS) + Injector.randomElement(Injector.ANIMALS)

    // Decide if we want to add a robot to the team.
    val robot = if (Injector.random.nextInt(Injector.ROBOT_PROBABILITY) == 0) Some("Robot-" + Injector.random.nextInt(Injector.NUM_ROBOTS))
    else None

    // Create the new team.
    val newTeam = new Injector.TeamInfo(teamName, System.currentTimeMillis(), robot)
    liveTeams += newTeam
    println("[+" + newTeam + "]")
    newTeam
  }

  /**
    * Remove a specific team.
    */
  private def removeTeam(teamIndex: Int) = {
    val removedTeam = liveTeams.remove(teamIndex)
    println("[-" + removedTeam + "]")
  }

  /** Generate a user gaming event. */
  def generateEventProto(currTime: Long, delayInMillis: Int): MessageProto = {
    val team = randomTeam()

    val robot = team.robot
    // If the team has an associated robot team member...
    val user = robot match {
      case Some(r) =>
        // Then use that robot for the message with some probability.
        // Set this probability to higher than that used to select any of the 'regular' team
        // members, so that if there is a robot on the team, it has a higher click rate.
        if (Injector.random.nextInt(team.numMembers / 2) == 0) r
        else team.getRandomUser
      case None =>
        // No robot
        team.getRandomUser
    }
    val message = addTimeInfoToEventProto(user, team.teamName, Injector.random.nextInt(Injector.MAX_SCORE), currTime, delayInMillis)
    message
  }


  /**
    * Add time info to a generated gaming event.
    */
  def addTimeInfoToEventProto(user: String, teamName: String, score: Long, currTime: Long, delayInMillis: Int): MessageProto = {
    val eventTime = (currTime - delayInMillis) / 1000 * 1000
    // Add a (redundant) 'human-readable' date string to make the data semantics more clear.
    val dateString = fmt.print(currTime)
    val e = NestedClass(r.nextLong())
    MessageProto(user, teamName, score, eventTime, dateString, e)
  }

  /** Generate a user gaming event. */
  def generateEventAvro(currTime: Long, delayInMillis: Int): GenericRecord = {
    val team = randomTeam()

    val robot = team.robot
    // If the team has an associated robot team member...
    val user = robot match {
      case Some(r) =>
        // Then use that robot for the message with some probability.
        // Set this probability to higher than that used to select any of the 'regular' team
        // members, so that if there is a robot on the team, it has a higher click rate.
        if (Injector.random.nextInt(team.numMembers / 2) == 0) r
        else team.getRandomUser
      case None =>
        // No robot
        team.getRandomUser
    }
    val message = addTimeInfoToEventAvro(user, team.teamName, Injector.random.nextInt(Injector.MAX_SCORE), currTime, delayInMillis)
    message
  }


  /**
    * Add time info to a generated gaming event.
    */
  def addTimeInfoToEventAvro(user: String, teamName: String, score: Long, currTime: Long, delayInMillis: Int): GenericRecord = {
    val eventTime = (currTime - delayInMillis) / 1000 * 1000
    // Add a (redundant) 'human-readable' date string to make the data semantics more clear.
    val dateString = fmt.print(currTime)
    avroT.toGenericRecord(MessageAvro(user, teamName, score, eventTime, dateString))
  }

  /**
    * Publish 'numMessages' arbitrary events from live users with the provided delay, to a
    * PubSub topic.
    */
  def publishDataAvro(numMessages: Int, delayInMillis: Int): Unit = {
    val pubsubMessages = for (i <- 0 until Math.max(1, numMessages)) yield {
      val currTime = System.currentTimeMillis()
      val message = generateEventAvro(currTime, delayInMillis)
      val pubsubMessage = new PubsubMessage().encodeData(AvroUtils.encodeAvro(message, schemaStr))
      pubsubMessage.setAttributes(ImmutableMap.of(TIMESTAMP_ATTRIBUTE, ((currTime - delayInMillis) / 1000 * 1000).toString))
      if (delayInMillis != 0) {
        println(pubsubMessage.getAttributes())
        println("late data for: " + message)
      }
      pubsubMessage
    }
    val publishRequest = new PublishRequest()
    publishRequest.setMessages(pubsubMessages.asJava)
    pubsub.projects().topics().publish(topic, publishRequest).execute()
  }

  def publishDataProto(numMessages: Int, delayInMillis: Int): Unit = {

    import cats.instances.list._
    import cats.instances.option._
    import parallelai.sot.executor.protobuf._

    val pubsubMessages = for (i <- 0 until Math.max(1, numMessages)) yield {
      val currTime = System.currentTimeMillis()
      val message = generateEventProto(currTime, delayInMillis)
      val pubsubMessage = new PubsubMessage().encodeData(message.toPB)
      pubsubMessage.setAttributes(ImmutableMap.of(TIMESTAMP_ATTRIBUTE, ((currTime - delayInMillis) / 1000 * 1000).toString))
      if (delayInMillis != 0) {
        println(pubsubMessage.getAttributes())
        println("late data for: " + message)
      }
      pubsubMessage
    }
    val publishRequest = new PublishRequest()
    publishRequest.setMessages(pubsubMessages.asJava)
    pubsub.projects().topics().publish(topic, publishRequest).execute()
  }

  /**
    * Publish generated events to a file.
    */
  def publishDataToFile(fileName: String, numMessages: Int, delayInMillis: Int) {
    val out = new PrintWriter(new OutputStreamWriter(
      new BufferedOutputStream(new FileOutputStream(fileName, true)), "UTF-8"))
    try {
      for (i <- 0 until Math.max(1, numMessages)) {
        val currTime = System.currentTimeMillis()
        val message = generateEventAvro(currTime, delayInMillis)
        println(message)
      }
    } catch {
      case (e: Exception) => e.printStackTrace()
    } finally {
      if (out != null) {
        out.flush()
        out.close()
      }
    }
  }

  def run() = {

    // Publish messages at a rate determined by the QPS and Thread sleep settings.
    for (i <- 0 until Int.MaxValue) {
      if (Thread.activeCount() > 10) println("I'm falling behind!")

      // Decide if this should be a batch of late data.
      val (numMessages, delayInMillis) = if (i % Injector.LATE_DATA_RATE == 0) {
        // Insert delayed data for one user (one message only)
        val delayMs = Injector.BASE_DELAY_IN_MILLIS + Injector.random.nextInt(Injector.FUZZY_DELAY_IN_MILLIS)
        println("DELAY(" + delayMs + ", " + 1 + ")")
        (1, delayMs)
      } else {
        print(".")
        val nMsg = MIN_QPS + Injector.random.nextInt(QPS_RANGE)
        (nMsg, 0)
      }

      if (fileName.isDefined) { // Won't use threading for the file write.
        publishDataToFile(fileName.get, numMessages, delayInMillis)
      } else { // Write to PubSub.
        // Start a thread to inject some data.
        new Thread() {
          override def run() {
            try {
              val res = if (serialiser == "avro") {
                publishDataAvro(numMessages, delayInMillis)
              } else if (serialiser == "proto") {
                publishDataProto(numMessages, delayInMillis)
              }
            } catch {
              case (e: IOException) => System.err.println(e)
            }
          }
        }.start()
      }
      // Wait before creating another injector thread.
      Thread.sleep(THREAD_SLEEP_MS)
    }
  }

}


object Injector {

  // The total number of robots in the system.
  private val NUM_ROBOTS = 20
  // Determines the chance that a team will have a robot team member.
  private val ROBOT_PROBABILITY = 3
  private val NUM_LIVE_TEAMS = 15
  private val BASE_MEMBERS_PER_TEAM = 5
  private val MEMBERS_PER_TEAM = 15
  private val MAX_SCORE = 20
  private val LATE_DATA_RATE = 5 * 60 * 2 // Every 10 minutes

  private val BASE_DELAY_IN_MILLIS = 5 * 60 * 1000 // 5-10 minute delay

  private val FUZZY_DELAY_IN_MILLIS = 5 * 60 * 1000

  // The minimum time a 'team' can live.
  private val BASE_TEAM_EXPIRATION_TIME_IN_MINS = 20
  private val TEAM_EXPIRATION_TIME_IN_MINS = 20

  // Lists used to generate random team names.
  private val COLORS = List("Magenta", "AliceBlue", "Almond", "Amaranth", "Amber", "Amethyst", "AndroidGreen", "AntiqueBrass",
    "Fuchsia", "Ruby", "AppleGreen", "Apricot", "Aqua", "ArmyGreen", "Asparagus", "Auburn", "Azure", "Banana", "Beige",
    "Bisque", "BarnRed", "BattleshipGrey")

  private val ANIMALS = List("Echidna", "Koala", "Wombat", "Marmot", "Quokka", "Kangaroo", "Dingo", "Numbat", "Emu",
    "Wallaby", "CaneToad", "Bilby", "Possum", "Cassowary", "Kookaburra", "Platypus", "Bandicoot", "Cockatoo", "Antechinus")

  private val random = new java.util.Random()

  /**
    * A class for holding team info: the name of the team, when it started,
    * and the current team members. Teams may but need not include one robot team member.
    */
  class TeamInfo(val teamName: String,
                 val startTimeInMillis: Long,
                 val expirationPeriod: Int,
                 val robot: Option[String], // The team might but need not include 1 robot. Will be non-null if so.
                 val numMembers: Int) {

    def this(teamName: String, startTimeInMillis: Long, robot: Option[String]) = {
      this(teamName, startTimeInMillis, Injector.random.nextInt(TEAM_EXPIRATION_TIME_IN_MINS), robot,
        Injector.random.nextInt(MEMBERS_PER_TEAM) + BASE_MEMBERS_PER_TEAM)
    }

    def endTimeInMillis = startTimeInMillis + (expirationPeriod * 60 * 1000)

    def getRandomUser = {
      val userNum = Injector.random.nextInt(numMembers)
      "user" + userNum + "_" + teamName
    }

    override def toString() = {
      "(" + teamName + ", num members: " + numMembers + ", starting at: " +
        startTimeInMillis + ", expires in: " + expirationPeriod + ", robot: " + robot + ")"
    }
  }

  /** Utility to grab a random element from an array of Strings. */
  def randomElement(list: List[String]) = {
    val index = random.nextInt(list.length)
    list(index)
  }


  def main(args: Array[String]): Unit = {

    if (args.length < 3) {
      println("Usage: Injector project-name (topic-name|none) (filename|none) (avro|proto)")
      System.exit(1)
    }
    val project = args(0)
    val topicName = if (args(1) == "none") None else Some(args(1))
    val fileName = if (args(2) == "none") None else Some(args(2))
    val serialiser = args(3)

    println("Starting Injector")
    val i = new Injector(project, topicName, fileName, serialiser)
    i.run()

  }

}


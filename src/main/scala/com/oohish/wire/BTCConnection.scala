package com.oohish.wire

import java.net.InetAddress
import java.net.InetSocketAddress
import scala.Array.canBuildFrom
import scala.math.BigInt.int2bigInt
import scala.util.Random
import com.oohish.wire.PeerManager.PeerConnected
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import com.oohish.bitcoinscodec.structures.Message._
import com.oohish.bitcoinscodec.messages._
import com.oohish.bitcoinscodec.structures._

object BTCConnection {
  def props(peer: Peer, networkParams: NetworkParameters, node: ActorRef, manager: ActorRef) =
    Props(classOf[BTCConnection], peer, networkParams, node, manager)

  case class ConnectTimeout()
  case class Outgoing(m: Message)

  def verack = Verack()

  def version(networkParams: NetworkParameters, peer: Peer) = Version(
    1, //versionNum(networkParams),
    Node.services,
    123456L, //DateTime.now().getMillis(),
    peerNetworkAddress(peer),
    myNetworkAddress,
    genNonce,
    "/Satoshi:0.7.2/",
    1,
    None)

  def versionNum(networkParams: NetworkParameters): Int =
    networkParams.PROTOCOL_VERSION.toInt

  def peerNetworkAddress(peer: Peer): NetworkAddress = {
    NetworkAddress(
      1,
      Left(IPV4(peer.address.getAddress().getHostAddress())),
      Port(peer.port))
  }

  def myNetworkAddress: NetworkAddress = peerNetworkAddress(selfPeer)

  def genNonce(): BigInt = {
    val bytes: Array[Byte] = Array.fill(8)(0)
    Random.nextBytes(bytes)
    BigInt(0.toByte +: bytes)
  }

  val selfPeer = Peer(new InetSocketAddress(InetAddress.getLocalHost(), 8333))

}

class BTCConnection(peer: Peer, networkParams: NetworkParameters, node: ActorRef, manager: ActorRef) extends Actor with ActorLogging {
  import BTCConnection._
  import akka.actor.Terminated
  import Node.Incoming

  context.parent ! BTCConnection.version(networkParams, peer)

  def receive = connecting(false, None)

  def connecting(verackReceived: Boolean, versionReceived: Option[Version]): Receive = {

    case _: Verack => {
      if (versionReceived.isDefined) {
        val version = versionReceived.get
        finishHandshake(version.version, version.timestamp)
      } else {
        context.become(connecting(true, None))
      }
    }

    case m: Version => {
      context.parent ! BTCConnection.verack
      if (verackReceived) {
        finishHandshake(m.version, m.timestamp)
      } else {
        context.parent ! BTCConnection.version(networkParams, peer)
        context.become(connecting(false, Some(m)))
      }
    }

    case m: ConnectTimeout => {
      context.stop(self)
    }

    case other => {
      log.debug("BTCConnection got other: " + other)
    }

  }

  def finishHandshake(version: Int, time: Long): Unit = {
    manager ! PeerConnected(peer, time)
    node ! Verack()
    // take the minimum of the client version and the connected peer's version.
    val negotiatedVersion = Math.min(networkParams.PROTOCOL_VERSION, version).toInt
    log.info("becoming connected with protocol version {}", negotiatedVersion)
    context.become(connected(negotiatedVersion))
  }

  def connected(version: Int): Receive = {

    case Outgoing(m) => {
      log.debug("outgoing message: " + m)
      context.parent ! m
    }

    case m: Message => {
      node ! m
    }

    case Terminated(ref) => {
      context.stop(self)
    }

    case other => {
      log.warning("BTCConnection got other: " + other)
    }

  }

}
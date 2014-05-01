package com.oohish.structures

import akka.util.ByteIterator
import akka.util.ByteString

/*
 * TxIn
 */
object TxIn extends StructureReader[TxIn] {

  def decode(it: ByteIterator): TxIn = {
    TxIn(
      OutPoint.decode(it),
      VarStr.decode(it).s,
      uint32_t.decode(it).n)
  }
}

case class TxIn(
  previous_output: OutPoint,
  sig_script: String,
  sequence: Long) extends Structure {

  def encode: ByteString = {
    val bb = ByteString.newBuilder
    bb ++= previous_output.encode
    bb ++= VarStr(sig_script).encode
    bb ++= uint32_t(sequence).encode
    bb.result
  }

}
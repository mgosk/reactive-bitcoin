package com.oohish.chain

import java.security.MessageDigest
import akka.util.ByteString
import com.oohish.structures.char32
import com.oohish.util.HexBytesUtil
import com.oohish.peermessages.Block
import com.oohish.structures.VarStruct
import com.oohish.peermessages.Tx
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Chain {

  /**
   * Calculate the hash of a block.
   */
  def blockHash(block: Block): char32 = {
    val bb = ByteString.newBuilder
    bb ++= block.version.encode
    bb ++= block.prev_block.encode
    bb ++= block.merkle_root.encode
    bb ++= block.timestamp.encode
    bb ++= block.bits.encode
    bb ++= block.nonce.encode
    val hashByteString = bb.result

    val messageDigest = MessageDigest.getInstance("SHA-256")
    val headerBytes: Array[Byte] = hashByteString.compact.toParArray.toArray
    val hash1 = messageDigest.digest(headerBytes)
    val hash2 = messageDigest.digest(hash1)
    char32(hash2.toList.reverse)
  }

  /**
   *   returns the list of indexes of blocks to be used in the block locator.
   */
  def blockLocatorIndices(chainLength: Int): List[Int] = {
    0 :: blockLocatorIndicesHelper(List(), chainLength - 1, 1, 0)
  }

  /**
   * helper function for getting block locator indices.
   */
  private def blockLocatorIndicesHelper(acc: List[Int], i: Int, step: Int, start: Int): List[Int] = {
    if (i < 0) {
      acc
    } else if (start >= 10) {
      blockLocatorIndicesHelper(i :: acc, i - step, step * 2, start + 1)
    } else {
      blockLocatorIndicesHelper(i :: acc, i - step, step, start + 1)
    }
  }

  /*
   * return the previous storedblock from a given storedblock
   */
  def prevStoredBlock(store: BlockStore, sb: StoredBlock): Future[Option[StoredBlock]] = {
    store.get(sb.block.prev_block)
  }

  /*
   * useful function
   */
  def futureCons[T](flst: Future[List[T]], x: T)(implicit ec: ExecutionContext): Future[List[T]] = {
    flst.flatMap { lst =>
      Future(x :: lst)
    }
  }

  /*
   * return a block locator for the block store.
   */
  def blockLocator(store: BlockStore)(implicit ec: ExecutionContext): Future[VarStruct[char32]] = {
    val maxHeight = store.getChainHead.map(_.height).getOrElse(0)
    val locatorIndices = Chain.blockLocatorIndices(maxHeight + 1)

    def blockLocatorHelper(acc: Future[List[char32]], maybeCur: Option[StoredBlock]): Future[List[char32]] = {
      maybeCur.map { cur =>
        val futurePrev = prevStoredBlock(store, cur)
        futurePrev.flatMap { maybePrev =>
          if (cur.height == 0) {
            blockLocatorHelper(futureCons(acc, Chain.blockHash(cur.block)), maybePrev)
          } else if (locatorIndices.contains(cur.height)) {
            blockLocatorHelper(futureCons(acc, Chain.blockHash(cur.block)), maybePrev)
          } else {
            blockLocatorHelper(acc, maybePrev)
          }
        }
      }.getOrElse(acc)
    }

    blockLocatorHelper(Future(List()), store.getChainHead).map { blockList =>
      VarStruct[char32](blockList.reverse)
    }
  }

  val emptyHashStop = char32(HexBytesUtil.hex2bytes("0000000000000000000000000000000000000000000000000000000000000000").toList)

}
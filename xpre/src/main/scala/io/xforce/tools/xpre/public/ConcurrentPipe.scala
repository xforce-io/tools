package io.xforce.tools.xpre.public

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import scala.collection.mutable.ArrayBuffer
import scala.util.Random.nextInt

class ConcurrentPipe[T](
    private val maxNumItems_ :Int = 100000,
    private val numBuckets_ :Int = 100) {

  require(numBuckets_ != 0, "numBuckets_can't_be_0")

  private val lists_ = new Array[java.util.LinkedList[T]](numBuckets_)
  private val locks_ = new Array[ReentrantLock](numBuckets_)
  private val numElms_ = new AtomicInteger(0)

  for (i <- 0 until numBuckets_) lists_(i) = new java.util.LinkedList[T]
  for (i <- 0 until numBuckets_) locks_(i) = new ReentrantLock()

  def push(buf :T): Boolean= {
    if (numElms_.get >= maxNumItems_) {
      return false
    }

    val index = getArea_(nextInt(numBuckets_))
    locks_(index).lock()
    lists_(index).addFirst(buf)
    locks_(index).unlock()
    numElms_.addAndGet(1)
    true
  }

  def pushBatch(bufs :Array[T]): Boolean= {
    if (numElms_.get + bufs.length >= maxNumItems_) {
      return false
    }

    val index = getArea_(nextInt(numBuckets_))
    locks_(index).lock()
    bufs.foreach (buf => lists_(index).addFirst(buf))
    locks_(index).unlock()
    numElms_.addAndGet(bufs.length)
    true
  }

  def pushAtTail(buf :T): Boolean= {
    if (numElms_.get >= maxNumItems_) {
      return false
    }

    val index = getArea_(nextInt(numBuckets_))
    locks_(index).lock()
    lists_(index).addLast(buf)
    locks_(index).unlock()
    numElms_.addAndGet(1)
    true
  }

  def pushBatchAtTail(bufs :Array[T]): Boolean= {
    if (numElms_.get + bufs.length >= maxNumItems_) {
      return false
    }

    val index = getArea_(nextInt(numBuckets_))
    locks_(index).lock()
    bufs.foreach (buf => lists_(index).addLast(buf))
    locks_(index).unlock()
    numElms_.addAndGet(bufs.length)
    true
  }

  def pop(): Option[T]= {
    var index = getArea_(nextInt(numBuckets_))
    val start_index=index
    while (true) {
      if (locks_(index).tryLock() == true) {
        val buf = lists_(index).pollLast()
        locks_(index).unlock()
        if (buf != null) {
          numElms_.addAndGet(-1)
          return Some(buf)
        }
      }

      index = (index+1) % numBuckets_
      if (index==start_index) {
        return None
      }
    }
    None
  }

  def popBatch(batch_size :Int=100): Option[ArrayBuffer[T]] = {
    val results = new ArrayBuffer[T]
    var numElmsTaken = 0
    var index = getArea_(nextInt(numBuckets_))
    val start_index = index
    while ( (numElmsTaken<batch_size) && (numElms_.get() > 0) ) {
      if (locks_(index).tryLock()) {
        val oldNumElmsTaken = numElmsTaken
        var break=false
        do {
          val buf = lists_(index).pollLast()
          if (buf!=null) {
            results.append(buf)
            numElmsTaken += 1
          } else {
            break=true
          }
        } while ( (numElmsTaken < batch_size) && !break )
        locks_(index).unlock()
        numElms_.addAndGet(oldNumElmsTaken-numElmsTaken)
      }

      index = (index+1) % numBuckets_
      if (index==start_index) {
        return if (results.length != 0) Some(results) else None
      }
    }
    return if (results.length != 0) Some(results) else None
  }

  def length = numElms_ get

  def space = maxNumItems_ - length

  private def getArea_(index :Int) = index % numBuckets_
}

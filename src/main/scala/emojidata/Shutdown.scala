package emojidata

import java.util

class Shutdown extends Thread {
  private val closeables = new util.ArrayDeque[AutoCloseable]()

  def +=(closeable: AutoCloseable): Unit = {
    closeables.add(closeable)
  }

  override def run(): Unit = {
    while (!closeables.isEmpty) {
      val closeable = closeables.removeLast()
      try {
        closeable.close()
      } catch {
        case x: Exception => println(x.getMessage)
      }
    }
  }
}

package edu.cwru.raycaster

import java.security.InvalidParameterException

val <A, B> Pair<A, B>.size: Int
  get() = 2

private operator fun <T> Pair<T, T>.get(index: Int): T =
    when (index) {
      0 -> this.first
      1 -> this.second
      else -> throw InvalidParameterException("index was not 1 or 2")
    }

class PairIterator<T>(private val p: Pair<T, T>) : Iterator<T> {
  private var index = 0

  override fun hasNext(): Boolean {
    return index < p.size
  }

  override fun next(): T {
    return p[index++]
  }
}

operator fun <T> Pair<T, T>.iterator(): Iterator<T> {
  return PairIterator(this)
}

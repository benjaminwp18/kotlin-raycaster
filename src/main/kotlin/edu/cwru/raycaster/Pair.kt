package edu.cwru.raycaster

import java.security.InvalidParameterException

val <A, B> Pair<A, B>.size: Int
  get() = 2

private operator fun <T> Pair<T, T>.get(index: Int): T =
    when (index) {
      0 -> first
      1 -> second
      else -> throw InvalidParameterException("Index into a Pair must be 0 or 1, got $index")
    }

operator fun <T> Pair<T, T>.iterator() = object : Iterator<T> {
  private var index = 0
  override fun hasNext() = index < size
  override fun next() = get(index++)
}

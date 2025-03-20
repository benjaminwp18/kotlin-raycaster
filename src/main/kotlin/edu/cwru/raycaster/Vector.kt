package edu.cwru.raycaster

import java.security.InvalidParameterException





open class Vec2<T>(inputs: Iterable<T>) : Iterable<T> where T : Comparable<T>, T : Number {
  private val point: Pair<T, T>

  init {
    var first: T? = null
    var second: T? = null
    for ((i, e) in inputs.withIndex()) {
      when (i) {
        0 -> first = e
        1 -> second = e
        else -> throw InvalidParameterException("Got an inputs longer than 2")
      }
    }
    point = Pair(first!!, second!!)
  }

  val size: Int
    get() = 2

  open val x = point.first
  open val y = point.second

  constructor(x: T, y: T) : this(listOf(x, y))

  constructor(p: Pair<T, T>) : this(p.first, p.second)

  override fun toString(): String {
    return "Vec2(x: ${this.x}, y: ${this.y})"
  }

  override fun iterator(): Iterator<T> {
    return object : Iterator<T> {
      private var index = 0

      override fun hasNext(): Boolean {
        return index < this@Vec2.size
      }

      override fun next(): T {
        return this@Vec2[index++]
      }
    }
  }

  private operator fun get(index: Int): T =
      when (index) {
        0 -> this.x
        1 -> this.y
        else -> throw InvalidParameterException("index was not 1 or 2")
      }

//  operator fun plus(vec2: Vec2<T>): Vec2<T> {
//    if (vec2.x is Int && this.x is Int){
//      return Vec2<T>(this.x + vec2.x, this.y + vec2.y)
//    }
//    return Vec2(this.x + vec2.x, this.y + vec2.y)
//  }
}

 data class MutableVec2<T>(override var x: T, override var y: T) :
    Vec2<T>(Pair(x, y)) where T: Comparable<T>, T : Number {
  fun clamp(xMin: T, xMax: T, yMin: T, yMax: T) {
    x = minOf(maxOf(x, xMin), xMax)
    y = minOf(maxOf(y, yMin), yMax)
  }
 }

fun main(args: Array<String>) {
  val d = 3.0
  val e = 5.0
  Vec2(d, e)
//  val c = Vec2(3, 4)
//  var a = Vec2(listOf(1, 2))
//  println(a)
//  a = Vec2(2, 3)
//  println(a)
//  println(Vec2(a))
//
//  for (p in Pair(3, 4)) {
//    println(p)
//  }
//  a = Vec2(1, 2)
//  val b = Vec2(Pair(3, 4))
//  println(a - b)
}

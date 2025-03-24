package edu.cwru.raycaster

import java.security.InvalidParameterException
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

open class Vec2<T>(inputs: Iterable<T>) where T : Number {
  val point: Pair<T, T>

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

  constructor(vec2: Vec2<T>) : this(vec2.x, vec2.y)

  override fun toString(): String {
    return "Vec2(x: ${this.x}, y: ${this.y})"
  }

  fun iterator(): Iterator<T> {
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

  operator fun get(index: Int): T =
      when (index) {
        0 -> this.x
        1 -> this.y
        else -> throw InvalidParameterException("index was not 1 or 2")
      }

  val magnitude: Double
    get() = sqrt(this.x.toDouble().pow(2) + this.y.toDouble().pow(2))
}

interface VectorOperations<T : Number> {

  val x: T
  val y: T

  operator fun plus(vec: Vec2<T>): Vec2<T>

  operator fun minus(vec: Vec2<T>): Vec2<T>

  operator fun times(value: T): Vec2<T>

  operator fun div(value: T): Vec2<T>

  val magnitude: Double

  fun dot(vec: Vec2<T>): T

  fun angleBetween(vec: Vec2<T>): Double
}

interface VectorOperationsInt : VectorOperations<Int> {

  override fun plus(vec: Vec2<Int>): Vec2Int {
    return Vec2Int(this.x + vec.x, this.y + vec.y)
  }

  override fun minus(vec: Vec2<Int>): Vec2Int {
    return Vec2Int(this.x - vec.x, this.y - vec.y)
  }

  override fun times(value: Int): Vec2Int {
    return Vec2Int(this.x * value, this.y * value)
  }

  override fun div(value: Int): Vec2Int {
    return Vec2Int(this.x / value, this.y / value)
  }

  override fun dot(vec: Vec2<Int>): Int {
    return this.x * vec.x + this.y * vec.y
  }

  override fun angleBetween(vec: Vec2<Int>): Double {
    return acos(this.dot(vec) / (this.magnitude * vec.magnitude))
  }
}

interface VectorOperationsDouble : VectorOperations<Double> {

  override fun plus(vec: Vec2<Double>): Vec2Double {
    return Vec2Double(this.x + vec.x, this.y + vec.y)
  }

  override fun minus(vec: Vec2<Double>): Vec2Double {
    return Vec2Double(this.x - vec.x, this.y - vec.y)
  }

  override fun times(value: Double): Vec2Double {
    return Vec2Double(this.x * value, this.y * value)
  }

  override fun div(value: Double): Vec2Double {
    return Vec2Double(this.x / value, this.y / value)
  }

  override fun dot(vec: Vec2<Double>): Double {
    return this.x * vec.x + this.y * vec.y
  }

  override fun angleBetween(vec: Vec2<Double>): Double {
    return acos(this.dot(vec) / (this.magnitude * vec.magnitude))
  }
}

class Vec2Int(inputs: Iterable<Int>) : Vec2<Int>(inputs), VectorOperationsInt {

  constructor(x: Int, y: Int) : this(listOf(x, y))

  constructor(p: Pair<Int, Int>) : this(p.first, p.second)

  constructor(vec2: Vec2<Int>) : this(vec2.x, vec2.y)
}

class Vec2Double(inputs: Iterable<Double>) : Vec2<Double>(inputs), VectorOperationsDouble {

  constructor(x: Double, y: Double) : this(listOf(x, y))

  constructor(p: Pair<Double, Double>) : this(p.first, p.second)

  constructor(vec2: Vec2<Double>) : this(vec2.x, vec2.y)
}

open class MutableVec2<T>(inputs: Iterable<T>) : Vec2<T>(inputs) where
T : Comparable<T>,
T : Number {

  override var x: T = super.point.first

  override var y: T = super.point.second

  operator fun set(index: Int, value: T) =
      when (index) {
        0 -> this.x = value
        1 -> this.y = value
        else -> throw InvalidParameterException("index was not 1 or 2")
      }

  fun clamp(xMin: T, xMax: T, yMin: T, yMax: T) {
    x = minOf(maxOf(x, xMin), xMax)
    y = minOf(maxOf(y, yMin), yMax)
  }
}

class MutableVec2Int(inputs: Iterable<Int>) : MutableVec2<Int>(inputs), VectorOperationsInt {

  constructor(x: Int, y: Int) : this(listOf(x, y))

  constructor(p: Pair<Int, Int>) : this(p.first, p.second)

  constructor(vec2: Vec2<Int>) : this(vec2.x, vec2.y)
}

class MutableVec2Double(inputs: Iterable<Double>) :
    MutableVec2<Double>(inputs), VectorOperationsDouble {

  constructor(x: Double, y: Double) : this(listOf(x, y))

  constructor(p: Pair<Double, Double>) : this(p.first, p.second)

  constructor(vec2: Vec2<Double>) : this(vec2.x, vec2.y)
}

fun main(args: Array<String>) {
  println(Vec2Int(3, 4) + Vec2(3, 4))

  var a = MutableVec2Int(3, 4)
  a.x = 7
  println(a)
}

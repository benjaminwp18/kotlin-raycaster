package edu.cwru.raycaster

import java.security.InvalidParameterException
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

open class Vec2<T>(inputs: Iterable<T>) where T : Number {
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

  operator fun plus(vec: Vec2<T>): Vec2<T>

  operator fun minus(vec: Vec2<T>): Vec2<T>

  operator fun times(value: T): Vec2<T>

  operator fun div(value: T): Vec2<T>

  fun dot(vec: Vec2<T>): T

  fun angleBetween(vec: Vec2<T>): Double
}

class Vec2Int(inputs: Iterable<Int>) : Vec2<Int>(inputs), VectorOperations<Int> {

  constructor(x: Int, y: Int) : this(listOf(x, y))

  constructor(p: Pair<Int, Int>) : this(p.first, p.second)

  constructor(vec2: Vec2<Int>) : this(vec2.x, vec2.y)

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

class Vec2Float(inputs: Iterable<Float>) : Vec2<Float>(inputs), VectorOperations<Float> {

  constructor(x: Float, y: Float) : this(listOf(x, y))

  constructor(p: Pair<Float, Float>) : this(p.first, p.second)

  constructor(vec2: Vec2<Float>) : this(vec2.x, vec2.y)

  override fun plus(vec: Vec2<Float>): Vec2Float {
    return Vec2Float(this.x + vec.x, this.y + vec.y)
  }

  override fun minus(vec: Vec2<Float>): Vec2Float {
    return Vec2Float(this.x - vec.x, this.y - vec.y)
  }

  override fun times(value: Float): Vec2Float {
    return Vec2Float(this.x * value, this.y * value)
  }

  override fun div(value: Float): Vec2Float {
    return Vec2Float(this.x / value, this.y / value)
  }

  override fun dot(vec: Vec2<Float>): Float {
    return this.x * vec.x + this.y * vec.y
  }

  override fun angleBetween(vec: Vec2<Float>): Double {
    return acos(this.dot(vec) / (this.magnitude * vec.magnitude))
  }
}

open class MutableVec2<T>(inputs: Iterable<T>) : Vec2<T>(inputs) where
T : Comparable<T>,
T : Number {

  constructor(x: T, y: T) : this(listOf(x, y))

  constructor(p: Pair<T, T>) : this(p.first, p.second)

  constructor(vec2: Vec2<T>) : this(vec2.x, vec2.y)

  override var x: T
    set(value) {
      MutableVec2(value, this.y)
    }
    get() = super.x

  override var y: T
    set(value) {
      MutableVec2(this.x, value)
    }
    get() = super.y

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

class MutableVec2Int(inputs: Iterable<Int>) : MutableVec2<Int>(inputs), VectorOperations<Int> {

  constructor(x: Int, y: Int) : this(listOf(x, y))

  constructor(p: Pair<Int, Int>) : this(p.first, p.second)

  constructor(vec2: Vec2<Int>) : this(vec2.x, vec2.y)

  override fun plus(vec: Vec2<Int>): MutableVec2Int {
    return MutableVec2Int(this.x + vec.x, this.y + vec.y)
  }

  override fun minus(vec: Vec2<Int>): MutableVec2Int {
    return MutableVec2Int(this.x - vec.x, this.y - vec.y)
  }

  override fun times(value: Int): MutableVec2Int {
    return MutableVec2Int(this.x * value, this.y * value)
  }

  override fun div(value: Int): MutableVec2Int {
    return MutableVec2Int(this.x / value, this.y / value)
  }

  override fun dot(vec: Vec2<Int>): Int {
    return this.x * vec.x + this.y * vec.y
  }

  override fun angleBetween(vec: Vec2<Int>): Double {
    return acos(this.dot(vec) / (this.magnitude * vec.magnitude))
  }
}

class MutableVec2Float(inputs: Iterable<Float>) :
    MutableVec2<Float>(inputs), VectorOperations<Float> {

  constructor(x: Float, y: Float) : this(listOf(x, y))

  constructor(p: Pair<Float, Float>) : this(p.first, p.second)

  constructor(vec2: Vec2<Float>) : this(vec2.x, vec2.y)

  override fun plus(vec: Vec2<Float>): MutableVec2Float {
    return MutableVec2Float(this.x + vec.x, this.y + vec.y)
  }

  override fun minus(vec: Vec2<Float>): MutableVec2Float {
    return MutableVec2Float(this.x - vec.x, this.y - vec.y)
  }

  override fun times(value: Float): MutableVec2Float {
    return MutableVec2Float(this.x * value, this.y * value)
  }

  override fun div(value: Float): MutableVec2Float {
    return MutableVec2Float(this.x / value, this.y / value)
  }

  override fun dot(vec: Vec2<Float>): Float {
    return this.x * vec.x + this.y * vec.y
  }

  override fun angleBetween(vec: Vec2<Float>): Double {
    return acos(this.dot(vec) / (this.magnitude * vec.magnitude))
  }
}

fun main(args: Array<String>) {}

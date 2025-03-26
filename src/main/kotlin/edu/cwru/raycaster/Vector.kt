package edu.cwru.raycaster

import java.security.InvalidParameterException
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

open class Vec2<T>(inputs: Iterable<T>) where T : Comparable<T>, T : Number {
  val point: Pair<T, T> = Pair(inputs.elementAt(0), inputs.elementAt(1))

  val size = 2

  open val x = point.first
  open val y = point.second

  operator fun component1(): T = x
  operator fun component2(): T = y

  constructor(x: T, y: T) : this(listOf(x, y))
  constructor(p: Pair<T, T>) : this(p.first, p.second)
  constructor(vec2: Vec2<T>) : this(vec2.x, vec2.y)

  override fun toString() = "Vec2(x: $x, y: $y)"

  fun iterator() = object : Iterator<T> {
    private var index = 0
    override fun hasNext() = index < this@Vec2.size
    override fun next() = this@Vec2[index++]
  }

  operator fun get(index: Int): T =
      when (index) {
        0 -> x
        1 -> y
        else -> throw InvalidParameterException("Index into a Vec2 must be 0 or 1, got $index")
      }

  val magnitude: Double
    get() = sqrt(x.toDouble().pow(2) + y.toDouble().pow(2))
}

interface VectorOperations<T> where T : Comparable<T>, T : Number {
  val x: T
  val y: T
  val magnitude: Double

  operator fun plus(vec: Vec2<T>): Vec2<T>
  operator fun minus(vec: Vec2<T>): Vec2<T>
  operator fun times(vec: Vec2<T>): Vec2<T>
  operator fun div(vec: Vec2<T>): Vec2<T>

  operator fun plus(value: T) = plus(Vec2(value, value))
  operator fun minus(value: T) = minus(Vec2(value, value))
  operator fun times(value: T) = times(Vec2(value, value))
  operator fun div(value: T) = div(Vec2(value, value))

  fun dot(vec: Vec2<T>): T
  fun angleBetween(vec: Vec2<T>): Double
}

interface VectorOperationsInt : VectorOperations<Int> {
  override fun plus(vec: Vec2<Int>) = Vec2Int(x + vec.x, y + vec.y)
  override fun minus(vec: Vec2<Int>) = Vec2Int(x - vec.x, y - vec.y)
  override fun times(vec: Vec2<Int>) = Vec2Int(x * vec.x, y * vec.y)
  override fun div(vec: Vec2<Int>) = Vec2Int(x / vec.x, y / vec.y)

  override fun dot(vec: Vec2<Int>) = x * vec.x + y * vec.y
  override fun angleBetween(vec: Vec2<Int>) = acos(dot(vec) / (magnitude * vec.magnitude))
}

interface VectorOperationsDouble : VectorOperations<Double> {
  override fun plus(vec: Vec2<Double>) = Vec2Double(x + vec.x, y + vec.y)
  override fun minus(vec: Vec2<Double>) = Vec2Double(x - vec.x, y - vec.y)
  override fun times(vec: Vec2<Double>) = Vec2Double(x * vec.x, y * vec.y)
  override fun div(vec: Vec2<Double>) = Vec2Double(x / vec.x, y / vec.y)

  override fun dot(vec: Vec2<Double>) = x * vec.x + y * vec.y
  override fun angleBetween(vec: Vec2<Double>) = acos(dot(vec) / (magnitude * vec.magnitude))
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

open class MutableVec2<T>(inputs: Iterable<T>) : Vec2<T>(inputs) where T : Comparable<T>, T : Number {
  override var x = super.point.first
  override var y = super.point.second

  operator fun set(index: Int, value: T) =
      when (index) {
        0 -> x = value
        1 -> y = value
        else -> throw InvalidParameterException("Index into a MutableVec2 must be 0 or 1, got $index")
      }

  fun clamp(xMin: T, xMax: T, yMin: T, yMax: T): MutableVec2<T> {
    x = minOf(maxOf(x, xMin), xMax)
    y = minOf(maxOf(y, yMin), yMax)
    return this  // Allow daisy-chaining
  }
}

class MutableVec2Int(inputs: Iterable<Int>) : MutableVec2<Int>(inputs), VectorOperationsInt {
  constructor(x: Int, y: Int) : this(listOf(x, y))
  constructor(p: Pair<Int, Int>) : this(p.first, p.second)
  constructor(vec2: Vec2<Int>) : this(vec2.x, vec2.y)
}

class MutableVec2Double(inputs: Iterable<Double>) : MutableVec2<Double>(inputs), VectorOperationsDouble {
  constructor(x: Double, y: Double) : this(listOf(x, y))
  constructor(p: Pair<Double, Double>) : this(p.first, p.second)
  constructor(vec2: Vec2<Double>) : this(vec2.x, vec2.y)
}

fun main() {
  println(Vec2Int(3, 4) + Vec2(3, 4))

  val a = MutableVec2Int(3, 4)
  a.x = 7
  println(a)

  val (x, y) = a
  println("$x, $y")
}

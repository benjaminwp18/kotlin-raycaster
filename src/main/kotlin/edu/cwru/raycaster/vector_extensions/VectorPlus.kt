package edu.cwru.raycaster.vector_extensions

import edu.cwru.raycaster.Vec2

// Addition

// Byte
@JvmName("VecBytePlusVec2Byte")
operator fun Vec2<Byte>.plus(vec2: Vec2<Byte>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecBytePlusVec2Double")
operator fun Vec2<Byte>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecBytePlusVec2Float")
operator fun Vec2<Byte>.plus(vec2: Vec2<Float>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecBytePlusVec2Int")
operator fun Vec2<Byte>.plus(vec2: Vec2<Int>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecBytePlusVec2Long")
operator fun Vec2<Byte>.plus(vec2: Vec2<Long>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecBytePlusVec2Short")
operator fun Vec2<Byte>.plus(vec2: Vec2<Short>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

// Double
@JvmName("VecDoublePlusVec2Byte")
operator fun Vec2<Double>.plus(vec2: Vec2<Byte>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecDoublePlusVec2Double")
operator fun Vec2<Double>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecDoublePlusVec2Float")
operator fun Vec2<Double>.plus(vec2: Vec2<Float>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecDoublePlusVec2Int")
operator fun Vec2<Double>.plus(vec2: Vec2<Int>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecDoublePlusVec2Long")
operator fun Vec2<Double>.plus(vec2: Vec2<Long>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecDoublePlusVec2Zhort")
operator fun Vec2<Double>.plus(vec2: Vec2<Short>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

// Float
@JvmName("VecFloatPlusVec2Byte")
operator fun Vec2<Float>.plus(vec2: Vec2<Byte>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecFloatPlusVec2Double")
operator fun Vec2<Float>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecFloatPlusVec2Float")
operator fun Vec2<Float>.plus(vec2: Vec2<Float>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecFloatPlusVec2Int")
operator fun Vec2<Float>.plus(vec2: Vec2<Int>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecFloatPlusVec2Long")
operator fun Vec2<Float>.plus(vec2: Vec2<Long>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("VecFloatPlusVec2Short")
operator fun Vec2<Float>.plus(vec2: Vec2<Short>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

// Int

@JvmName("Vec2IntPlusVec2Byte")
operator fun Vec2<Int>.plus(vec2: Vec2<Byte>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2IntPlusVec2Double")
operator fun Vec2<Int>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2IntPlusVec2Float")
operator fun Vec2<Int>.plus(vec2: Vec2<Float>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2IntPlusVec2Int")
operator fun Vec2<Int>.plus(vec2: Vec2<Int>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2IntPlusVec2Long")
operator fun Vec2<Int>.plus(vec2: Vec2<Long>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2IntPlusVec2Short")
operator fun Vec2<Int>.plus(vec2: Vec2<Short>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

// Long
@JvmName("Vec2LongPlusVec2Byte")
operator fun Vec2<Long>.plus(vec2: Vec2<Byte>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2LongPlusVec2Double")
operator fun Vec2<Long>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2LongPlusVec2Float")
operator fun Vec2<Long>.plus(vec2: Vec2<Float>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2LongPlusVec2Int")
operator fun Vec2<Long>.plus(vec2: Vec2<Int>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2LongPlusVec2Long")
operator fun Vec2<Long>.plus(vec2: Vec2<Long>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2LongPlusVec2Short")
operator fun Vec2<Long>.plus(vec2: Vec2<Short>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

// Short
@JvmName("Vec2ShortPlusVec2Byte")
operator fun Vec2<Short>.plus(vec2: Vec2<Byte>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2ShortPlusVec2Double")
operator fun Vec2<Short>.plus(vec2: Vec2<Double>): Vec2<Double> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2ShortPlusVec2Float")
operator fun Vec2<Short>.plus(vec2: Vec2<Float>): Vec2<Float> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2ShortPlusVec2Int")
operator fun Vec2<Short>.plus(vec2: Vec2<Int>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2ShortPlusVec2Long")
operator fun Vec2<Short>.plus(vec2: Vec2<Long>): Vec2<Long> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

@JvmName("Vec2ShortPlusVec2Short")
operator fun Vec2<Short>.plus(vec2: Vec2<Short>): Vec2<Int> {
  return Vec2(this.x + vec2.x, this.y + vec2.y)
}

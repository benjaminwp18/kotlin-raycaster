package edu.cwru.raycaster

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.nio.IntBuffer
import kotlin.math.absoluteValue

const val NS_IN_S = 1_000_000_000.0

const val PX_PER_BLOCK = 50
const val BLOCKS_PER_PX = 1.0 / PX_PER_BLOCK.toDouble()

const val PLAYER_MOVE_RATE = 3.0  // blocks / sec
const val PLAYER_RADIUS_BLOCKS = 0.25
const val PLAYER_RADIUS_PX = (PLAYER_RADIUS_BLOCKS * PX_PER_BLOCK).toInt()

val KEY_VECTORS = mapOf(
    KeyCode.W     to Vec2Double(0.0, -PLAYER_MOVE_RATE),
    KeyCode.UP    to Vec2Double(0.0, -PLAYER_MOVE_RATE),
    KeyCode.S     to Vec2Double(0.0,  PLAYER_MOVE_RATE),
    KeyCode.DOWN  to Vec2Double(0.0,  PLAYER_MOVE_RATE),
    KeyCode.A     to Vec2Double(-PLAYER_MOVE_RATE, 0.0),
    KeyCode.LEFT  to Vec2Double(-PLAYER_MOVE_RATE, 0.0),
    KeyCode.D     to Vec2Double( PLAYER_MOVE_RATE, 0.0),
    KeyCode.RIGHT to Vec2Double( PLAYER_MOVE_RATE, 0.0),
).withDefault { Vec2Double(0.0, 0.0) }

data class Block(val color: Color, val passable: Boolean) {
    companion object {
        private val CHAR_TO_BLOCK = mapOf(
            ' ' to Block(Color.WHITE, true),
            '#' to Block(Color.BLUE, false)
        ).withDefault { Block(Color.PURPLE, false) }

        fun fromChar(c: Char) = CHAR_TO_BLOCK.getValue(c)
    }
}

fun stringToBlockMap(str: String): Array<Array<Block>> {
    val lines = str.split('\n')
    return Array(lines.size) { y ->
        Array(lines[y].length) { x ->
            Block.fromChar(lines[y][x])
        }
    }
}

private val MAP = stringToBlockMap("""
    ######
    #    #
    #  # #
    #    #
    ######
""".trimIndent())

val MAP_WIDTH_BLOCKS = MAP[0].size
val MAP_HEIGHT_BLOCKS = MAP.size
val MAP_WIDTH_PX = MAP_WIDTH_BLOCKS * PX_PER_BLOCK
val MAP_HEIGHT_PX = MAP_HEIGHT_BLOCKS * PX_PER_BLOCK

const val FPV_WIDTH_PX = 800
const val FPV_HEIGHT_PX = 400

fun Double.format(scale: Int) = "%.${scale}f".format(this)

class Player() {
    var position = MutableVec2Double(2.0, 2.0)
    var direction = MutableVec2Double(-1.0, 0.0)
    var camPlane = MutableVec2Double(0.0, 0.66)
}

enum class WallType {
    NorthSouth, EastWest
}

class Raycaster : Application() {
    private val keyMap: MutableMap<KeyCode, Boolean> = KEY_VECTORS.keys.associateWith { false }
        .toMutableMap().withDefault { false }
    private var prevFrameTime = 0L
    private val player = Player()

    override fun start(primaryStage: Stage) {
        val frameRateLabel = Label("No FPS data")

        primaryStage.title = "Kotlin Raycaster"

        val topDownView = ImageCanvas(MAP_WIDTH_PX, MAP_HEIGHT_PX)
        val firstPersonView = ImageCanvas(FPV_WIDTH_PX, FPV_HEIGHT_PX)

        val root = VBox()
        root.children.add(frameRateLabel)

        val viewBox = HBox()
        root.children.add(viewBox)
        viewBox.children.add(topDownView)
        viewBox.children.add(firstPersonView)

        primaryStage.scene = Scene(root, MAP_WIDTH_PX + FPV_WIDTH_PX + 10.0, FPV_HEIGHT_PX + 50.0)

        primaryStage.scene.setOnKeyPressed {
            if (it.code in keyMap.keys) {
                keyMap[it.code] = true
            }
        }
        primaryStage.scene.setOnKeyReleased {
            if (it.code in keyMap.keys) {
                keyMap[it.code] = false
            }
        }

        primaryStage.show()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                // Time delta from previous frame in seconds; multiply rates by this to deal with variable FPS
                val deltaSec = (now - prevFrameTime) / NS_IN_S
                // Instantaneous frame rate
                val frameRate = 1.0 / deltaSec
                frameRateLabel.text = "FPS: ${frameRate.format(2)}\nδS: ${deltaSec.format(2)}"
                prevFrameTime = now

                // Don't walk through walls
                for ((key, pressed) in keyMap) {
                    if (pressed) {
                        val newPos = player.position + KEY_VECTORS.getValue(key) * deltaSec
                        val topLeft = newPos - PLAYER_RADIUS_BLOCKS
                        val bottomRight = newPos + PLAYER_RADIUS_BLOCKS

                        // TODO: Snap to wall if you would clip it to avoid gaps
                        if (MAP[topLeft.y.toInt()][topLeft.x.toInt()].passable and
                            MAP[topLeft.y.toInt()][bottomRight.x.toInt()].passable and
                            MAP[bottomRight.y.toInt()][topLeft.x.toInt()].passable and
                            MAP[bottomRight.y.toInt()][bottomRight.x.toInt()].passable) {
                            player.position = MutableVec2Double(newPos)
                        }
                    }
                }

                // Don't leave the map
                player.position.clamp(
                    PLAYER_RADIUS_BLOCKS, MAP_WIDTH_BLOCKS - PLAYER_RADIUS_BLOCKS,
                    PLAYER_RADIUS_BLOCKS, MAP_HEIGHT_BLOCKS - PLAYER_RADIUS_BLOCKS
                )

                for ((y, row) in MAP.withIndex()) {
                    for ((x, block) in row.withIndex()) {
                        topDownView.prepRect(
                            x * PX_PER_BLOCK, y * PX_PER_BLOCK,
                            PX_PER_BLOCK, PX_PER_BLOCK,
                            block.color
                        )
                    }
                }

                topDownView.prepRect(
                    (player.position.x * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    (player.position.y * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    PLAYER_RADIUS_PX * 2, PLAYER_RADIUS_PX * 2,
                    Color.RED
                )

                topDownView.redraw()

                firstPersonView.prepRect(
                    0, 0,
                    FPV_WIDTH_PX, FPV_HEIGHT_PX,
                    Color.PURPLE
                )

                firstPersonView.redraw()

                for (screenX in 0 until FPV_WIDTH_PX) {
                    val cameraX = 2 * screenX.toDouble() / FPV_WIDTH_PX - 1
                    val rayDir = (player.direction + player.camPlane) * cameraX

                    val rayMapPos = MutableVec2Int(player.position.toVec2Int())
                    val sideDist = MutableVec2Double(0.0, 0.0)
                    val deltaDist = (1.0 / rayDir).absoluteValue
                    val step = MutableVec2Int(0, 0)
                    var hitWall = false
                    var hitSide = WallType.EastWest

                    if (rayDir.x < 0) {
                        step.x = -1
                        sideDist.x = (player.position.x - rayMapPos.x) * deltaDist.x
                    }
                    else {
                        step.x = 1
                        sideDist.x = (-player.position.x + rayMapPos.x + 1.0) * deltaDist.x
                    }
                    if (rayDir.y < 0) {
                        step.y = -1
                        sideDist.y = (player.position.y - rayMapPos.y) * deltaDist.y
                    }
                    else {
                        step.y = 1
                        sideDist.y = (-player.position.y + rayMapPos.y + 1.0) * deltaDist.y
                    }

                    while (!hitWall) {
                        if (sideDist.x < sideDist.y) {
                            sideDist.x += deltaDist.x
                            rayMapPos.x += step.x
                            hitSide = WallType.EastWest
                        }
                        else {
                            sideDist.y += deltaDist.y
                            rayMapPos.y += step.y
                            hitSide = WallType.NorthSouth
                        }

                        if (!MAP[rayMapPos.y][rayMapPos.x].passable) {
                            hitWall = true
                        }
                    }

                    val dirIndicator = Vec2Int((player.direction.x * 20.0).toInt(), 5)
                    topDownView.prepRect(player.position.x.toInt() * PX_PER_BLOCK, player.position.y.toInt() * PX_PER_BLOCK, 10, 10, Color.PURPLE)
                    topDownView.prepRect((player.position.x * PX_PER_BLOCK).toInt() - 2, (player.position.y * PX_PER_BLOCK).toInt() - 2, dirIndicator.x, dirIndicator.y, Color.PURPLE)
                    topDownView.prepRect(rayMapPos.x * PX_PER_BLOCK, rayMapPos.y * PX_PER_BLOCK, 10, 10, Color.GREEN)

                    val perpWallDist =
                        if (hitSide == WallType.EastWest) sideDist.x - deltaDist.x
                        else sideDist.y - deltaDist.y
//                    println(perpWallDist)
                    val lineHeight = (FPV_HEIGHT_PX / perpWallDist).toInt()
                    val drawStart = maxOf(FPV_HEIGHT_PX / 2 - lineHeight / 2, 0)
                    val drawEnd = minOf(FPV_HEIGHT_PX / 2 + lineHeight / 2, FPV_HEIGHT_PX - 1)

                    val color = MAP[rayMapPos.y][rayMapPos.x].color

                    if (lineHeight + drawStart >= FPV_HEIGHT_PX) {
                        println("$drawStart -> $lineHeight")
                    }

                    // TODO: Different brightness for NS vs. EW sides
//                    if (hitSide == WallType.NorthSouth) {
//                        color /= 2
//                    }

//                    firstPersonView.prepRect(screenX, drawStart, 1, lineHeight, color)
                }

                firstPersonView.redraw()
            }
        }.start()
    }
}

// TODO: replace with extension function on JavaFX Color? Maybe not cause it stores everything as floats?
data class Color(val r: Int, val g: Int, val b: Int, val a: Int) {
    fun toInt() = (a shl 24) or (r shl 16) or (g shl 8) or b

    companion object {
        fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b, 255)
        fun rgba(r: Int, g: Int, b: Int, a: Int) = Color(r, g, b, a)

        val RED = rgb(255, 0, 0)
        val GREEN = rgb(0, 255, 0)
        val BLUE = rgb(0, 0, 255)
        val PURPLE = rgb(255, 0, 255)
        val AQUA = rgb(0, 255, 255)
        val YELLOW = rgb(255, 255, 0)
        val BLACK = rgb(0, 0, 0)
        val WHITE = rgb(255, 255, 255)
    }
}

class ImageCanvas private constructor(
    private val pixelBuffer: PixelBuffer<IntBuffer>
) : ImageView(WritableImage(pixelBuffer)) {
    // https://foojay.io/today/high-performance-rendering-in-javafx/

    private val pixels: IntArray = pixelBuffer.buffer.array()
    private val width: Int = pixelBuffer.width
    private val height: Int = pixelBuffer.height

    private fun coordsToIndex(x: Int, y: Int) = (x % width) + (y * width)
    private fun indexToCoords(i: Int) = Vec2Int(i % width, i / width)

    fun prepPixel(x: Int, y: Int, color: Color) {
        pixels[coordsToIndex(x, y)] = color.toInt()
    }

    fun prepRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        val topLeft = MutableVec2Int(x, y)
        val bottomRight = MutableVec2Int(x + w, y + h)

        // Negative width/height -> invert
        if (w < 0) {
            topLeft.x = x + w
            bottomRight.x = x
        }
        if (h < 0) {
            topLeft.y = y + h
            bottomRight.y = y
        }

        for (r in topLeft.y until bottomRight.y) {
            for (c in topLeft.x until bottomRight.x) {
                prepPixel(c, r, color)
            }
        }
    }

    fun prepLine(x1: Int, y1: Int, x2: Int, y2: Int) {

    }

    fun redraw() {
        // Tell the buffer that the entire area needs redrawing
        // TODO: keep track of areas that have been "prepped" and only redraw those?
        pixelBuffer.updateBuffer { b: PixelBuffer<IntBuffer>? -> null }
    }

    companion object {
        // Use companion invoke to construct buffer before calling super()
        // TODO: make less janky
        operator fun invoke(width: Int, height: Int): ImageCanvas {
            val buffer = IntBuffer.allocate(width * height)
            val pixelBuffer = PixelBuffer(width, height, buffer, PixelFormat.getIntArgbPreInstance())
            return ImageCanvas(pixelBuffer)
        }
    }
}

fun main() {
    Application.launch(Raycaster::class.java)
}
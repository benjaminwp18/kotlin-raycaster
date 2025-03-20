package edu.cwru.raycaster

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.nio.IntBuffer

val KEYS = listOf('w', 'a', 's', 'd', 'q', 'e')

const val NS_IN_S = 1_000_000_000.0

const val PX_PER_BLOCK = 50

const val PLAYER_MOVE_RATE = 3.0  // blocks / sec
const val PLAYER_RADIUS_BLOCKS = 0.5
const val PLAYER_RADIUS_PX = (PLAYER_RADIUS_BLOCKS * PX_PER_BLOCK).toInt()

val BLOCK_COLORS = mapOf(
    0 to Color.WHITE,
    1 to Color.BLACK,
).withDefault { Color.PURPLE }  // Noticeable error color

private val MAP = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1)
)

val MAP_WIDTH_BLOCKS = MAP[0].size
val MAP_HEIGHT_BLOCKS = MAP.size
val MAP_WIDTH_PX = MAP_WIDTH_BLOCKS * PX_PER_BLOCK
val MAP_HEIGHT_PX = MAP_HEIGHT_BLOCKS * PX_PER_BLOCK

fun Double.format(scale: Int) = "%.${scale}f".format(this)


class Raycaster : Application() {
    private val keyMap: MutableMap<Char, Boolean> = KEYS.associateWith { false }.toMutableMap().withDefault { false }
    private var prevFrameTime = 0L
    private val playerPosition = MutableVec2(1.0, 1.0)

    override fun start(primaryStage: Stage) {
        val frameRateLabel = Label("No FPS data")

        primaryStage.title = "Kotlin Raycaster"

        val topDownView = ImageCanvas(MAP_WIDTH_PX, MAP_HEIGHT_PX)

        val root = VBox()
        root.children.add(frameRateLabel)
        root.children.add(topDownView)

        primaryStage.scene = Scene(root, 500.0, 500.0)

        primaryStage.scene.setOnKeyPressed {
            if (it.text.length == 1) {
                val keyChar = it.text[0]
                if (keyChar in keyMap.keys) {
                    keyMap[keyChar] = true
                }
            }
        }
        primaryStage.scene.setOnKeyReleased {
            if (it.text.length == 1) {
                val keyChar = it.text[0]
                if (keyChar in keyMap.keys) {
                    keyMap[keyChar] = false
                }
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

                if (keyMap.getValue('w')) {
                    playerPosition.y -= PLAYER_MOVE_RATE * deltaSec
                }
                if (keyMap.getValue('s')) {
                    playerPosition.y += PLAYER_MOVE_RATE * deltaSec
                }
                if (keyMap.getValue('d')) {
                    playerPosition.x += PLAYER_MOVE_RATE * deltaSec
                }
                if (keyMap.getValue('a')) {
                    playerPosition.x -= PLAYER_MOVE_RATE * deltaSec
                }

                // Don't leave the map
                playerPosition.clamp(
                    PLAYER_RADIUS_BLOCKS, MAP_WIDTH_BLOCKS - PLAYER_RADIUS_BLOCKS,
                    PLAYER_RADIUS_BLOCKS, MAP_HEIGHT_BLOCKS - PLAYER_RADIUS_BLOCKS
                )

                for ((r, row) in MAP.withIndex()) {
                    for ((c, block) in row.withIndex()) {
                        topDownView.prepRect(
                            r * PX_PER_BLOCK, c * PX_PER_BLOCK,
                            PX_PER_BLOCK, PX_PER_BLOCK,
                            BLOCK_COLORS.getValue(block)
                        )
                    }
                }

                topDownView.prepRect(
                    (playerPosition.x * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    (playerPosition.y * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    PLAYER_RADIUS_PX * 2, PLAYER_RADIUS_PX * 2,
                    Color.RED
                )

                topDownView.redraw()
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

        val RED = Color.rgb(255, 0, 0)
        val GREEN = Color.rgb(0, 255, 0)
        val BLUE = Color.rgb(0, 0, 255)
        val PURPLE = Color.rgb(255, 0, 255)
        val AQUA = Color.rgb(0, 255, 255)
        val YELLOW = Color.rgb(255, 255, 0)
        val BLACK = Color.rgb(0, 0, 0)
        val WHITE = Color.rgb(255, 255, 255)
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
    private fun indexToCoords(i: Int) = Vec2(i % width, i / width)

    fun prepPixel(x: Int, y: Int, color: Color) {
        pixels[coordsToIndex(x, y)] = color.toInt()
    }

    fun prepRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
        for (r in y until y + h) {
            for (c in x until x + w) {
                prepPixel(c, r, color)
            }
        }
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

fun main(args: Array<String>) {
    Application.launch(Raycaster::class.java)
}
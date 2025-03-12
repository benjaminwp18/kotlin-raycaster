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
const val NS_TO_S = 1_000_000_000.0
const val SIXTY_FPS_DELTA_NS = NS_TO_S / 60.0

fun Double.format(scale: Int) = "%.${scale}f".format(this)

class Raycaster : Application() {
    private val keyMap: MutableMap<Char, Boolean> = KEYS.associateWith { false }.toMutableMap().withDefault { false }
    private var prevFrameTime = 0L

    override fun start(primaryStage: Stage) {
        val frameRateLabel = Label("??.??")

        primaryStage.title = "Kotlin Raycaster"

        val imageView = ImageCanvas(100, 100)

        val root = VBox()
        root.children.add(frameRateLabel)
        root.children.add(imageView)

        primaryStage.scene = Scene(root, 300.0, 250.0)

        primaryStage.scene.setOnKeyPressed {
            println("Pressed '${it.text}'")
            val keyChar = it.text[0]
            if (keyChar in keyMap.keys) {
                keyMap[keyChar] = true
            }
            println(keyMap)
        }
        primaryStage.scene.setOnKeyReleased {
            println("Released '${it.text}'")
            val keyChar = it.text[0]
            if (keyChar in keyMap.keys) {
                keyMap[keyChar] = false
            }
            println(keyMap)
        }

        primaryStage.show()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                val deltaNanoS = now - prevFrameTime
                // Instantaneous frame rate
                val frameRate = 1_000_000_000.0 / deltaNanoS
                // Delta relative to 60 FPS; multiply rates by relativeDelta to account for FPS fluctuations
                val relativeDelta = deltaNanoS / SIXTY_FPS_DELTA_NS
                frameRateLabel.text = "FPS: ${frameRate.format(2)}\nRδ:  ${relativeDelta.format(2)}"
                prevFrameTime = now

                imageView.prepRect(10, 20, 30, 30, Color(10, 10, (0..255).random(), 255))
                imageView.redraw()
            }
        }.start()
    }
}

// TODO: replace with extension function on JavaFX Color?
data class Color(val r: Int, val g: Int, val b: Int, val a: Int) {
    fun toInt() = (a shl 24) or (r shl 16) or (g shl 8) or b
}

data class ArrayCoords(val x: Int, val y: Int)

class ImageCanvas private constructor(
    private val pixelBuffer: PixelBuffer<IntBuffer>
) : ImageView(WritableImage(pixelBuffer)) {
    // https://foojay.io/today/high-performance-rendering-in-javafx/

    private val pixels: IntArray = pixelBuffer.buffer.array()
    private val width: Int = pixelBuffer.width
    private val height: Int = pixelBuffer.height

    private fun coordsToIndex(x: Int, y: Int) = (x % width) + (y * width)
    private fun indexToCoords(i: Int) = ArrayCoords(i % width, i / width)

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
        // tell the buffer that the entire area needs redrawing
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
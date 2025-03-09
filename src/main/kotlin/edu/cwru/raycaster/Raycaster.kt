package edu.cwru.raycaster

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import java.nio.IntBuffer

class Raycaster : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "Hello World!"
        val btn: Button = Button()
        btn.text = "Say 'Hello World'"
        btn.onAction = EventHandler<ActionEvent?> {
            println("Hello World!")
        }

        val imageView = ImageCanvas(50, 100)

        val root = StackPane()
        root.children.add(btn)
        root.children.add(imageView)
        primaryStage.scene = Scene(root, 300.0, 250.0)
        primaryStage.show()

        object : AnimationTimer() {
            override fun handle(now: Long) {
                imageView.redraw()
            }
        }.start()
    }
}

class ImageCanvas private constructor(
    private val pixelBuffer: PixelBuffer<IntBuffer>
) : ImageView(WritableImage(pixelBuffer)) {
    // https://foojay.io/today/high-performance-rendering-in-javafx/

    private val pixels: IntArray = pixelBuffer.buffer.array()
    private val width: Int = pixelBuffer.width
    private val height: Int = pixelBuffer.height

    fun redraw() {
        val alpha = 255
        val red = (0..255).random()
        val green = 0
        val blue = 0
        val colorARGB = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
        for ((i, _) in pixels.withIndex()) {
            // (x % IMAGE_WIDTH) + (y * IMAGE_WIDTH)
            pixels[i] = colorARGB
        }

        // tell the buffer that the entire area needs redrawing
        pixelBuffer.updateBuffer { b: PixelBuffer<IntBuffer>? -> null }
    }

    companion object {
        // Use companion invoke to construct buffer before calling super()
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
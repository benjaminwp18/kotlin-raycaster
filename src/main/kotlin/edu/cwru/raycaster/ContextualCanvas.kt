package edu.cwru.raycaster

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import java.nio.ByteBuffer

class ContextualCanvas(private val width: Int, private val height: Int): Canvas(width.toDouble(), height.toDouble()) {
    val context: GraphicsContext = graphicsContext2D
    var fill: Paint
        get() = context.fill
        set(color) {
            context.fill = color
        }
    var stroke: Paint
        get() = context.stroke
        set(color) {
            context.stroke = color
        }

    fun drawImage(image: Image, x: Number, y: Number) {
        context.drawImage(image, x.toDouble(), y.toDouble())
    }

    fun writePixel(x: Int, y: Int, color: Color) {
        context.pixelWriter.setColor(x, y, color)
    }

    fun writePixels(x: Int, y: Int, w: Int, h: Int, pixelFormat: PixelFormat<ByteBuffer>, buffer: ByteArray, offset: Int, scanlineStride: Int) {
        context.pixelWriter.setPixels(x, y, w, h, pixelFormat, buffer, offset, scanlineStride)
    }

    fun fillRect(x: Number, y: Number, w: Number, h: Number, color: Color? = null) {
        val oldFill = fill
        if (color != null) {
            fill = color
        }
        context.fillRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
        fill = oldFill
    }

    fun strokeLine(x1: Number, y1: Number, x2: Number, y2: Number, color: Color? = null) {
        val oldStroke = stroke
        if (color != null) {
            stroke = color
        }
        context.strokeLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble())
        stroke = oldStroke
    }
    fun strokeLine(p1: Vec2Double, p2: Vec2Double, color: Color? = null) =
        strokeLine(p1.x, p1.y, p2.x, p2.y, color)
    fun strokeLine(p1: Vec2Int, p2: Vec2Int, color: Color? = null) =
        strokeLine(p1.x, p1.y, p2.x, p2.y, color)
}
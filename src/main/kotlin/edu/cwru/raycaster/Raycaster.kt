package edu.cwru.raycaster

import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Stage
import java.io.FileInputStream


const val NS_IN_S = 1_000_000_000.0

const val PX_PER_BLOCK = 64
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

const val TEXTURE_PATH = "src/main/resources/edu/cwru/raycaster/textures/"


enum class Texture(fileName: String){
    EAGLE("eagle.png"),
    RED_BRICK("red_brick.png"),
    PURPLE_STONE("purple_stone.png"),
    GREY_STONE("grey_stone.png"),
    BLUE_BRICK("blue_brick.png"),
    MOSSY("mossy.png"),
    WOOD("wood.png"),
    COLOR_STONE("color_stone.png");

    private val inputStream = FileInputStream(TEXTURE_PATH + fileName)
    val image = Image(inputStream)
}

data class Block(val color: Color,
                 val texture: Texture,
                 val passable: Boolean = false) {

    companion object {
        private val CHAR_TO_BLOCK = mapOf(
            ' ' to Block(Color.WHITE, Texture.MOSSY, true),
            'B' to Block(Color.BLUE, Texture.BLUE_BRICK),
            'G' to Block(Color.GREEN, Texture.WOOD),
            'O' to Block(Color.ORANGE, Texture.EAGLE),
        ).withDefault { Block(Color.PURPLE, Texture.PURPLE_STONE) }

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
    BBBBBB
    G    B
    B  B B
    O    B
    BBBBBB
""".trimIndent())

val SKY_COLOR: Color = Color.LIGHTBLUE
val FLOOR_COLOR: Color = Color.LIGHTGRAY

val MAP_WIDTH_BLOCKS = MAP[0].size
val MAP_HEIGHT_BLOCKS = MAP.size
val MAP_WIDTH_PX = MAP_WIDTH_BLOCKS * PX_PER_BLOCK
val MAP_HEIGHT_PX = MAP_HEIGHT_BLOCKS * PX_PER_BLOCK

const val FPV_WIDTH_PX = 800
const val FPV_HEIGHT_PX = 400

fun Double.format(scale: Int) = "%.${scale}f".format(this)

class Player {
    var position = MutableVec2Double(2.0, 2.0)
    var direction = MutableVec2Double(-1.0, 0.0)
    var camPlane = MutableVec2Double(0.0, 1.0)
}

enum class WallType {
    NorthSouth, EastWest
}

const val USE_TEXTURES = true

class Raycaster : Application() {
    private val keyMap: MutableMap<KeyCode, Boolean> = KEY_VECTORS.keys.associateWith { false }
        .toMutableMap().withDefault { false }
    private var prevFrameTime = 0L
    private val player = Player()

    override fun start(primaryStage: Stage) {
        val frameRateLabel = Label("No FPS data")

        primaryStage.title = "Kotlin Raycaster"

        val topDownCanvas = ContextualCanvas(MAP_WIDTH_PX, MAP_HEIGHT_PX)
        val firstPersonCanvas = ContextualCanvas(FPV_WIDTH_PX, FPV_HEIGHT_PX)

        val root = VBox()
        root.children.add(frameRateLabel)

        val viewBox = HBox()
        root.children.add(viewBox)
        viewBox.children.add(topDownCanvas)
        viewBox.children.add(firstPersonCanvas)

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
                        val xLocation = (x * PX_PER_BLOCK).toDouble()
                        val yLocation = (y * PX_PER_BLOCK).toDouble()
                        if (USE_TEXTURES){
                            topDownCanvas.drawImage(block.texture.image, xLocation, yLocation)
                        }
                        else {
                            topDownCanvas.fillRect(
                                xLocation, yLocation,
                                PX_PER_BLOCK, PX_PER_BLOCK,
                                block.color
                            )
                        }
                    }
                }

                // Draws Player
                topDownCanvas.fillRect(
                    (player.position.x * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    (player.position.y * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
                    PLAYER_RADIUS_PX * 2, PLAYER_RADIUS_PX * 2,
                    Color.RED
                )

                // Draws Sky and Floor
                // TODO: Not sure how to make this work with textures
                firstPersonCanvas.fillRect(0, 0, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, SKY_COLOR)
                firstPersonCanvas.fillRect(0, FPV_HEIGHT_PX / 2, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, FLOOR_COLOR)

                for (screenX in 0 until FPV_WIDTH_PX) {
                    val cameraX = 2 * screenX.toDouble() / FPV_WIDTH_PX - 1

                    // TODO: This is probably be "+ player.camPlane". Fix when real movement is written.
                    val rayDir = player.direction - player.camPlane * cameraX

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

                        // Assumes only impassable tiles are walls
                        // Will cause errors if map is not surrounded by walls
                        if (!MAP[rayMapPos.y][rayMapPos.x].passable) {
                            hitWall = true
                        }
                    }

                    val perpWallDist =
                        if (hitSide == WallType.EastWest) sideDist.x - deltaDist.x
                        else sideDist.y - deltaDist.y
                    val lineHeight = (FPV_HEIGHT_PX / perpWallDist).toInt()
                    val drawStart = maxOf(FPV_HEIGHT_PX / 2 - lineHeight / 2, 0)

                    var color = MAP[rayMapPos.y][rayMapPos.x].color

                    if (hitSide == WallType.NorthSouth) {
                        color = color.darker()
                    }

                    firstPersonCanvas.fillRect(screenX, drawStart, 1, lineHeight, color)

                    // Tag the block the player is in
                    val playerBlock = player.position.toVec2Int()
                    topDownCanvas.fillRect(playerBlock.x * PX_PER_BLOCK, playerBlock.y * PX_PER_BLOCK, 10, 10, Color.PURPLE)

                    // Tag the blocks the rays hit
                    topDownCanvas.fillRect(rayMapPos.x * PX_PER_BLOCK, rayMapPos.y * PX_PER_BLOCK, 10, 10, Color.GREEN)

                    // Draw lines for rays
                    val playerPosPx = (player.position * PX_PER_BLOCK.toDouble()).toVec2Int()
                    val rayDirPx = playerPosPx + (rayDir * perpWallDist * PX_PER_BLOCK.toDouble()).toVec2Int()
                    topDownCanvas.strokeLine(
                        playerPosPx.x, playerPosPx.y,
                        rayDirPx.x, rayDirPx.y,
                        Color.rgb(255, 0, (255.0 * (screenX / FPV_WIDTH_PX.toDouble())).toInt())
                    )

                    // Draw line in the player's direction
                    val playerDirPx = playerPosPx + (player.direction * 20.0).toVec2Int()
                    topDownCanvas.strokeLine(playerPosPx.x, playerPosPx.y, playerDirPx.x, playerDirPx.y, Color.BLACK)

                    // Draw camera plane
                    val playerPlaneEndPx = playerDirPx + (player.camPlane * 20.0).toVec2Int()
                    val playerPlaneStartPx = playerDirPx - (player.camPlane * 20.0).toVec2Int()
                    topDownCanvas.strokeLine(playerPlaneStartPx.x, playerPlaneStartPx.y, playerPlaneEndPx.x, playerPlaneEndPx.y, Color.BLACK)
                }
            }
        }.start()
    }
}

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

    fun drawImage(image: Image, x: Double, y: Double) {
        context.drawImage(image, x, y)
    }

    fun fillRect(x: Double, y: Double, w: Double, h: Double, color: Color? = null) {
        val oldFill = fill
        if (color != null) {
            fill = color
        }
        context.fillRect(x, y, w, h)
        fill = oldFill
    }
    fun fillRect(x: Number, y: Number, w: Number, h: Number, color: Color? = null) =
        fillRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), color)

    fun strokeLine(x1: Double, y1: Double, x2: Double, y2: Double, color: Color? = null) {
        val oldStroke = stroke
        if (color != null) {
            stroke = color
        }
        context.strokeLine(x1, y1, x2, y2)
        stroke = oldStroke
    }
    fun strokeLine(x1: Number, y1: Number, x2: Number, y2: Number, color: Color? = null) =
        strokeLine(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), color)
    fun strokeLine(p1: Vec2Double, p2: Vec2Double, color: Color? = null) =
        strokeLine(p1.x, p1.y, p2.x, p2.y, color)
    fun strokeLine(p1: Vec2Int, p2: Vec2Int, color: Color? = null) =
        strokeLine(p1.x, p1.y, p2.x, p2.y, color)
}

fun main() {
    Application.launch(Raycaster::class.java)
}
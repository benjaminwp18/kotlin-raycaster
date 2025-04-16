package edu.cwru.raycaster

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.PixelFormat
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.floor

const val NS_IN_S = 1_000_000_000.0

const val TEXTURE_WIDTH = 64
const val TEXTURE_HEIGHT = 64

const val PX_PER_BLOCK = TEXTURE_WIDTH
const val BLOCKS_PER_PX = 1.0 / PX_PER_BLOCK.toDouble()

const val PLAYER_MOVE_RATE = 3.0  // blocks / sec
const val PLAYER_ANGULAR_VELOCITY = PI   // radians / sec
const val PLAYER_RADIUS_BLOCKS = 0.1
const val PLAYER_RADIUS_PX = (PLAYER_RADIUS_BLOCKS * PX_PER_BLOCK).toInt()

val STRAFE_KEY_ANGLES = mapOf(
    KeyCode.W     to 0.0,
    KeyCode.UP    to 0.0,
    KeyCode.S     to PI,
    KeyCode.DOWN  to PI,
    KeyCode.A     to -0.5 * PI,
    KeyCode.LEFT  to -0.5 * PI,
    KeyCode.D     to 0.5 * PI,
    KeyCode.RIGHT to 0.5 * PI,
)

val ROTATION_KEY_SIGNS = mapOf(
    KeyCode.E to 1.0,
    KeyCode.Q to -1.0,
)

// Must manually type to get correct nullability from Java Color class
val SKY_COLOR: Color = Color.LIGHTBLUE
val FLOOR_COLOR: Color = Color.LIGHTGRAY
val SKY_TEXTURE = Texture.WOOD
val FLOOR_TEXTURE = Texture.GREY_STONE

fun ByteArray.writePixel(x: Int, y: Int, color: Color) {
    val pxIndex = (y * FPV_WIDTH_PX + x) * FPV_BYTES_PER_PX
    this[pxIndex] = (color.blue * 255.0).toInt().toByte()
    this[pxIndex + 1] = (color.green * 255.0).toInt().toByte()
    this[pxIndex + 2] = (color.red * 255.0).toInt().toByte()
    this[pxIndex + 3] = (color.opacity * 255.0).toInt().toByte()
}

fun ByteArray.fillRect(x: Int, y: Int, w: Int, h: Int, color: Color) {
    for (xIdx in x until x + w) {
        for (yIdx in y until y + h) {
            writePixel(xIdx, yIdx, color)
        }
    }
}

data class Block(val color: Color,
                 val texture: Texture,
                 val passable: Boolean = false) {
    companion object {
        private val CHAR_TO_BLOCK = mapOf(
            ' ' to Block(Color.WHITE, FLOOR_TEXTURE, true),
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

val MAP_WIDTH_BLOCKS = MAP[0].size
val MAP_HEIGHT_BLOCKS = MAP.size
val MAP_WIDTH_PX = MAP_WIDTH_BLOCKS * PX_PER_BLOCK
val MAP_HEIGHT_PX = MAP_HEIGHT_BLOCKS * PX_PER_BLOCK

const val FPV_ASPECT_WIDTH_PX = 400
const val FPV_ASPECT_HEIGHT_PX = 200
const val FPV_BYTES_PER_PX = 4
const val FPV_SCALE = 2
const val FPV_WIDTH_PX = FPV_ASPECT_WIDTH_PX * FPV_SCALE
const val FPV_HEIGHT_PX = FPV_ASPECT_HEIGHT_PX * FPV_SCALE

fun Double.format(scale: Int) = "%.${scale}f".format(this)

class Player {
    var position = MutableVec2Double(2.0, 2.0)
    var direction = MutableVec2Double(-1.0, 0.0)
    var camPlane = MutableVec2Double(direction.rotate(PI / 2))
}

enum class WallType {
    NorthSouth, EastWest
}

const val USE_TEXTURES = true

class Raycaster : Application() {
    private val keyMap: MutableMap<KeyCode, Boolean> = (STRAFE_KEY_ANGLES.keys + ROTATION_KEY_SIGNS.keys).associateWith { false }
        .toMutableMap()

    private var prevFrameTime = 0L
    private val player = Player()
    private val topDownCanvas = ContextualCanvas(MAP_WIDTH_PX, MAP_HEIGHT_PX)
    private val firstPersonCanvas = ContextualCanvas(FPV_WIDTH_PX, FPV_HEIGHT_PX)
    private val frameRateLabel = Label("No FPS data")

    // TODO: Consider using a Buffer instead of an Array (and adding pixels horizontally) for performance
    private val FPVBuffer = ByteArray(FPV_WIDTH_PX * FPV_HEIGHT_PX * FPV_BYTES_PER_PX)
    private val FPVPixelFormat: PixelFormat<ByteBuffer> = PixelFormat.getByteBgraPreInstance()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Kotlin Raycaster"

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

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()

                // Time delta from previous frame in seconds; multiply rates by this to deal with variable FPS
                val deltaSec = (now - prevFrameTime) / 1000.0
                // Instantaneous frame rate
                val frameRate = 1.0 / deltaSec

                prevFrameTime = now

                withContext(Dispatchers.Main) {
                    frameRateLabel.text = "FPS: ${frameRate.format(2)}\nδS: ${deltaSec.format(2)}"
                }

                var time1 = System.currentTimeMillis()

                updateState(deltaSec)
                var time2 = System.currentTimeMillis()
                println("State update used: ${time2 - time1}")
                time1 = time2

                withContext(Dispatchers.Main) {
                    drawTopDownMap()
                }
                time2 = System.currentTimeMillis()
                println("Top down draw used: ${time2 - time1}")
                time1 = time2

                updateFPVBuffer()
                time2 = System.currentTimeMillis()
                println("FPV buffer update used: ${time2 - time1}")
                time1 = time2

                withContext(Dispatchers.Main) {
                    drawFPVBuffer()
                }
                time2 = System.currentTimeMillis()
                println("FPV buffer draw used: ${time2 - time1}")
                time1 = time2
            }
        }
    }

    private fun drawFPVBuffer() {
        firstPersonCanvas.writePixels(0, 0, FPV_WIDTH_PX, FPV_HEIGHT_PX, FPVPixelFormat, FPVBuffer,
            0, FPV_WIDTH_PX * FPV_BYTES_PER_PX)
    }

    private fun updateState(deltaSec: Double) {
        // Don't walk through walls
        for ((key, pressed) in keyMap) {
            if (pressed) {
                if (key in ROTATION_KEY_SIGNS) {
                    val newDirection = player.direction.rotate(ROTATION_KEY_SIGNS.getValue(key) * PLAYER_ANGULAR_VELOCITY * deltaSec)
                    player.direction = MutableVec2Double(newDirection)
                    player.camPlane = MutableVec2Double(newDirection.rotate(PI / 2))
                }
                else {
                    val direction = player.direction.rotate(STRAFE_KEY_ANGLES.getValue(key))

                    val newPos = MutableVec2Double(player.position + direction * deltaSec * PLAYER_MOVE_RATE)
                    val topLeft = newPos - PLAYER_RADIUS_BLOCKS
                    val bottomRight = newPos + PLAYER_RADIUS_BLOCKS

                    // TODO: Snap to wall if you would clip it to avoid gaps
                    if (MAP[topLeft.y.toInt()][topLeft.x.toInt()].passable and
                        MAP[topLeft.y.toInt()][bottomRight.x.toInt()].passable and
                        MAP[bottomRight.y.toInt()][topLeft.x.toInt()].passable and
                        MAP[bottomRight.y.toInt()][bottomRight.x.toInt()].passable
                    ) {
                        player.position = MutableVec2Double(newPos)
                    }
                }
            }
        }

        // Don't leave the map
        player.position.clamp(
            PLAYER_RADIUS_BLOCKS, MAP_WIDTH_BLOCKS - PLAYER_RADIUS_BLOCKS,
            PLAYER_RADIUS_BLOCKS, MAP_HEIGHT_BLOCKS - PLAYER_RADIUS_BLOCKS
        )
    }

    private fun drawTopDownMap() {
        for ((y, row) in MAP.withIndex()) {
            for ((x, block) in row.withIndex()) {
                val xLocation = x * PX_PER_BLOCK
                val yLocation = y * PX_PER_BLOCK
                if (USE_TEXTURES) {
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

        // Draw player
        topDownCanvas.fillRect(
            (player.position.x * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
            (player.position.y * PX_PER_BLOCK).toInt() - PLAYER_RADIUS_PX,
            PLAYER_RADIUS_PX * 2, PLAYER_RADIUS_PX * 2,
            Color.RED
        )

        // Tag the block the player is in
        val playerBlock = player.position.toVec2Int()
        topDownCanvas.fillRect(playerBlock.x * PX_PER_BLOCK, playerBlock.y * PX_PER_BLOCK, 10, 10, Color.PURPLE)

        // Draw line in the player's direction
        val playerPosPx = (player.position * PX_PER_BLOCK.toDouble()).toVec2Int()
        val playerDirPx = playerPosPx + (player.direction * 20.0).toVec2Int()
        topDownCanvas.strokeLine(playerPosPx.x, playerPosPx.y, playerDirPx.x, playerDirPx.y, Color.BLACK)

        // Draw camera plane
        val playerPlaneEndPx = playerDirPx + (player.camPlane * 20.0).toVec2Int()
        val playerPlaneStartPx = playerDirPx - (player.camPlane * 20.0).toVec2Int()
        topDownCanvas.strokeLine(playerPlaneStartPx.x, playerPlaneStartPx.y, playerPlaneEndPx.x, playerPlaneEndPx.y, Color.BLACK)
    }

    private fun updateFPVBuffer() {
//        val playerPosPx = (player.position * PX_PER_BLOCK.toDouble()).toVec2Int()
        // Draw sky and floor
        if (USE_TEXTURES) {
            val firstRayDir = player.direction - player.camPlane
            val lastRayDir = player.direction + player.camPlane
            val posZ = 0.5 * FPV_ASPECT_HEIGHT_PX.toDouble()
            val halfHeight = FPV_ASPECT_HEIGHT_PX / 2
            val invWidth = 1.0 / FPV_ASPECT_WIDTH_PX
            val maxTextureX = TEXTURE_WIDTH - 1
            val maxTextureY = TEXTURE_HEIGHT - 1

            runBlocking {
                for (screenY in 0 until FPV_ASPECT_HEIGHT_PX) {
                    launch {
                        val centeredScreenY = screenY - halfHeight
                        val rowDistance = posZ / centeredScreenY.toDouble()
                        val floorStep = (lastRayDir - firstRayDir) * (rowDistance * invWidth)
                        var floorX = player.position.x + firstRayDir.x * rowDistance
                        var floorY = player.position.y + firstRayDir.y * rowDistance

                        for (screenX in 0 until FPV_ASPECT_WIDTH_PX) {
                            val cellX = floorX.toInt()
                            val cellY = floorY.toInt()

                            val fracX = floorX - cellX
                            val fracY = floorY - cellY

                            val textureX = ((TEXTURE_WIDTH * fracX).toInt()).coerceIn(0, maxTextureX)
                            val textureY = ((TEXTURE_HEIGHT * fracY).toInt()).coerceIn(0, maxTextureY)

                            val floorColor = FLOOR_TEXTURE.image.pixelReader.getColor(textureX, textureY)
                            val ceilColor = SKY_TEXTURE.image.pixelReader.getColor(textureX, textureY)

                            FPVBuffer.fillRect(screenX * FPV_SCALE, screenY * FPV_SCALE, FPV_SCALE, FPV_SCALE, floorColor)
                            FPVBuffer.fillRect(screenX * FPV_SCALE, (FPV_ASPECT_HEIGHT_PX - screenY - 1) * FPV_SCALE, FPV_SCALE, FPV_SCALE, ceilColor)

                            floorX += floorStep.x
                            floorY += floorStep.y
                        }
                    }
                }
            }
        }
        else {
            FPVBuffer.fillRect(0, 0, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, SKY_COLOR)
            FPVBuffer.fillRect(0, FPV_HEIGHT_PX / 2, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, FLOOR_COLOR)
        }

        // Draw walls
        runBlocking {
            for (screenX in 0 until FPV_ASPECT_WIDTH_PX) {
                launch {
                    val cameraX = 2 * screenX.toDouble() / FPV_ASPECT_WIDTH_PX - 1

                    val rayDir = player.direction + player.camPlane * cameraX

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

                    val lineHeight = (FPV_ASPECT_HEIGHT_PX / perpWallDist).toInt()
                    val drawStart = maxOf(FPV_ASPECT_HEIGHT_PX / 2 - lineHeight / 2, 0)
                    val drawEnd = minOf(FPV_ASPECT_HEIGHT_PX / 2 + lineHeight / 2, FPV_ASPECT_HEIGHT_PX - 1)

                    if (USE_TEXTURES) {
                        val textureReader = MAP[rayMapPos.y][rayMapPos.x].texture.image.pixelReader

                        var wallX =
                            if (hitSide == WallType.EastWest) player.position.y + perpWallDist * rayDir.y
                            else player.position.x + perpWallDist * rayDir.x
                        wallX -= floor(wallX)

                        var texX = (wallX * TEXTURE_WIDTH.toDouble()).toInt()
                        if ((hitSide == WallType.EastWest && rayDir.x > 0) ||
                            (hitSide == WallType.NorthSouth && rayDir.y < 0)
                        ) {
                            texX = TEXTURE_WIDTH - texX - 1
                        }

                        val step = 1.0 * TEXTURE_HEIGHT /  lineHeight
                        var texPos = (drawStart - FPV_ASPECT_HEIGHT_PX / 2 + lineHeight / 2) * step

                        for (y in drawStart until drawEnd) {
                            val texY = minOf(texPos.toInt(), TEXTURE_HEIGHT - 1)
                            texPos += step
                            var color = textureReader.getColor(texX, texY)
                            if (hitSide == WallType.NorthSouth) {
                                color = color.darker()
                            }

                            FPVBuffer.fillRect(screenX * FPV_SCALE, y * FPV_SCALE, FPV_SCALE, FPV_SCALE, color)
                        }
                    }
                    else {
                        var color = MAP[rayMapPos.y][rayMapPos.x].color

                        if (hitSide == WallType.NorthSouth) {
                            color = color.darker()
                        }

                        // lineHeight is not clamped, so use drawEnd - drawStart to find real height
                        FPVBuffer.fillRect(screenX * FPV_SCALE, drawStart * FPV_SCALE, 1 * FPV_SCALE, (drawEnd - drawStart) * FPV_SCALE, color)
                    }

                    //            // Tag the blocks the rays hit
                    //            topDownCanvas.fillRect(rayMapPos.x * PX_PER_BLOCK, rayMapPos.y * PX_PER_BLOCK, 10, 10, Color.GREEN)
                    //
                    //            // Draw lines for rays
                    //            val rayDirPx = playerPosPx + (rayDir * perpWallDist * PX_PER_BLOCK.toDouble()).toVec2Int()
                    //            topDownCanvas.strokeLine(
                    //                playerPosPx.x, playerPosPx.y,
                    //                rayDirPx.x, rayDirPx.y,
                    //                Color.rgb(255, 0, (255.0 * (screenX / FPV_WIDTH_PX.toDouble())).toInt())
                    //            )
                }
            }
        }


    }
}

fun main() {
    Application.launch(Raycaster::class.java)
}
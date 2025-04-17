package edu.cwru.raycaster

import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.text.Font
import javafx.stage.Stage
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.floor

fun Double.format(scale: Int) = "%.${scale}f".format(this)
const val MS_IN_S = 1000.0

const val LOG_PERFORMANCE_METRICS = false

const val USE_TEXTURES = true
const val TEXTURE_WIDTH = 64
const val TEXTURE_HEIGHT = 64
const val PX_PER_BLOCK = TEXTURE_WIDTH

const val USE_MOUSE_INPUT = false
const val PLAYER_MOVE_RATE = 3.0  // blocks / sec
const val PLAYER_KEYBOARD_TURN_RATE = PI   // radians / sec
const val PLAYER_MOUSE_TURN_RATE = PI / 4   // radians / sec

const val PLAYER_RADIUS_BLOCKS = 0.1
const val PLAYER_RADIUS_PX = (PLAYER_RADIUS_BLOCKS * PX_PER_BLOCK).toInt()

const val ENABLE_FLASHLIGHT_FOG = true
const val ENABLE_FLASHLIGHT_VIGNETTE = false
const val FLASHLIGHT_PENETRATION_BLOCKS = 2.0 // After this nothing is visible
const val ENABLE_FLASHLIGHT = ENABLE_FLASHLIGHT_FOG || ENABLE_FLASHLIGHT_VIGNETTE
const val FLASHLIGHT_VIGNETTE_STEEPNESS = 10.0
const val FLASHLIGHT_VIGNETTE_RADIUS = 0.5

// Size before scaling:
const val FPV_ASPECT_WIDTH_PX = 800
const val FPV_ASPECT_HEIGHT_PX = 400
val FPV_ASPECT_CENTER = Vec2Int(FPV_ASPECT_WIDTH_PX / 2, FPV_ASPECT_HEIGHT_PX / 2)

const val FPV_SCALE = 1

// Real pixel size after scaling:
const val FPV_WIDTH_PX = FPV_ASPECT_WIDTH_PX * FPV_SCALE
const val FPV_HEIGHT_PX = FPV_ASPECT_HEIGHT_PX * FPV_SCALE

// Raycasting constants so we don't have to recompute them over and over
const val FPV_HALF_HEIGHT_PX_DOUBLE = FPV_ASPECT_HEIGHT_PX.toDouble() / 2.0
const val FPV_HALF_HEIGHT_PX = FPV_HALF_HEIGHT_PX_DOUBLE.toInt()
const val FPV_INVERSE_WIDTH_PX = 1.0 / FPV_ASPECT_WIDTH_PX
const val MAX_TEXTURE_X = TEXTURE_WIDTH - 1
const val MAX_TEXTURE_Y = TEXTURE_HEIGHT - 1

// Raycasting multithreading stripe sizes
// + 1 so last stripe may be smaller than others
val CPU_CORES_AVAILABLE = Runtime.getRuntime().availableProcessors()
val FLOOR_CEIL_STRIPE_SIZE = FPV_HALF_HEIGHT_PX / CPU_CORES_AVAILABLE + 1
val WALL_STRIPE_SIZE = FPV_ASPECT_WIDTH_PX / CPU_CORES_AVAILABLE + 1

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

data class Ray(val end: Vec2Double, val index: Int) {
    val endBlock = end.toVec2Int()
}

enum class WallType {
    NorthSouth, EastWest
}

class Player {
    var position = MutableVec2Double(2.0, 2.0)
    var direction = MutableVec2Double(-1.0, 0.0)
    var camPlane = MutableVec2Double(direction.rotate(PI / 2))
    var mouseDx = 0.0
    val debugRays = mutableListOf<Ray>()

    val positionPx: Vec2Int
        get() = (position * PX_PER_BLOCK.toDouble()).toVec2Int()

    fun rotate(angle: Double) {
        val newDirection = direction.rotate(angle)
        direction = MutableVec2Double(newDirection)
        camPlane = MutableVec2Double(newDirection.rotate(PI / 2))
    }
}

class Raycaster : Application() {
    private val keyMap: MutableMap<KeyCode, Boolean> = (STRAFE_KEY_ANGLES.keys + ROTATION_KEY_SIGNS.keys).associateWith { false }
        .toMutableMap()

    private var prevFrameTime = 0L
    private val player = Player()
    private val map = Map()
    private val topDownCanvas = ContextualCanvas(map.mapWidthPx, map.mapHeightPx)
    private val firstPersonCanvas = ContextualCanvas(FPV_WIDTH_PX, FPV_HEIGHT_PX)
    private val frameRateLabel = Label("No FPS data")

    private var recenteringMouse = false
    private var escaped = false
    private val robot = Robot()

    private lateinit var primaryStage: Stage

    override fun start(stage: Stage) {
        primaryStage = stage
        primaryStage.title = "Kotlin Raycaster"

        val padding20 = Insets(20.0, 20.0, 20.0, 20.0)

        val root = VBox().apply {
            background = Background(BackgroundFill(Color.BLACK, CornerRadii(0.0), Insets(0.0)))
            children.add(frameRateLabel.apply {
                textFill = Color.WHITE
                font = Font(20.0)
                padding = padding20
            })
            children.add(HBox().apply {
                children.add(StackPane().apply {
                    children.add(topDownCanvas.apply { alignment = Pos.CENTER })
                    padding = padding20
                })
                children.add(StackPane().apply {
                    children.add(firstPersonCanvas.apply { alignment = Pos.CENTER })
                    padding = padding20
                })
            })
        }

        primaryStage.scene = Scene(root, map.mapWidthPx + FPV_WIDTH_PX + 4 * 20.0, FPV_HEIGHT_PX + 200.0).apply {
            setOnKeyPressed {
                if (it.code in keyMap.keys) {
                    keyMap[it.code] = true
                }
                else if (it.code == KeyCode.ESCAPE) {
                    escaped = true
                }
            }
            setOnKeyReleased {
                if (it.code in keyMap.keys) {
                    keyMap[it.code] = false
                }
            }

            if (USE_MOUSE_INPUT) {
                cursor = Cursor.NONE
                centerMouse()
                setOnMouseMoved {
                    if (recenteringMouse || escaped) {
                        recenteringMouse = false
                        return@setOnMouseMoved
                    }

                    player.mouseDx = (it.screenX - (primaryStage.x + primaryStage.width / 2)) / (primaryStage.width / 2)

                    recenteringMouse = true
                    centerMouse()
                }
            }
        }

        primaryStage.show()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()

                // Time delta from previous frame in seconds; multiply rates by this to deal with variable FPS
                val deltaSec = (now - prevFrameTime) / MS_IN_S
                // Instantaneous frame rate
                val frameRate = 1.0 / deltaSec

                prevFrameTime = now

                withContext(Dispatchers.Main) {
                    frameRateLabel.text = "FPS: ${frameRate.format(2)}\nδS: ${deltaSec.format(2)}"
                }

                var time1 = System.currentTimeMillis()
                var time2: Long

                updateState(deltaSec)
                if (LOG_PERFORMANCE_METRICS) {
                    time2 = System.currentTimeMillis()
                    println("State update used: ${time2 - time1}")
                    time1 = time2
                }

                launch(Dispatchers.Main) {
                    drawTopDownMap()
                }.join()
                if (LOG_PERFORMANCE_METRICS) {
                    time2 = System.currentTimeMillis()
                    println("Top down draw used: ${time2 - time1}")
                    time1 = time2
                }

                launch { bufferFloorCeil() }.join()
                launch { bufferWalls() }.join()
                if (LOG_PERFORMANCE_METRICS) {
                    time2 = System.currentTimeMillis()
                    println("FPV buffer update used: ${time2 - time1}")
                    time1 = time2
                }

                launch(Dispatchers.Main) {
                    firstPersonCanvas.flushBuffer()
                }.join()
                if (LOG_PERFORMANCE_METRICS) {
                    time2 = System.currentTimeMillis()
                    println("FPV buffer draw used: ${time2 - time1}")
                }
            }
        }
    }

    private fun centerMouse() {
        robot.mouseMove(
            primaryStage.x + primaryStage.width / 2,
            primaryStage.y + primaryStage.height / 2
        )
    }

    private fun updateState(deltaSec: Double) {
        player.rotate(player.mouseDx * PLAYER_MOUSE_TURN_RATE)
        player.mouseDx = 0.0

        // Don't walk through walls
        for ((key, pressed) in keyMap) {
            if (pressed) {
                if (key in ROTATION_KEY_SIGNS) {
                    player.rotate(ROTATION_KEY_SIGNS.getValue(key) * PLAYER_KEYBOARD_TURN_RATE * deltaSec)
                }
                else {
                    val direction = player.direction.rotate(STRAFE_KEY_ANGLES.getValue(key))

                    val newPos = MutableVec2Double(player.position + direction * deltaSec * PLAYER_MOVE_RATE)
                    val topLeft = newPos - PLAYER_RADIUS_BLOCKS
                    val bottomRight = newPos + PLAYER_RADIUS_BLOCKS

                    // TODO: Snap to wall if you would clip it to avoid gaps
                    if (map.map[topLeft.y.toInt()][topLeft.x.toInt()].passable and
                        map.map[topLeft.y.toInt()][bottomRight.x.toInt()].passable and
                        map.map[bottomRight.y.toInt()][topLeft.x.toInt()].passable and
                        map.map[bottomRight.y.toInt()][bottomRight.x.toInt()].passable
                    ) {
                        player.position = MutableVec2Double(newPos)
                    }
                }
            }
        }

        // Don't leave the map
        player.position.clamp(
            PLAYER_RADIUS_BLOCKS, map.mapWidthBlocks - PLAYER_RADIUS_BLOCKS,
            PLAYER_RADIUS_BLOCKS, map.mapHeightBlocks - PLAYER_RADIUS_BLOCKS
        )
    }

    private fun drawTopDownMap() {
        for ((y, row) in map.map.withIndex()) {
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

        val playerPosPx = player.positionPx

        for (ray in player.debugRays) {
            // Tag the blocks the rays hit
            topDownCanvas.fillRect(ray.endBlock.x * PX_PER_BLOCK, ray.endBlock.y * PX_PER_BLOCK, 10, 10, Color.GREEN)

            // Draw lines for rays
            val rayDirPx = (ray.end * PX_PER_BLOCK.toDouble()).toVec2Int()
            topDownCanvas.strokeLine(
                playerPosPx.x, playerPosPx.y,
                rayDirPx.x, rayDirPx.y,
                Color.rgb(255, 0, (255.0 * (ray.index / FPV_WIDTH_PX.toDouble())).toInt())
            )
        }

        // Tag the block the player is in
        val playerBlock = player.position.toVec2Int()
        topDownCanvas.fillRect(playerBlock.x * PX_PER_BLOCK, playerBlock.y * PX_PER_BLOCK, 10, 10, Color.PURPLE)

        // Draw line in the player's direction
        val playerDirPx = playerPosPx + (player.direction * 20.0).toVec2Int()
        topDownCanvas.strokeLine(playerPosPx.x, playerPosPx.y, playerDirPx.x, playerDirPx.y, Color.BLACK)

        // Draw camera plane
        val playerPlaneEndPx = playerDirPx + (player.camPlane * 20.0).toVec2Int()
        val playerPlaneStartPx = playerDirPx - (player.camPlane * 20.0).toVec2Int()
        topDownCanvas.strokeLine(playerPlaneStartPx.x, playerPlaneStartPx.y, playerPlaneEndPx.x, playerPlaneEndPx.y, Color.BLACK)
    }

    private suspend fun bufferFloorCeil() = coroutineScope {
        if (USE_TEXTURES) {
            val firstRayDir = player.direction - player.camPlane
            val lastRayDir = player.direction + player.camPlane

            val floorStripeJobs = (FPV_HALF_HEIGHT_PX until FPV_ASPECT_HEIGHT_PX step FLOOR_CEIL_STRIPE_SIZE).map { stripeStart ->
                launch(Dispatchers.Default) {
                    for (screenY in stripeStart until minOf(stripeStart + FLOOR_CEIL_STRIPE_SIZE, FPV_ASPECT_HEIGHT_PX)) {
                        val centeredScreenY = screenY - FPV_HALF_HEIGHT_PX
                        val rowDistance = FPV_HALF_HEIGHT_PX_DOUBLE / centeredScreenY.toDouble()
                        val floorStep = (lastRayDir - firstRayDir) * (rowDistance * FPV_INVERSE_WIDTH_PX)
                        var floorX = player.position.x + firstRayDir.x * rowDistance
                        var floorY = player.position.y + firstRayDir.y * rowDistance

                        for (screenX in 0 until FPV_ASPECT_WIDTH_PX) {
                            val cellX = floorX.toInt()
                            val cellY = floorY.toInt()

                            val fracX = floorX - cellX
                            val fracY = floorY - cellY

                            val textureX = ((TEXTURE_WIDTH * fracX).toInt()).coerceIn(0, MAX_TEXTURE_X)
                            val textureY = ((TEXTURE_HEIGHT * fracY).toInt()).coerceIn(0, MAX_TEXTURE_Y)

                            var floorColor = Map.floorTexture.image.pixelReader.getColor(textureX, textureY)
                            var ceilColor = Map.ceilingTexture.image.pixelReader.getColor(textureX, textureY)

                            val ceilScreenY = (FPV_ASPECT_HEIGHT_PX - screenY - 1)

                            if (ENABLE_FLASHLIGHT) {
                                floorColor = applyFlashlight(floorColor, screenX, screenY, rowDistance)
                                ceilColor = applyFlashlight(ceilColor, screenX, ceilScreenY, rowDistance)
                            }

                            firstPersonCanvas.bufferRect(
                                screenX * FPV_SCALE, screenY * FPV_SCALE,
                                FPV_SCALE, FPV_SCALE, floorColor
                            )

                            firstPersonCanvas.bufferRect(
                                screenX * FPV_SCALE, ceilScreenY * FPV_SCALE,
                                FPV_SCALE, FPV_SCALE, ceilColor
                            )

                            floorX += floorStep.x
                            floorY += floorStep.y
                        }
                    }
                }
            }

            floorStripeJobs.joinAll()
        }
        else {
            firstPersonCanvas.bufferRect(0, 0, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, Map.ceilingColor)
            firstPersonCanvas.bufferRect(0, FPV_HEIGHT_PX / 2, FPV_WIDTH_PX, FPV_HEIGHT_PX / 2, Map.floorColor)
        }
    }

    private fun applyFlashlight(color: Color, screenX: Int, screenY: Int, distance: Double): Color {
        // Shortcut to skip calculations if we're completely out of range
        if (ENABLE_FLASHLIGHT_FOG && FLASHLIGHT_PENETRATION_BLOCKS < distance) {
            return Color.BLACK
        }

        val fogCoefficient =
            if (ENABLE_FLASHLIGHT_FOG) maxOf(1.0 - distance / FLASHLIGHT_PENETRATION_BLOCKS, 0.0)
            else 1.0

        var vignetteCoefficient = 1.0
        if (ENABLE_FLASHLIGHT_VIGNETTE) {
            val screenPos = Vec2Int(screenX, screenY) - FPV_ASPECT_CENTER
            val normDistFromCenter = screenPos.magnitude / FPV_ASPECT_CENTER.magnitude
            vignetteCoefficient = (-0.5 * (FLASHLIGHT_VIGNETTE_STEEPNESS *
                    (normDistFromCenter - FLASHLIGHT_VIGNETTE_RADIUS)) + 0.5).coerceIn(0.0, 1.0)
        }

        return color.deriveColor(0.0, 1.0, fogCoefficient * vignetteCoefficient, 1.0)
    }

    private suspend fun bufferWalls() = coroutineScope {
        player.debugRays.clear()

        val wallStripeJobs = (0 until FPV_ASPECT_WIDTH_PX step WALL_STRIPE_SIZE).map { stripeStart ->
            launch(Dispatchers.Default) {
                for (screenX in stripeStart until minOf(stripeStart + WALL_STRIPE_SIZE, FPV_ASPECT_WIDTH_PX)) {
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
                        if (!map.map[rayMapPos.y][rayMapPos.x].passable) {
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
                        val textureReader = map.map[rayMapPos.y][rayMapPos.x].texture.image.pixelReader

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

                        val step = 1.0 * TEXTURE_HEIGHT / lineHeight
                        var texPos = (drawStart - FPV_ASPECT_HEIGHT_PX / 2 + lineHeight / 2) * step

                        for (y in drawStart until drawEnd) {
                            val texY = minOf(texPos.toInt(), TEXTURE_HEIGHT - 1)
                            texPos += step

                            var color = textureReader.getColor(texX, texY)

                            if (ENABLE_FLASHLIGHT) {
                                color = applyFlashlight(color, screenX, y, perpWallDist)
                            }
                            else if (hitSide == WallType.NorthSouth) {
                                color = color.darker()
                            }

                            firstPersonCanvas.bufferRect(screenX * FPV_SCALE, y * FPV_SCALE, FPV_SCALE, FPV_SCALE, color)
                        }
                    }
                    else {
                        var color = map.map[rayMapPos.y][rayMapPos.x].color

                        if (hitSide == WallType.NorthSouth) {
                            color = color.darker()
                        }

                        // lineHeight is not clamped, so use drawEnd - drawStart to find real height
                        firstPersonCanvas.bufferRect(
                            screenX * FPV_SCALE, drawStart * FPV_SCALE,
                            1 * FPV_SCALE, (drawEnd - drawStart) * FPV_SCALE,
                            color
                        )
                    }

                    // Add rays for debug rendering later
                    player.debugRays.add(Ray(
                        player.position + rayDir * perpWallDist,
                        screenX
                    ))
                }
            }
        }

        wallStripeJobs.joinAll()
    }
}

fun main() {
    println("Running on $CPU_CORES_AVAILABLE CPU cores")
    Application.launch(Raycaster::class.java)
}
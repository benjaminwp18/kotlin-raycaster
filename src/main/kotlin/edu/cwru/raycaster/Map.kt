package edu.cwru.raycaster

import javafx.scene.paint.Color
import kotlin.random.Random

class Map {
    companion object {
        // Must manually type to get correct nullability from Java Color class
        val ceilingColor: Color = Color.LIGHTBLUE
        val floorColor: Color = Color.LIGHTGRAY
        val ceilingTexture = Texture.WOOD
        val floorTexture = Texture.GREY_STONE
    }

    val map: Array<Array<Block>> = generateDungeonString(21, 21).toBlockMap()
    val mapWidthBlocks = map[0].size
    val mapHeightBlocks = map.size
}

data class Block(val color: Color,
                 val texture: Texture,
                 val passable: Boolean = false) {
    companion object {
        private val CHAR_TO_BLOCK = mapOf(
            ' ' to Block(Color.WHITE, Map.floorTexture, true),

            'B' to Block(Color.BLUE, Texture.BLUE_BRICK),
            'R' to Block(Color.RED, Texture.RED_BRICK),

            'S' to Block(Color.DARKGRAY, Texture.COLOR_STONE),
            'G' to Block(Color.LIGHTGRAY, Texture.GREY_STONE),
            'M' to Block(Color.GREEN, Texture.MOSSY),

            'W' to Block(Color.BROWN, Texture.WOOD),
            'E' to Block(Color.ORANGE, Texture.EAGLE),
        ).withDefault { Block(Color.PURPLE, Texture.PURPLE_STONE) }

        fun fromChar(c: Char) = CHAR_TO_BLOCK.getValue(c)
    }
}

fun String.toBlockMap(): Array<Array<Block>> {
    val lines = split('\n')
    return Array(lines.size) { y ->
        Array(lines[y].length) { x ->
            Block.fromChar(lines[y][x])
        }
    }
}

fun generateDungeonString(
    width: Int, height: Int,
    roomAttempts: Int = 5,
    minRoomWidth: Int = 6, maxRoomWidth: Int = 8,
    minRoomHeight: Int = 4, maxRoomHeight: Int = 6,
    wallChars: List<Char> = listOf('B', 'R', 'G', 'M', 'E')
): String {
    val dungeon = generateMaze(width, height)

    // Add rooms
    repeat(roomAttempts) {
        val roomWidth = Random.nextInt(minRoomWidth, maxRoomWidth)
        val roomHeight = Random.nextInt(minRoomHeight, maxRoomHeight)
        val roomX = Random.nextInt(1, width - roomWidth - 1)
        val roomY = Random.nextInt(1, height - roomHeight - 1)

        // Don't overlap w/another room's center
        var overlaps = false
        for (y in roomY until roomY + roomHeight) {
            for (x in roomX until roomX + roomWidth) {
                if (dungeon[y][x] == '#') {
                    overlaps = true
                    break
                }
            }
        }

        if (!overlaps) {
            val roomChar = wallChars.random()
            for (y in roomY until roomY + roomHeight) {
                for (x in roomX until roomX + roomWidth) {
                    if (y == roomY || x == roomX || y == roomY + roomHeight - 1 || x == roomX + roomWidth - 1) {
                        // Add walls
                        if (dungeon[y][x] != ' ') {
                            dungeon[y][x] = roomChar
                        }
                    }
                    else {
                        // Mark center
                        dungeon[y][x] = '#'
                    }
                }
            }
        }
    }

    return dungeon.joinToString("\n") { row ->
        row.map { if (it == '#') ' ' else it }
            .joinToString("")
    }
}

fun generateMaze(width: Int, height: Int): Array<CharArray> {
    val maze = Array(height) { CharArray(width) { 'S' } }

    maze[1][1] = ' '
    dfsMaze(Vec2Int(1, 1), maze)

    return maze
}

fun dfsMaze(pos: Vec2Int, maze: Array<CharArray>) {
    val directions = listOf(Vec2Int(0, -1), Vec2Int(0, 1),
        Vec2Int(-1, 0), Vec2Int(1, 0)).shuffled()

    for (dir in directions) {
        val neighbor = pos + dir
        val farNeighbor = neighbor + dir

        if (1 <= farNeighbor.y && farNeighbor.y < maze.size - 1 &&
            1 <= farNeighbor.x && farNeighbor.x < maze[0].size - 1 &&
            maze[farNeighbor.y][farNeighbor.x] != ' ') {
            maze[neighbor.y][neighbor.x] = ' '
            maze[farNeighbor.y][farNeighbor.x] = ' '
            dfsMaze(farNeighbor, maze)
        }
    }
}
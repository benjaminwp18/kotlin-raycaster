package edu.cwru.raycaster

import javafx.scene.paint.Color

class Map {
    val map = """
        SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
        S     S         S       S       S       S
        SSSSS SW     WS SSSSS S S S SSSSS SSS SSS
        S   S         S     S S   S   S   S S   S
        S SSSSSWWWWWW SSSSS SSSSSSS S S SSS SSS S
        S S           S   S       S S         S S
        S S SSS SSSSS SSS SSSSSSS S SSSSSSSSSSS S
        S S   S S   S         S S S   S       S S
        S SSS S S SSSSSSSSSSS S S SSSSS SSSSS S S
        S     S S     S     S S S S   S   S   S S
        S SSSSS S S SSS SSS S S S S S S S S SSS S
        S S   S S S       S   S S S S S S S   S S
        S S S S S SSSSSSSSS SSS S S S SSS SSS S S
        S   S S S     S   S     S S S     S   S S
        SSSSS S SSSSSSS S SSSSS S S SSSSSSS SSS S
        S   E E         S S     S S S     S   S S
        S E    ESSSSSSSSS SSSSS S S S SSS SSS S S
        S             S S     S S   S S   S   S S
        S E EEEESSSSS S SSSSS SSSSSSS S SSS SSS S
        S S                 S         S         S
        SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
    """.trimIndent().toBlockMap()

    companion object {
        // Must manually type to get correct nullability from Java Color class
        val ceilingColor: Color = Color.LIGHTBLUE
        val floorColor: Color = Color.LIGHTGRAY
        val ceilingTexture = Texture.WOOD
        val floorTexture = Texture.GREY_STONE
    }

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
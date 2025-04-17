package edu.cwru.raycaster

import javafx.scene.paint.Color

class Map {
    val map = """
        BBBBBB
        G    B
        B  B B
        O    B
        BBBBBB
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
    val mapWidthPx = mapWidthBlocks * PX_PER_BLOCK
    val mapHeightPx = mapHeightBlocks * PX_PER_BLOCK
}

data class Block(val color: Color,
                 val texture: Texture,
                 val passable: Boolean = false) {
    companion object {
        private val CHAR_TO_BLOCK = mapOf(
            ' ' to Block(Color.WHITE, Map.floorTexture, true),
            'B' to Block(Color.BLUE, Texture.BLUE_BRICK),
            'G' to Block(Color.GREEN, Texture.WOOD),
            'O' to Block(Color.ORANGE, Texture.EAGLE),
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
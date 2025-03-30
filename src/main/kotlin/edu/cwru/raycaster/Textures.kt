package edu.cwru.raycaster

import javafx.scene.image.Image
import java.io.FileInputStream

const val TEXTURE_PATH = "src/main/resources/edu/cwru/raycaster/textures/"

enum class Texture(fileName: String) {
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
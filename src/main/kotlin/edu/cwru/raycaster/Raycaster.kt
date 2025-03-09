package edu.cwru.raycaster

import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button

class Raycaster : Application() {
    override fun start(primaryStage: Stage) {
        primaryStage.title = "Hello World!"
        val btn: Button = Button()
        btn.text = "Say 'Hello World'"
        btn.onAction = EventHandler<ActionEvent?> {
            println("Hello World!")
        }

        val root = StackPane()
        root.children.add(btn)
        primaryStage.scene = Scene(root, 300.0, 250.0)
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Raycaster::class.java)
}
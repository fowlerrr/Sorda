package com.sorda.jfx

import com.sorda.jfx.controllers.NodeController
import com.sorda.jfx.views.LoginView
import javafx.application.Application
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import tornadofx.App

class Main : App(LoginView::class) {

    private val nodeController by inject<NodeController>()

    // List of references for daemon threads (such as JavaFX server)
    private val threads: List<Thread> = mutableListOf()

    override fun start(stage: Stage) {
        super.start(stage)
        stage.setOnCloseRequest {
            val button = Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to exit?").apply {
                initOwner(stage.scene.window)
            }.showAndWait().get()
            if (button == ButtonType.OK) {
                nodeController.close()
            } else {
                it.consume()
            }
        }
    }

    override fun stop() {
        super.stop()
        for (thread in threads) {
            thread.interrupt()
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}
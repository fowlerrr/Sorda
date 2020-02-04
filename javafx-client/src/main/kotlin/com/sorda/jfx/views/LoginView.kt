package com.sorda.jfx.views

import com.sorda.jfx.controllers.AlertHelper
import com.sorda.jfx.controllers.NodeController
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import tornadofx.View
import tornadofx.importStylesheet


class LoginView : View("Login") {

    override val root by fxml<Parent>()

    private val nodeController by inject<NodeController>()

    private val host by fxid<TextField>()
    private val port by fxid<TextField>()
    private val username by fxid<TextField>()
    private val password by fxid<PasswordField>()
    private val loginButton by fxid<Button>()

    init {
        importStylesheet("/style.css")
        loginButton.setOnAction { login() }
    }

    private fun login() {
        val inputFields = listOf(host, port, username, password)
        inputFields.forEach { AlertHelper.removeRedBorder(it) }
        val invalidCount = inputFields.count { it.emptyInput() }

        if (invalidCount == 0) {
            try {
                setInputStates(true)
                nodeController.login(host.text, port.text.toInt(), username.text, password.text)
                replaceWith<LobbyView>()
            } catch (e: Exception) {
                AlertHelper.showAlert(Alert.AlertType.ERROR, loginButton.scene.window, "Login Error", e.message!!)
                setInputStates(false)
            }
        }
    }

    private fun TextField.emptyInput(): Boolean {
        return if (text.isNullOrBlank()) {
            AlertHelper.setRedBorder(this)
            true
        } else {
            false
        }
    }

    private fun setInputStates(disabled: Boolean) {
        listOf(host, port, username, password, loginButton).forEach {
            it.isDisable = disabled
        }
    }
}
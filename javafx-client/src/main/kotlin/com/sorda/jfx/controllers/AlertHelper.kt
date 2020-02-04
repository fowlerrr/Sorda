package com.sorda.jfx.controllers

import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonType
import javafx.scene.control.TextField
import javafx.stage.Window


object AlertHelper {

    fun showAlert(alertType: Alert.AlertType, owner: Window, title: String, message: String) {
        val alert = Alert(alertType)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.initOwner(owner)
        alert.show()
    }

    fun requestConfirmation(
            alertType: Alert.AlertType,
            owner: Window,
            title: String,
            message: String
    ): Boolean {
        val alert = Alert(alertType)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.initOwner(owner)
        val yesButton = ButtonType("Yes", ButtonData.YES)
        val noButton = ButtonType("No", ButtonData.NO)
        alert.buttonTypes.setAll(yesButton, noButton)

        val result = alert.showAndWait()

        return result.isPresent && result.get() == yesButton
    }

    fun requestBidDetails(
        owner: Window,
        title: String,
        message: String
        ): Boolean {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Place Bid"
            alert.headerText = null
            alert.contentText = message
            alert.initOwner(owner)
            val yesButton = ButtonType("Yes", ButtonData.YES)
            val noButton = ButtonType("No", ButtonData.NO)
            alert.buttonTypes.setAll(yesButton, noButton)

            val result = alert.showAndWait()

            return result.isPresent && result.get() == yesButton
    }

    fun setRedBorder(textField: TextField) {
        val styleClass = textField.styleClass
        if (!styleClass.contains("tf-red-border")) {
            styleClass.add("tf-red-border")
        }
    }

    fun removeRedBorder(textField: TextField) {
        textField.styleClass.removeAll("tf-red-border")
    }
}
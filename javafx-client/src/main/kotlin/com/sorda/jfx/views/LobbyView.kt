package com.sorda.jfx.views

import com.sorda.jfx.controllers.AlertHelper
import com.sorda.jfx.controllers.NodeController
import com.sorda.states.BidState
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Tab
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import tornadofx.View
import tornadofx.selectedItem


class LobbyView : View("Main controller")  {

    override val root by fxml<Parent>()

    private val nodeController by inject<NodeController>()

    private val listedItemsTab by fxid<Tab>()
    private val myBidsTab by fxid<Tab>()
    private val myItemsTab by fxid<Tab>()

    private val listedItemsTable by fxid<TableView<BidState>>()
    private val listedDescriptionColumn by fxid<TableColumn<BidState, String>>()
    private val listedPriceColumn by fxid<TableColumn<BidState, String>>()
    private val listedByColumn by fxid<TableColumn<BidState, String>>()
    private val auctionEndColumn by fxid<TableColumn<BidState, String>>()

    private val bidButton by fxid<Button>()
    private val bidAmountField by fxid<TextField>()

    init {
        refreshListedItems()
        listedItemsTab.setOnSelectionChanged { refreshListedItems() }
        myBidsTab.setOnSelectionChanged { refreshMyBids() }
        myItemsTab.setOnSelectionChanged { refreshMyItems() }
        bidButton.setOnAction { handleBidButtonAction() }
    }

    private fun refreshListedItems() {
        listedItemsTable.items?.clear()
        val currentItems = nodeController.getListedItems()

        listedDescriptionColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.description) }
        listedPriceColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.lastPrice.toString()) }
        listedByColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.issuer.name.toString()) }
        auctionEndColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.expiry.toString()) }

        listedItemsTable.items.addAll(currentItems)
    }

    private fun refreshMyItems() {
        
    }

    private fun refreshMyBids() {

    }

    private fun handleBidButtonAction() {
        val owner = bidButton.scene.window
        val selectedItem = listedItemsTable.selectedItem
        if (selectedItem == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Please select an item to bid on!")
            return
        }
        val bidAmount = bidAmountField.text
        if (bidAmount.isNullOrBlank() || bidAmount.toDoubleOrNull() == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The bid amount must be a number!")
            return
        }

        nodeController.bidOnItem(selectedItem.itemLinearId, bidAmount.toDouble())
    }
}

package com.sorda.jfx.views

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.sorda.jfx.BidData
import com.sorda.jfx.ItemData
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
import net.corda.core.contracts.Amount
import tornadofx.View
import tornadofx.selectedItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class LobbyView : View("Main controller")  {

    override val root by fxml<Parent>()

    private val nodeController by inject<NodeController>()

    private val listedItemsTab by fxid<Tab>()
    private val myBidsTab by fxid<Tab>()
    private val myItemsTab by fxid<Tab>()

    private val listedItemsTable by fxid<TableView<BidState>>()
    private val listedDescriptionColumn by fxid<TableColumn<BidState, String>>()
    private val listedPriceColumn by fxid<TableColumn<BidState, String>>()
    private val listedHighestBidderColumn by fxid<TableColumn<BidState, String>>()
    private val listedByColumn by fxid<TableColumn<BidState, String>>()
    private val auctionEndColumn by fxid<TableColumn<BidState, String>>()

    private val myBidsTable by fxid<TableView<BidData>>()
    private val myBidDescriptionColumn by fxid<TableColumn<BidData, String>>()
    private val myBidAmountBidColumn by fxid<TableColumn<BidData, String>>()
    private val myBidStatusColumn by fxid<TableColumn<BidData, String>>()
    private val myBidAuctionEndColumn by fxid<TableColumn<BidData, String>>()

    private val myItemsTable by fxid<TableView<ItemData>>()
    private val myItemsNameColumn by fxid<TableColumn<ItemData, String>>()
    private val myItemsListedColumn by fxid<TableColumn<ItemData, String>>()

    private val bidButton by fxid<Button>()
    private val bidAmount by fxid<TextField>()

    private val listItemButton by fxid<Button>()
    private val listNewItemButton by fxid<Button>()
    private val listItemDescription by fxid<TextField>()
    private val listItemStartingPrice by fxid<TextField>()
    private val listItemExpiry by fxid<TextField>()

    init {
        currentStage?.height = 600.0
        currentStage?.width = 820.0

        refreshListedItems()
        listedItemsTab.setOnSelectionChanged { refreshListedItems() }
        myBidsTab.setOnSelectionChanged { refreshMyBids() }
        myItemsTab.setOnSelectionChanged { refreshMyItems() }
        bidButton.setOnAction { handleBidButtonAction() }
        listNewItemButton.setOnAction { handleListNewItemButtonAction() }
        listItemButton.setOnAction { handleListItemButtonAction() }

        // Listed items tab
        listedDescriptionColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.description) }
        listedPriceColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.lastPrice.toReadableString()) }
        listedHighestBidderColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.lastSuccessfulBidder.name.organisation) }
        listedByColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.issuer.name.organisation) }
        auctionEndColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.expiry.toString()) }

        // My bids tab
        myBidDescriptionColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bidState.description) }
        myBidAmountBidColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bidState.lastPrice.toReadableString()) }
        myBidStatusColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bidStatus.toString()) }
        myBidAuctionEndColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bidState.expiry.toString()) }

        // My items tab
        myItemsNameColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.itemState.name) }
        myItemsListedColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.listed.toString()) }
    }

    private fun refreshListedItems() {
        listedItemsTable.items?.clear()
        val currentItems = nodeController.getListedItems()
        listedItemsTable.items.addAll(currentItems)
    }

    private fun refreshMyBids() {
        myBidsTable.items?.clear()
        val myBids = nodeController.getMyBids()
        myBidsTable.items.addAll(myBids)
    }

    private fun refreshMyItems() {
        myItemsTable.items?.clear()
        val myItems = nodeController.getMyItems()
        myItemsTable.items.addAll(myItems)
        listItemDescription.clear()
        listItemStartingPrice.clear()
        listItemExpiry.text = "2020-02-04T23:00:00" // placeholder
    }

    private fun handleBidButtonAction() {
        val owner = bidButton.scene.window
        val selectedItem = listedItemsTable.selectedItem
        if (selectedItem == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Please select an item to bid on!")
            return
        }

        val bidAmount = bidAmount.text
        if (bidAmount.isNullOrBlank() || bidAmount.toDoubleOrNull() == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The bid amount must be a number!")
            return
        }

        if (selectedItem.issuer == nodeController.ourIdentity.legalIdentities.last()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "You cannot bid on your own listing!")
            return
        }

        val bidAmountVal = bidAmount.toDouble()
        if (bidAmountVal <= selectedItem.lastPrice.quantity) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Your bid must be higher than the current price!")
            return
        }

        nodeController.bidOnItem(selectedItem, bidAmount.toDouble())
        refreshListedItems()
    }

    private fun handleListNewItemButtonAction() {
        val owner = listNewItemButton.scene.window
        val description = listItemDescription.text
        if (description == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Please input an item description!")
            return
        }
        val startingPriceString = listItemStartingPrice.text
        if (startingPriceString.isNotBlank() && startingPriceString.toDoubleOrNull() == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The starting price must be a number!")
            return
        }
        val startingPrice = startingPriceString.toDoubleOrNull() ?: 0.0

        val expiryString = listItemExpiry.text
        val expiry = try {
            LocalDateTime.parse(expiryString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The expiry must be a valid date!")
            return
        }
        if (expiry.isBefore(LocalDateTime.now())) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The expiry must be a valid date in the future!")
            return
        }
        nodeController.createAndListItem(description, startingPrice, Instant.from(expiry.atOffset(ZoneOffset.UTC)))
        refreshMyItems()
    }

    private fun handleListItemButtonAction() {
        val owner = listItemButton.scene.window

        val selectedItem = myItemsTable.selectedItem
        if (selectedItem == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Please select an item to list on!")
            return
        }

        if (selectedItem.listed) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "You cannot list an already listed item!")
            return
        }

        val startingPriceString = listItemStartingPrice.text
        if (startingPriceString.isNotBlank() && startingPriceString.toDoubleOrNull() == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The starting price must be a number!")
            return
        }
        val startingPrice = startingPriceString.toDoubleOrNull() ?: 0.0

        val expiryString = listItemExpiry.text
        val expiry = try {
            LocalDateTime.parse(expiryString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The expiry must be a valid date!")
            return
        }
        if (expiry.isBefore(LocalDateTime.now())) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "The expiry must be a valid date in the future!")
            return
        }
        nodeController.listItem(selectedItem.itemState, startingPrice, Instant.from(expiry.atOffset(ZoneOffset.UTC)))
        refreshMyItems()
    }

    private fun Amount<TokenType>.toReadableString(): String {
        return token.tokenIdentifier + quantity
    }
}

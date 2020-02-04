//package com.sorda.jfx.views
//
//import com.sorda.jfx.controllers.AlertHelper
//import com.sorda.jfx.controllers.NodeController
//import com.sorda.states.BidState
//import javafx.application.Platform
//import javafx.beans.property.SimpleStringProperty
//import javafx.scene.Parent
//import javafx.scene.control.Alert
//import javafx.scene.control.Button
//import javafx.scene.control.Tab
//import javafx.scene.control.TableColumn
//import javafx.scene.control.TableView
//import tornadofx.View
//import tornadofx.selectedItem
//
//
//class BidView : View("Main controller")  {
//
//    override val root by fxml<Parent>()
//
//    private val nodeController by inject<NodeController>()
//
//    private val listedItemsTab by fxid<Tab>()
//    private val myBidsTab by fxid<Tab>()
//    private val myItemsTab by fxid<Tab>()
//
//    private val listedItemsTable by fxid<TableView<BidState>>()
//    private val listedDescriptionColumn by fxid<TableColumn<BidState, String>>()
//    private val listedPriceColumn by fxid<TableColumn<BidState, String>>()
//    private val listedByColumn by fxid<TableColumn<BidState, String>>()
//    private val auctionEndColumn by fxid<TableColumn<BidState, String>>()
//
//    private val placeBidButton by fxid<Button>()
//
//    init {
//        refreshListedItems()
//        listedItemsTab.setOnSelectionChanged { refreshListedItems() } // Possibly just refreshes every time
//    }
//
//    private fun refreshListedItems() {
//        listedItemsTable.items?.clear()
//        val currentItems = nodeController.getListedItems()
//
//        listedDescriptionColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.description) }
//        listedPriceColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.lastPrice.toString()) }
//        listedByColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.issuer.name.toString()) }
//        auctionEndColumn.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.expiry.toString()) }
//
//        listedItemsTable.items.addAll(currentItems)
//    }
//
//    private fun handlePlaceBidButtonAction() {
//        val owner = bidButton.scene.window
//        val selectedItem = listedItemsTable.selectedItem
//        if (selectedItem == null) {
//            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", "Please select an item to bid on")
//            return
//        }
//
//            val tableView = find<TableView>()
//            tableView.openWindow()
//
//        try {
//            nodeController.initiateGame(partyX500Name)
//        } catch (ex: Throwable) {
//            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Error!", ex.message!!)
//        }
//    }
//    }
//}

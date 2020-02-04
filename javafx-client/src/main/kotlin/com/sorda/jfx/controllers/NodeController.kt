package com.sorda.jfx.controllers

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetListedItemsFlow
import com.sorda.flows.session.PlaceBidFirstFlow
import com.sorda.flows.session.PlaceBidSecondFlow
import com.sorda.jfx.BidData
import com.sorda.jfx.BidStatus
import com.sorda.jfx.ItemData
import com.sorda.jfx.NodeRPCConnection
import com.sorda.states.BidState
import com.sorda.states.ItemState
import net.corda.core.messaging.startFlow
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import tornadofx.Controller
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors

class NodeController: Controller() {

    private lateinit var nodeRPCConnection: NodeRPCConnection
    lateinit var ourIdentity: NodeInfo

    private val flowExecutorPool = Executors.newFixedThreadPool(5)

    fun login(host: String, port: Int, username: String, password: String) {
        initialiseNodeRPCConnection(host, port, username, password)
        ourIdentity = nodeRPCConnection.proxy.nodeInfo()
    }

    fun close() {
        try {
            flowExecutorPool.shutdownNow()
            nodeRPCConnection.close()
        } catch (e: Exception) {
            println("Something went wrong during closing of node RPC connection")
            e.printStackTrace()
        }
    }

    /**
     * Return node info for all other non-notary participants on the network.
     */
    fun getNetworkParticipants(): List<NodeInfo> {
        val notaryIdentities = nodeRPCConnection.proxy.networkParameters.notaries.map { it.identity }
        return nodeRPCConnection.proxy.networkMapSnapshot().filter {
            it != ourIdentity && it.legalIdentities.none { identity -> notaryIdentities.contains(identity) }
        }
    }

    fun createAndListItem(description: String, lastPrice: Double, expiry: Instant) {
        flowExecutorPool.submit {
            nodeRPCConnection.proxy
                    .startFlow({ desc, price, expires -> CreateAndListItemFlow(desc, price, expires) }, description, lastPrice, expiry)
                    .returnValue
                    .getOrThrow()
        }
    }

    fun getListedItems(): List<BidState> {
        return nodeRPCConnection.proxy
                    .startFlow { GetListedItemsFlow.Initiator() }
                    .returnValue
                    .getOrThrow(Duration.ofSeconds(20))
    }

    fun getMyItems(): List<ItemData> {
        val listedItems = getListedItems()
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return nodeRPCConnection.proxy.vaultQueryByCriteria(criteria, ItemState::class.java).states.map {
            it.state.data
        }.filter{
            it.owner == ourIdentity.legalIdentities.last()
        }.map {
            val listed = listedItems.any { bidState -> bidState.itemLinearId == it.linearId }
            ItemData(it, listed)
        }
    }

    fun getMyBids(): List<BidData> {
        val ourParty = ourIdentity.legalIdentities.last()
        val listedItems = getListedItems()
        return nodeRPCConnection.proxy.vaultQuery(BidState::class.java).states.map {
            it.state.data
        }.filter {
            it.issuer != ourParty && it.lastSuccessfulBidder == ourParty
        }.map {
            BidData(it, BidStatus.UNKNOWN)
        }
    }

    fun bidOnItem(bidState: BidState, offerPrice: Double) {
        // Retrieve bid state so it is in our vault
        nodeRPCConnection.proxy.startFlow({
            issuer, itemId, offer -> PlaceBidFirstFlow(issuer, itemId, offer)
        }, bidState.issuer, bidState.linearId, offerPrice).returnValue.getOrThrow(Duration.ofSeconds(20))

        // Actually make the bid
        nodeRPCConnection.proxy.startFlow({
            issuer, itemId, offer -> PlaceBidSecondFlow(issuer, itemId, offer)
        }, bidState.issuer, bidState.linearId, offerPrice).returnValue.getOrThrow(Duration.ofSeconds(20))
    }

    private fun initialiseNodeRPCConnection(host: String, port: Int, username: String, password: String) {
        nodeRPCConnection = NodeRPCConnection(
                host = host,
                rpcPort = port,
                username = username,
                password = password
        ).also { it.initialiseNodeRPCConnection() }
    }
}
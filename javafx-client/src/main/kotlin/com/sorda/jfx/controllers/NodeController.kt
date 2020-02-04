package com.sorda.jfx.controllers

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetListedItemsFlow
import com.sorda.jfx.NodeRPCConnection
import com.sorda.states.BidState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.startFlow
import net.corda.core.node.NodeInfo
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

    fun bidOnItem(bidStateId: UniqueIdentifier, amountToBid: Double) {
        // TODO
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
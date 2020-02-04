package com.sorda.jfx

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort

/**
 * Wraps a node RPC proxy.
 *
 * The RPC proxy is configured based on the properties in `application.properties`.
 *
 * @property host The host of the node we are connecting to.
 * @property username The username for logging into the RPC client.
 * @property password The password for logging into the RPC client.
 * @property rpcPort The RPC port of the node we are connecting to.
 */
open class NodeRPCConnection(
        private val host: String,
        private val username: String,
        private val password: String,
        private val rpcPort: Int
){

    lateinit var rpcConnection: CordaRPCConnection
        private set
    lateinit var proxy: CordaRPCOps
        private set

    fun initialiseNodeRPCConnection() {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val rpcClient = CordaRPCClient(rpcAddress)
        val rpcConnection = rpcClient.start(username, password)
        proxy = rpcConnection.proxy
    }

    fun close() {
        rpcConnection.notifyServerAndClose()
    }
}
package com.sorda.webserver


import com.sorda.flows.session.ListItemFlow
import com.sorda.flows.tokens.IssueSordaTokens
import net.corda.core.messaging.startFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy
    private val nodeInfo = proxy.nodeInfo()
    private val myIdentity = nodeInfo.legalIdentities.first()
    private val myLegalName = myIdentity.name

    /**
     * POST http://{host_api}/tokens
     */
    @CrossOrigin(origins = [Whitelist.w1, Whitelist.w2])
    @PostMapping(value = ["/tokens", "/tokens/{account_name}"], headers = ["Content-Type=application/json"])
    fun addTokens (@RequestBody addTokens: AddTokens,
                   @PathVariable(value = "account_name", required = false) account_name: String?)
        : ResponseEntity<Map<String, Any>> = try {

        val (issuanceResultTx : SignedTransaction, party) = if (account_name == null) {
            // add tokens to the node
            val tx = proxy.startFlow(::IssueSordaTokens, addTokens.amount).returnValue.getOrThrow()
            Pair (tx, myIdentity.name.toString())
        } else {
//            val accountInfoList = proxy.startFlow (::AccountInfoByName, account_name).returnValue.getOrThrow()
//            val accountInfo = accountInfoList.single { it.state.data.host == myIdentity }
//            val uuid = accountInfo.state.data.identifier
            val tx = proxy.startFlow(::IssueSordaTokens, addTokens.amount).returnValue.getOrThrow()
            Pair (tx, myIdentity.name.toString())
        }

        ResponseEntity.status(HttpStatus.OK).body(mapOf("amount" to  addTokens.amount,
                "tx_id" to issuanceResultTx.id.toString(),
                "message" to "Successfully added tokens to party=$party."))
    } catch (ex: Throwable) {
        logger.error(ex.message.toString())
        ResponseEntity.status(BAD_REQUEST).body(mapOf("message" to ex.localizedMessage.toString()))
    }

    /**
     * POST http://{host_api}/list_new_item
     * Description: Add new item on the CORDA node.
     */
    @CrossOrigin(origins = [Whitelist.w1, Whitelist.w2])
    @PostMapping(value = ["/list_new_item"], headers = ["Content-Type=application/json"])
    fun listNewItem (@RequestBody newItem: NewItem): ResponseEntity<Map<String, Any>> = try {
        Controller.logger.info("Creating an account on node ", myIdentity)
        val txHash= proxy.startFlow(::ListItemFlow, newItem.description).returnValue.getOrThrow()

        ResponseEntity.status(HttpStatus.CREATED).body(mapOf(
                "hash" to txHash,
                "message" to "New item with description ${newItem.description} created on node $myLegalName"))
    }
    catch (ex: Throwable) {
        Controller.logger.error(ex.message.toString())
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf("error" to ex.localizedMessage.toString()))
    }

}

data class NewItem constructor (
    val description: String
)

data class AddTokens constructor(
    val amount: Double
)


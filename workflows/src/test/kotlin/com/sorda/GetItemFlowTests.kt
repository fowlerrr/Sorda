package com.sorda

import com.sorda.flows.session.CreateAndListItemFlow
import com.sorda.flows.session.GetItemsFlow
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetItemFlowTests {

    private var notaryName = "O=Notary,L=London,C=GB"
    private var minimumPlatformVersion = 4
    private val user = User("mark", "dadada", setOf(Permissions.all()))

    lateinit var mockNetwork : MockNetwork
    lateinit var nodeA : StartedMockNode
    lateinit var nodeB : StartedMockNode

    @Before
    fun setup () {
    }

    @After
    fun tearDown () {
    }

    @Test
    fun `get items from all nodes`() {
        driver(DriverParameters(
                startNodesInProcess = true
            )
        ) {
            val aliceNode = startNode(providedName = ALICE_NAME, rpcUsers = listOf(user)).getOrThrow()
            val bobNode = startNode(providedName = BOB_NAME, rpcUsers = listOf(user)).getOrThrow()

            val alice = aliceNode.nodeInfo.identityAndCertFromX500Name(ALICE_NAME)
            val bob = bobNode.nodeInfo.identityAndCertFromX500Name(BOB_NAME)

            aliceNode.rpc.startFlowDynamic(
                CreateAndListItemFlow::class.java,
                "red bike",
                10.0,
                Instant.now().plus(10, ChronoUnit.MINUTES)
            )
            aliceNode.rpc.startFlowDynamic(
                CreateAndListItemFlow::class.java,
                "blue bike",
                10.0,
                Instant.now().plus(10, ChronoUnit.MINUTES)
            )

            val items = aliceNode.rpc.startFlowDynamic(GetItemsFlow::class.java).returnValue.getOrThrow()
            assertThat(items.size).isEqualTo(2)
        }
    }

}


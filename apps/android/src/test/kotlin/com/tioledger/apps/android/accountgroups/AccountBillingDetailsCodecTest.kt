package com.tioledger.apps.android.accountgroups

import com.tioledger.ui.more.AccountBillingDetails
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPersistence
import com.tioledger.ui.more.AccountGroupStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class AccountBillingDetailsCodecTest {
    @Test
    fun billingDetailsSurviveStoreRecreationRoundTrip() {
        val expected =
            mapOf(
                "card|one" to
                    AccountBillingDetails(
                        billingDay = 15,
                        paymentDueDay = 27,
                        paymentAccountId = "bank|main",
                        description = "Travel card\nprimary",
                    ),
            )

        val persistence = EncodedBillingPersistence()
        val firstStore = AccountGroupStore(persistence = persistence)
        assertTrue(firstStore.saveBillingDetails("card|one", expected.getValue("card|one")))
        val persistedValue = persistence.billingDetails
        val reloadedAfterRecreation = AccountGroupStore(persistence = persistence).billingDetailsForAccount("card|one")

        assertTrue(persistedValue.contains("MTU="))
        assertEquals(expected.getValue("card|one"), reloadedAfterRecreation)
    }

    @Test
    fun legacyBillingDetailsWithoutOptionalFieldsStillLoad() {
        val encoded = listOf("card-legacy", "15", "27").joinToString("|") { encode(it) }

        assertEquals(
            mapOf("card-legacy" to AccountBillingDetails(15, 27, null, "")),
            AccountBillingDetailsCodec.decode(encoded),
        )
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}

private class EncodedBillingPersistence : AccountGroupPersistence {
    var billingDetails = ""

    override fun load(): List<AccountGroupEntry>? = null

    override fun save(groups: List<AccountGroupEntry>): Boolean = true

    override fun loadAccountBillingDetails(): Map<String, AccountBillingDetails> {
        return AccountBillingDetailsCodec.decode(billingDetails)
    }

    override fun saveAccountBillingDetails(details: Map<String, AccountBillingDetails>): Boolean {
        billingDetails = AccountBillingDetailsCodec.encode(details)
        return true
    }
}

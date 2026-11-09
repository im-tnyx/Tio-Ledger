package com.tioledger.apps.android.accountgroups

import android.content.Context
import com.tioledger.ui.more.AccountBillingDetails
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPersistence
import com.tioledger.ui.more.AccountGroupPolicy
import java.util.Base64

class SharedPreferencesAccountGroupPersistence(
    context: Context,
) : AccountGroupPersistence {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): List<AccountGroupEntry>? {
        val encoded = preferences.getString(GROUPS_KEY, null) ?: return null
        return encoded.lineSequence().mapNotNull(::decode).toList().ifEmpty { null }
    }

    override fun save(groups: List<AccountGroupEntry>): Boolean =
        preferences.edit().putString(GROUPS_KEY, groups.joinToString("\n", transform = ::encode)).commit()

    override fun loadAccountGroupIds(): Map<String, String> {
        val encoded = preferences.getString(ACCOUNT_GROUP_IDS_KEY, null) ?: return emptyMap()
        return encoded.lineSequence().mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != ACCOUNT_GROUP_ID_FIELD_COUNT) {
                null
            } else {
                decodeField(fields[ACCOUNT_ID_INDEX]) to decodeField(fields[GROUP_ID_INDEX])
            }
        }.toMap()
    }

    override fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean {
        val encoded =
            accountGroupIds.entries.joinToString("\n") { (accountId, groupId) ->
                listOf(accountId, groupId).joinToString(FIELD_SEPARATOR, transform = ::encodeField)
            }
        return preferences.edit().putString(ACCOUNT_GROUP_IDS_KEY, encoded).commit()
    }

    override fun loadAccountBillingDetails(): Map<String, AccountBillingDetails> {
        val encoded = preferences.getString(ACCOUNT_BILLING_DETAILS_KEY, null) ?: return emptyMap()
        return AccountBillingDetailsCodec.decode(encoded)
    }

    override fun saveAccountBillingDetails(details: Map<String, AccountBillingDetails>): Boolean {
        return preferences
            .edit()
            .putString(ACCOUNT_BILLING_DETAILS_KEY, AccountBillingDetailsCodec.encode(details))
            .commit()
    }

    private fun encode(group: AccountGroupEntry): String =
        listOf(
            group.id,
            group.title,
            group.subtitle.orEmpty(),
            group.policy.name,
            group.isPermanent.toString(),
        ).joinToString(FIELD_SEPARATOR, transform = ::encodeField)

    private fun decode(value: String): AccountGroupEntry? {
        val fields = value.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null
        val policy = fields[POLICY_INDEX].let { name -> AccountGroupPolicy.entries.firstOrNull { it.name == name } }
        return if (policy == null) {
            null
        } else {
            AccountGroupEntry(
                id = decodeField(fields[ID_INDEX]),
                title = decodeField(fields[TITLE_INDEX]),
                subtitle = decodeField(fields[SUBTITLE_INDEX]).ifBlank { null },
                policy = policy,
                isPermanent = decodeField(fields[PERMANENT_INDEX]).toBoolean(),
            )
        }
    }

    private fun encodeField(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodeField(value: String): String =
        runCatching { String(Base64.getDecoder().decode(value), Charsets.UTF_8) }.getOrDefault("")

    private companion object {
        const val PREFERENCES_NAME = "tio_ledger_account_groups"
        const val GROUPS_KEY = "groups"
        const val ACCOUNT_GROUP_IDS_KEY = "account_group_ids"
        const val ACCOUNT_BILLING_DETAILS_KEY = "account_billing_details"
        const val FIELD_SEPARATOR = "|"
        const val FIELD_COUNT = 5
        const val ACCOUNT_GROUP_ID_FIELD_COUNT = 2
        const val ID_INDEX = 0
        const val TITLE_INDEX = 1
        const val SUBTITLE_INDEX = 2
        const val POLICY_INDEX = 3
        const val PERMANENT_INDEX = 4
        const val ACCOUNT_ID_INDEX = 0
        const val GROUP_ID_INDEX = 1
    }
}

private const val MIN_BILLING_DAY = 1
private const val MAX_BILLING_DAY = 31

private fun isValidBillingDay(day: Int): Boolean = day in MIN_BILLING_DAY..MAX_BILLING_DAY

internal object AccountBillingDetailsCodec {
    private const val FIELD_SEPARATOR = "|"
    private const val ACCOUNT_ID_INDEX = 0
    private const val BILLING_DAY_INDEX = 1
    private const val PAYMENT_DUE_DAY_INDEX = 2
    private const val PAYMENT_ACCOUNT_ID_INDEX = 3
    private const val DESCRIPTION_INDEX = 4
    private const val BILLING_DETAILS_FIELD_COUNT = 3
    private const val EXTENDED_BILLING_DETAILS_FIELD_COUNT = 5

    fun encode(details: Map<String, AccountBillingDetails>): String =
        details.entries.joinToString("\n") { (accountId, detail) ->
            listOf(
                accountId,
                detail.billingDay.toString(),
                detail.paymentDueDay.toString(),
                detail.paymentAccountId.orEmpty(),
                detail.description,
            ).joinToString(FIELD_SEPARATOR, transform = ::encodeField)
        }

    fun decode(encoded: String): Map<String, AccountBillingDetails> =
        encoded.lineSequence().mapNotNull { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != BILLING_DETAILS_FIELD_COUNT && fields.size != EXTENDED_BILLING_DETAILS_FIELD_COUNT) {
                null
            } else {
                val billingDay =
                    decodeField(fields[BILLING_DAY_INDEX]).toIntOrNull()?.takeIf(::isValidBillingDay)
                        ?: return@mapNotNull null
                val paymentDueDay =
                    decodeField(fields[PAYMENT_DUE_DAY_INDEX]).toIntOrNull()?.takeIf(::isValidBillingDay)
                        ?: return@mapNotNull null
                decodeField(fields[ACCOUNT_ID_INDEX]) to
                    AccountBillingDetails(
                        billingDay = billingDay,
                        paymentDueDay = paymentDueDay,
                        paymentAccountId =
                            fields.getOrNull(PAYMENT_ACCOUNT_ID_INDEX)?.let(::decodeField)?.ifBlank { null },
                        description = fields.getOrNull(DESCRIPTION_INDEX)?.let(::decodeField).orEmpty(),
                    )
            }
        }.toMap()

    private fun encodeField(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
    }

    private fun decodeField(value: String): String =
        runCatching { String(Base64.getDecoder().decode(value), Charsets.UTF_8) }.getOrDefault("")
}

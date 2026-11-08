package com.tioledger.apps.android.accountgroups

import android.content.Context
import android.util.Base64
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPersistence
import com.tioledger.ui.more.AccountGroupPolicy

class SharedPreferencesAccountGroupPersistence(
    context: Context,
) : AccountGroupPersistence {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): List<AccountGroupEntry>? {
        val encoded = preferences.getString(GROUPS_KEY, null) ?: return null
        return encoded.lineSequence().mapNotNull(::decode).toList().ifEmpty { null }
    }

    override fun save(groups: List<AccountGroupEntry>) {
        preferences.edit().putString(GROUPS_KEY, groups.joinToString("\n", transform = ::encode)).commit()
    }

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

    private fun encodeField(value: String): String =
        Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )

    private fun decodeField(value: String): String =
        runCatching { String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8) }.getOrDefault("")

    private companion object {
        const val PREFERENCES_NAME = "tio_ledger_account_groups"
        const val GROUPS_KEY = "groups"
        const val ACCOUNT_GROUP_IDS_KEY = "account_group_ids"
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

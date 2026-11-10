package com.tioledger.apps.android.accountgroups

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPolicy
import com.tioledger.ui.more.AccountGroupRemovalResult
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.DEFAULT_ACCOUNT_GROUPS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

private typealias PreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener

class SharedPreferencesAccountGroupPersistenceTest {
    @Test
    fun defaultAndCustomGroupsPoliciesOrderAndAssignmentsSurvivePersistenceRecreation() {
        val context = TestContext()
        val firstPersistence = SharedPreferencesAccountGroupPersistence(context)
        val firstStore = AccountGroupStore(persistence = firstPersistence)
        val custom = firstStore.addGroup("Travel", AccountGroupPolicy.OTHER)!!
        firstStore.reorder(3, 1)
        assertTrue(firstStore.assignAccount("wallet-1", custom.id))

        val reloadedPersistence = SharedPreferencesAccountGroupPersistence(context)
        val reloadedGroups = reloadedPersistence.load()
        val secondStore = AccountGroupStore(persistence = reloadedPersistence)

        assertEquals(listOf("cash", custom.id, "accounts", "credit_card"), reloadedGroups?.map { it.id })
        assertEquals(DEFAULT_ACCOUNT_GROUPS.first(), reloadedGroups?.first())
        assertEquals(AccountGroupPolicy.OTHER, reloadedGroups?.get(1)?.policy)
        assertEquals(AccountGroupPolicy.OTHER.accountType, secondStore.groupForId(custom.id)?.policy?.accountType)
        assertEquals(custom.id, secondStore.groupIdForAccount("wallet-1"))
    }

    @Test
    fun assignedCustomGroupCannotBeDeletedAfterPersistenceRecreation() {
        val context = TestContext()
        val firstStore = AccountGroupStore(persistence = SharedPreferencesAccountGroupPersistence(context))
        val custom = firstStore.addGroup("Savings", AccountGroupPolicy.BANK)!!
        assertTrue(firstStore.assignAccount("bank-1", custom.id))

        val recreatedStore = AccountGroupStore(persistence = SharedPreferencesAccountGroupPersistence(context))

        assertEquals(AccountGroupRemovalResult.GroupContainsAccounts, recreatedStore.removeGroup(custom.id))
        assertEquals(custom.id, recreatedStore.groupIdForAccount("bank-1"))
    }

    @Test
    fun invalidStoredPolicyAndMalformedRecordDoNotCorruptValidGroupsOrRewriteStoredData() {
        val context = TestContext()
        val preferences = context.getSharedPreferences("tio_ledger_account_groups", Context.MODE_PRIVATE)
        val valid = encodeFields("valid", "Valid Group", "", AccountGroupPolicy.BANK.name, "false")
        val legacyCase = encodeFields("legacy-case", "Legacy Case", "", AccountGroupPolicy.CASH.name, "TRUE")
        val invalidPolicy = encodeFields("bad-policy", "Bad Policy", "", "NOT_A_POLICY", "false")
        val malformed = "not|a|complete|group"
        val original = listOf(valid, legacyCase, invalidPolicy, malformed).joinToString("\n")
        preferences.edit().putString("groups", original).commit()

        val loaded = SharedPreferencesAccountGroupPersistence(context).load()

        assertEquals(
            listOf(
                AccountGroupEntry("valid", "Valid Group", null, AccountGroupPolicy.BANK, false),
                AccountGroupEntry("legacy-case", "Legacy Case", null, AccountGroupPolicy.CASH, true),
            ),
            loaded,
        )
        assertEquals(original, preferences.getString("groups", null))
    }

    @Test
    fun failedSharedPreferencesCommitDoesNotReportSuccessfulGroupCreation() {
        val persistence = SharedPreferencesAccountGroupPersistence(TestContext(commitSucceeds = false))
        val store = AccountGroupStore(persistence = persistence)

        assertFalse(persistence.save(DEFAULT_ACCOUNT_GROUPS))
        assertNull(store.addGroup("Must not appear", AccountGroupPolicy.BANK))
        assertEquals(DEFAULT_ACCOUNT_GROUPS, store.groups.value)
    }

    private fun encodeFields(vararg values: String): String =
        values.joinToString("|") { value ->
            Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
        }
}

private class TestContext(
    private val commitSucceeds: Boolean = true,
) : ContextWrapper(null) {
    private val preferences = TestSharedPreferences(commitSucceeds)

    override fun getSharedPreferences(
        name: String,
        mode: Int,
    ): SharedPreferences = preferences
}

private class TestSharedPreferences(
    private val commitSucceeds: Boolean,
) : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(
        key: String,
        defValue: String?,
    ): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(
        key: String,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(
        key: String,
        defValue: Int,
    ): Int = values[key] as? Int ?: defValue

    override fun getLong(
        key: String,
        defValue: Long,
    ): Long = values[key] as? Long ?: defValue

    override fun getFloat(
        key: String,
        defValue: Float,
    ): Float = values[key] as? Float ?: defValue

    override fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = key in values

    override fun edit(): SharedPreferences.Editor = TestEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: PreferenceChangeListener) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(listener: PreferenceChangeListener) = Unit

    private inner class TestEditor : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var shouldClear = false

        override fun putString(
            key: String,
            value: String?,
        ): SharedPreferences.Editor = put(key, value)

        override fun putStringSet(
            key: String,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = put(key, values?.toSet())

        override fun putInt(
            key: String,
            value: Int,
        ): SharedPreferences.Editor = put(key, value)

        override fun putLong(
            key: String,
            value: Long,
        ): SharedPreferences.Editor = put(key, value)

        override fun putFloat(
            key: String,
            value: Float,
        ): SharedPreferences.Editor = put(key, value)

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): SharedPreferences.Editor = put(key, value)

        override fun remove(key: String): SharedPreferences.Editor =
            apply {
                removals += key
            }

        override fun clear(): SharedPreferences.Editor = apply { shouldClear = true }

        override fun commit(): Boolean {
            if (!commitSucceeds) return false
            if (shouldClear) values.clear()
            removals.forEach(values::remove)
            values.putAll(updates)
            return true
        }

        override fun apply() {
            commit()
        }

        private fun put(
            key: String,
            value: Any?,
        ): SharedPreferences.Editor =
            apply {
                updates[key] = value
                removals -= key
            }
    }
}

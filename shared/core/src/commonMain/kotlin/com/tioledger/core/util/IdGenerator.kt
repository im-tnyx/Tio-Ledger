package com.tioledger.core.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface IdGenerator {
    fun nextId(): String
}

class UuidGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun nextId(): String {
        return Uuid.random().toString()
    }
}

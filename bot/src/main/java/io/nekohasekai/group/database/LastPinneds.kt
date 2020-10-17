package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.id.IdTable

object LastPinneds : IdTable<Long>("last_pinned") {

    override val id = long("chat_id").entityId()

    val pinnedMessage = long("pinned_message")

}
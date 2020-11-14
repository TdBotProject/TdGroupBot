package io.nekohasekai.group.database

import io.nekohasekai.ktlib.db.DatabaseDispatcher
import io.nekohasekai.ktlib.db.TwoIndexCacheMap
import org.jetbrains.exposed.sql.Table

object UserFirstMessage : Table("user_no_message") {

    val chatId = integer("group_id")
    val userId = integer("user_id")
    val firstMessage = integer("first_message").nullable()

    override val primaryKey = PrimaryKey(chatId, userId)

    class NoMessageMap(database: DatabaseDispatcher) : TwoIndexCacheMap<Int, Int, Int?>(database, UserFirstMessage, chatId, userId, firstMessage)

}
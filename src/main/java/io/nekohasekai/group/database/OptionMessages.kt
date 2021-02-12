package io.nekohasekai.group.database

import io.nekohasekai.ktlib.db.DatabaseDispatcher
import io.nekohasekai.ktlib.db.TwoIndexCacheMap
import org.jetbrains.exposed.sql.Table

object OptionMessages : Table("option_messages") {

    val userId = integer("user_id")
    val chatId = long("chat_id")
    val optionMessage = long("option_message")

    val userIndex = index(true, userId, chatId)
    override val primaryKey = PrimaryKey(userId, optionMessage)

    class MessagesMap(database: DatabaseDispatcher) : TwoIndexCacheMap<Int, Long, Long>(database, OptionMessages, userId, chatId, optionMessage)
    class ChatsMap(database: DatabaseDispatcher) : TwoIndexCacheMap<Int, Long, Long>(database, OptionMessages, userId, optionMessage, chatId)

}
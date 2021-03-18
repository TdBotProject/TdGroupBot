package io.nekohasekai.group.exts

import io.nekohasekai.group.database.OptionMessages
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

fun TdHandler.getOptionsChat(userId: Int, messageId: Long): Long? = database {

    OptionMessages
        .select {
            (OptionMessages.userId eq userId) and (OptionMessages.optionMessage eq messageId)
        }
        .adjustSlice { slice(OptionMessages.chatId) }
        .firstOrNull()?.let { it[OptionMessages.chatId] }

}

fun TdHandler.getOptionsMessage(chatId: Long, userId: Int): Long? = database {

    OptionMessages
        .select {
            (OptionMessages.chatId eq chatId) and (OptionMessages.userId eq userId)
        }
        .adjustSlice { slice(OptionMessages.optionMessage) }
        .firstOrNull()?.let { it[OptionMessages.optionMessage] }

}

fun TdHandler.removeOptionsMessage(chatId: Long, userId: Int): Boolean = database.write {

    OptionMessages
        .deleteWhere {
            (OptionMessages.chatId eq chatId) and (OptionMessages.userId eq userId)
        } > 0

}

fun TdHandler.writeOptionsMessage(chatId: Long, userId: Int, messageId: Long) = database.write {

    OptionMessages
        .insert {
            it[OptionMessages.chatId] = chatId
            it[OptionMessages.userId] = userId
            it[OptionMessages.optionMessage] = messageId
        }

}
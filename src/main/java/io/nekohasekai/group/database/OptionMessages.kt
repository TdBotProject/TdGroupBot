package io.nekohasekai.group.database

import org.jetbrains.exposed.sql.Table

object OptionMessages : Table("option_messages") {

    val userId = integer("user_id")
    val chatId = long("chat_id")
    val optionMessage = long("option_message")

    val userIndex = index(true, userId, chatId)
    override val primaryKey = PrimaryKey(userId, optionMessage)

}
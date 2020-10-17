package io.nekohasekai.group.database

import org.jetbrains.exposed.sql.Table

object OptionMessages : Table("option_messaged") {

    val userId = integer("user_id")
    val chatId = long("chat_id")
    val optionMessage = long("option_message")

    init {

        index(true, userId, chatId)

        uniqueIndex("admin")

    }

    override val primaryKey = PrimaryKey(userId, optionMessage)

}
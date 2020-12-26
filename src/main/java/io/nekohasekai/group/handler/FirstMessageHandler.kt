package io.nekohasekai.group.handler

import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.database.UserFirstMessage
import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.Seconds
import io.nekohasekai.ktlib.td.extensions.isServiceMessage
import io.nekohasekai.ktlib.td.extensions.toSupergroupId
import io.nekohasekai.ktlib.td.utils.deleteDelay
import io.nekohasekai.ktlib.td.utils.isChatAdmin
import io.nekohasekai.ktlib.td.utils.make
import td.TdApi

class FirstMessageHandler : TdHandler() {

    lateinit var userFirstMessageMap: UserFirstMessage.FirstMessageMap

    override fun onLoad() {
        userFirstMessageMap = UserFirstMessage.FirstMessageMap(database)

        initFunction("_reset_fm")
    }

    override suspend fun onFunction(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        function: String,
        param: String,
        params: Array<String>,
        originParams: Array<String>
    ) {

        userFirstMessageMap.fetch(chatId.toSupergroupId to userId).set(null)
        sudo make "OK" onSuccess deleteDelay(message, timeMs = 1 * Seconds) replyTo message
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (userId == 0 || isChatAdmin(chatId, userId) || message.isServiceMessage) return

        val config = global.groupConfigs.fetch(chatId).value ?: return
        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) true else {
            message.date - userFirstMessage.value!! < 5
        }.takeIf { it } ?: return

        if (sudo.handlers.filterIsInstance<Interface>().all {
                !it.onFirstMessage(userId, chatId, message, config)
            }) {
            userFirstMessage.set(message.date)
        }

    }

    interface Interface {
        suspend fun onFirstMessage(
            userId: Int,
            chatId: Long,
            message: TdApi.Message,
            config: GroupConfig
        ): Boolean
    }


}
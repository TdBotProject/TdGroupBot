package io.nekohasekai.group.handler

import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.isServiceMessage
import io.nekohasekai.ktlib.td.utils.delete
import td.TdApi

class DeleteServiceMessagesHandler : TdHandler() {

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (!message.isServiceMessage) return
        val action = (global.groupConfigs.fetch(chatId).value?.takeIf { it.deleteServiceMessages != 0 }
            ?: return).deleteServiceMessages

        if (action == 1 && message.content is TdApi.MessageChatJoinByLink) {
            delete(message)
            finishEvent()
        } else if (action == 2) when (message.content) {
            is TdApi.MessageChatAddMembers,
            is TdApi.MessageChatJoinByLink,
            is TdApi.MessageChatDeleteMember -> {
                delete(message)
                finishEvent()
            }
        } else {
            delete(message)
            finishEvent()
        }

    }

}
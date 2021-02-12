package io.nekohasekai.group.handler

import io.nekohasekai.group.MP_DEL_WARN
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.removeNonASCII
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChatMember
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.core.raw.setChatMemberStatus
import io.nekohasekai.ktlib.td.extensions.displayName
import io.nekohasekai.ktlib.td.extensions.htmlInlineMention
import io.nekohasekai.ktlib.td.extensions.isMember
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.delayDelete
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.makeHtml
import io.nekohasekai.ktlib.td.utils.muteMember
import td.TdApi

class MemberPolicyHandler : TdHandler() {

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (message.content !is TdApi.MessageChatAddMembers &&
            message.content !is TdApi.MessageChatJoinByLink &&
            message.replyInChatId == 0L
        ) return

        val action = (global.groupConfigs.fetch(chatId).value?.takeIf { it.memberPolicy != 0 } ?: return).memberPolicy

        if (action == 1) {
            val content = message.content
            if (content is TdApi.MessageChatJoinByLink) {
                sudo delete message
                setChatMemberStatus(
                    chatId,
                    userId,
                    TdApi.ChatMemberStatusBanned((System.currentTimeMillis() / 1000).toInt())
                )
            } else if (content is TdApi.MessageChatAddMembers) {
                sudo delete message
                for (memberUserId in content.memberUserIds) {
                    setChatMemberStatus(
                        chatId,
                        memberUserId,
                        TdApi.ChatMemberStatusBanned((System.currentTimeMillis() / 1000).toInt())
                    )
                }
            }
        } else if (!getChatMember(chatId, userId).isMember) {
            muteMember(chatId, userId, (System.currentTimeMillis() / 1000).toInt() + 40)
            sudo makeHtml localeFor(
                chatId,
                userId
            ).MP_DEL_WARN.input(
                getUser(userId).displayName
                    .removeNonASCII()
                    .htmlInlineMention(userId)) onSuccess {
                sudo delete message
                delayDelete(it, timeMs = 15000L)
            } replyTo message
        }

    }

}
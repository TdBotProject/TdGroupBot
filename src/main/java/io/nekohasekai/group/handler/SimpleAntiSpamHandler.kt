package io.nekohasekai.group.handler

import cn.hutool.core.util.CharUtil
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.mkLog
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.reportSupergroupSpam
import io.nekohasekai.ktlib.td.extensions.displayName
import io.nekohasekai.ktlib.td.extensions.textOrCaption
import io.nekohasekai.ktlib.td.extensions.toSupergroupId
import io.nekohasekai.ktlib.td.utils.banChatMember
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.kickMember
import io.nekohasekai.ktlib.td.utils.muteMember
import td.TdApi

class SimpleAntiSpamHandler : TdHandler(), FirstMessageHandler.Interface {

    val log = mkLog("AntiSpam")

    val virusAbs = ".*\\.(cmd|bat|exe|ps1|rar|zip|lha|lzh)".toRegex()

    override suspend fun onFirstMessage(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        config: GroupConfig
    ): Boolean {

        val action = config.simpleAs.takeIf { it > 0 } ?: return false
        val content = message.content

        suspend fun exec(): Nothing {
            when (action) {
                1 -> muteMember(chatId, userId)
                2 -> banChatMember(chatId, userId)
                3 -> kickMember(chatId, userId)
            }
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                reportSupergroupSpam(chatId.toSupergroupId, userId, longArrayOf(message.id))
                deleteChatMessagesFromUser(chatId, userId)
            } else {
                sudo delete message
            }
            finishEvent()
        }

        if (content is TdApi.MessageDocument) {
            if (content.document.fileName.matches(virusAbs)) {
                log.trace("virus like file detected")
                exec()
            } else {
                log.trace("else file: ${content.document.fileName}")
            }
        } else if (content is TdApi.MessageContact) {
            log.trace("content detected: ${content.contact.displayName}")
            exec()
        } else if (message.forwardInfo != null &&
            (message.textOrCaption == null ||
                    message.textOrCaption!!.count { CharUtil.isEmoji(it) } > 2)
        ) {
            log.trace("forward detected")
            exec()
        }

        val isSafe = content is TdApi.MessageSticker ||
                content is TdApi.MessageText &&
                content.text.entities.isEmpty() &&
                content.text.text.count { CharUtil.isEmoji(it) } < 5

        if (!isSafe) {
            log.trace("Unsafe message")
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                deleteChatMessagesFromUser(chatId, userId)
            } else {
                sudo delete message
            }
            return true
        }

        return false

    }

}
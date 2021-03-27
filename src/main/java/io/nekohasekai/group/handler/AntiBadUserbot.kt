package io.nekohasekai.group.handler

import cn.hutool.cache.impl.LFUCache
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.postLog
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.getMessageOrNull
import io.nekohasekai.ktlib.td.core.raw.reportSupergroupSpam
import io.nekohasekai.ktlib.td.extensions.senderUserId
import io.nekohasekai.ktlib.td.extensions.textOrCaption
import io.nekohasekai.ktlib.td.extensions.toSupergroupId
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi
import java.io.File

class AntiBadUserbot : TdHandler() {

    companion object {

        val userbotCommands = File("src/main/resources/userbot_commands.txt").readLines()

    }

    val commandDict = LFUCache<Long, TdApi.Message>(-1, 10 * 1000L)

    override suspend fun gc() {
        super.gc()
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (userId == 0 || isChatAdmin(chatId, userId)) return
        val text = message.textOrCaption ?: return
        val config = global.groupConfigs.fetch(chatId).value ?: return
        val action = config.simpleAs.takeIf { it > 0 } ?: return

        suspend fun exec(): Nothing {
            SimpleAntiSpamHandler.spamDict.put(userId, Unit)

            when (action) {
                1 -> muteMember(chatId, userId)
                2 -> banChatMember(chatId, userId)
                3 -> kickMember(chatId, userId)
            }

            sudo delete message
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                reportSupergroupSpam(chatId.toSupergroupId, userId, longArrayOf(message.id))
                deleteChatMessagesFromUser(chatId, userId)
            }
            finishEvent()
        }

        if (userbotCommands.contains(text)) {
            postLog(message, "Type", "Bad Userbot")
            exec()
        } else if (text.startsWith("-")) {
            commandDict.put(chatId + message.id, message)
        }

    }

    override suspend fun onMessageContent(chatId: Long, messageId: Long, newContent: TdApi.MessageContent) {

        val message = getMessageOrNull(chatId, messageId) ?: return
        val userId = message.senderUserId
        if (userId == 0 || isChatAdmin(chatId, userId)) return
        val config = global.groupConfigs.fetch(chatId).value ?: return
        val action = config.simpleAs.takeIf { it > 0 } ?: return

        suspend fun exec(): Nothing {
            SimpleAntiSpamHandler.spamDict.put(userId, Unit)

            when (action) {
                1 -> muteMember(chatId, userId)
                2 -> banChatMember(chatId, userId)
                3 -> kickMember(chatId, userId)
            }

            sudo delete message
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                reportSupergroupSpam(chatId.toSupergroupId, userId, longArrayOf(message.id))
                deleteChatMessagesFromUser(chatId, userId)
            }
            finishEvent()
        }

        if (commandDict.containsKey(chatId + messageId)) {
            postLog(message, "Type", "Bad Userbot")
            exec()
        }


    }

}
package io.nekohasekai.group.handler

import cn.hutool.core.date.SystemClock
import cn.hutool.core.util.CharUtil
import io.nekohasekai.group.database.UserFirstMessage.FirstMessageMap
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.defaultLog
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.reportSupergroupSpam
import io.nekohasekai.ktlib.td.core.raw.setChatMemberStatus
import io.nekohasekai.ktlib.td.extensions.Minutes
import io.nekohasekai.ktlib.td.extensions.isServiceMessage
import io.nekohasekai.ktlib.td.extensions.textOrCaption
import io.nekohasekai.ktlib.td.extensions.toSupergroupId
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.fetchMessages
import io.nekohasekai.ktlib.td.utils.formatMessage
import io.nekohasekai.ktlib.td.utils.isChatAdmin
import td.TdApi

/**
 * 机器人检查
 * 由于诸多检查, 不应被删除服务消息覆盖.
 */
class SimpleAntiSpamHandler : TdHandler() {

    val virusAbs = ".*\\.(cmd|bat|exe|ps1|rar|zip)".toRegex()

    lateinit var userFirstMessageMap: FirstMessageMap

    override fun onLoad() {
        userFirstMessageMap = FirstMessageMap(database)

        initFunction("_del_me")
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
        if (!isUserAgentAvailable(chatId)) rejectFunction()

        userAgent!!.deleteChatMessagesFromUser(chatId, userId)
        userFirstMessageMap.fetch(chatId.toSupergroupId to userId).write(0)
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        val status = LinkedHashMap<String, String>()

        process(userId, chatId, message, status)

        if (status.isEmpty()) return

        defaultLog.debug(formatMessage(message) + " (${status.map { it.key + ": " + it.value }.joinToString(", ")})")

    }

    suspend fun process(userId: Int, chatId: Long, message: TdApi.Message, status: LinkedHashMap<String, String>) {

        if (userId == 0 || isChatAdmin(chatId, userId) || message.isServiceMessage) {
            return
        }
        val action = (global.groupConfigs.fetch(chatId).value?.takeIf { it.simpleAs != 0 } ?: return).simpleAs
        val content = message.content

        var isFirstMessage = true
        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) {
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                fetchMessages(
                    TdApi.SearchChatMessages(
                        chatId,
                        "",
                        TdApi.MessageSenderUser(userId),
                        0,
                        0,
                        100,
                        TdApi.SearchMessagesFilterEmpty(),
                        0
                    )
                ) { messages ->
                    val foundMsg = messages.find { !it.isServiceMessage && message.date - it.date > 3 * 60 }
                    if (foundMsg != null) {
                        isFirstMessage = false
                        userFirstMessage.set(foundMsg.date)
                        status["firstMessage"] = "found"
                    }
                    messages.isNotEmpty() && foundMsg == null
                }
            } else {
                status["firstMessage"] = "noAgent"
                isFirstMessage = true
            }
        } else {
            isFirstMessage = message.date - userFirstMessage.value!! < 3 * 60
            status["firstMessage"] = "record"
        }

        status["isFirstMessage"] = "$isFirstMessage"

        if (!isFirstMessage) return

        suspend fun exec(): Nothing {
            when (action) {
                1 -> setChatMemberStatus(
                    chatId, userId, TdApi.ChatMemberStatusRestricted(
                        true, 0, TdApi.ChatPermissions()
                    )
                )
                2 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusBanned())
                3 -> setChatMemberStatus(
                    chatId,
                    userId,
                    TdApi.ChatMemberStatusBanned(((SystemClock.now() + 1 * Minutes) / 1000).toInt())
                )
            }
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                reportSupergroupSpam(chatId.toSupergroupId, userId, longArrayOf(message.id))
                deleteChatMessagesFromUser(chatId, userId)
            } else {
                sudo delete message
            }
            userFirstMessage.set(0)
            finishEvent()
        }

        if (content is TdApi.MessageDocument) {
            if (content.document.fileName.matches(virusAbs)) {
                status["result"] = "bad_file"
                exec()
            }
        } else if (content is TdApi.MessageContact) {
            status["result"] = "ad_contact"
            exec()
        } else if (message.forwardInfo != null &&
            (message.textOrCaption == null ||
                    message.textOrCaption!!.count { CharUtil.isEmoji(it) } > 2)
        ) {
            status["result"] = "forward"
            exec()
        }

        val isSafe = (content is TdApi.MessageText) &&
                content.text.entities.none {
                    when (it.type) {
                        is TdApi.TextEntityTypeUrl,
                        is TdApi.TextEntityTypeTextUrl,
                        is TdApi.TextEntityTypeMention -> true
                        else -> false
                    }
                } &&
                content.text.text.count { CharUtil.isEmoji(it) } < 3

        if (!isSafe) {
            status["result"] = "delete"
            sudo delete message
        } else {
            status["result"] = "safe"
            userFirstMessage.set(message.date)
        }

    }

}
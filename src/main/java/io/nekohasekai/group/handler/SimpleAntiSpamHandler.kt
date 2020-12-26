package io.nekohasekai.group.handler

import cn.hutool.core.date.SystemClock
import cn.hutool.core.util.CharUtil
import io.nekohasekai.group.database.UserFirstMessage.FirstMessageMap
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.mkLog
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
import io.nekohasekai.ktlib.td.utils.isChatAdmin
import td.TdApi

/**
 * 机器人检查
 * 由于诸多检查, 不应被删除服务消息覆盖.
 */
class SimpleAntiSpamHandler : TdHandler() {

    val log = mkLog("AntiSpam")

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

        if (userId == 0 || isChatAdmin(chatId, userId) || message.isServiceMessage) {
            if (userId == 0) log.trace("userId == 0")
            else if (isChatAdmin(chatId, userId)) log.trace("skip admin message")
            return
        }

        val config = global.groupConfigs.fetch(chatId).value

        if (config == null || config.simpleAs == 0) {
            log.trace("disabled")
            return
        }

        val action = config.simpleAs
        val content = message.content

        var isFirstMessage = true
        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) {
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                log.trace("search message logs")
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
                        log.trace("message found")
                    }
                    messages.isNotEmpty() && foundMsg == null
                }
            } else {
                log.trace("agent not available")
                isFirstMessage = true
            }
        } else {
            isFirstMessage = message.date - userFirstMessage.value!! < 3 * 60
            log.trace("record found")
        }

        if (!isFirstMessage) {
            log.trace("skip non-first message")
            return
        }

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
                log.trace("bad file detected")
                exec()
            }
        } else if (content is TdApi.MessageContact) {
            log.trace("contact detected")
            exec()
        } else if (message.forwardInfo != null &&
            (message.textOrCaption == null ||
                    message.textOrCaption!!.count { CharUtil.isEmoji(it) } > 2)
        ) {
            log.trace("forwared detected")
            exec()
        }

        val isSafe = (content is TdApi.MessageText) &&
                content.text.entities.isEmpty() &&
                content.text.text.count { CharUtil.isEmoji(it) } < 2

        if (!isSafe) {
            log.trace("bad message detected")
            sudo delete message
        } else {
            log.trace("message safe")
            userFirstMessage.set(message.date)
        }

    }

}
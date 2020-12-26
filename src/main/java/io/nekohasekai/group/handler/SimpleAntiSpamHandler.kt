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

class SimpleAntiSpamHandler : TdHandler() {

    val log = mkLog("AntiSpam")

    val virusAbs = ".*\\.(cmd|bat|exe|ps1|rar|zip|lha|lzh)".toRegex()

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

        if (userId == 0 || isChatAdmin(chatId, userId) || message.isServiceMessage) return
        val config = global.groupConfigs.fetch(chatId).value
        if (config == null || config.simpleAs == 0) return
        val action = config.simpleAs
        val content = message.content
        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) true else {
            message.date - userFirstMessage.value!! < 3 * 60
        }.takeIf { it } ?: return

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
                exec()
            }
        } else if (content is TdApi.MessageContact) {
            exec()
        } else if (message.forwardInfo != null &&
            (message.textOrCaption == null ||
                    message.textOrCaption!!.count { CharUtil.isEmoji(it) } > 2)
        ) {
            exec()
        }

        val isSafe = (content is TdApi.MessageText) &&
                content.text.entities.isEmpty() &&
                content.text.text.count { CharUtil.isEmoji(it) } < 2

        if (!isSafe) {
            sudo delete message
        } else {
            userFirstMessage.set(message.date)
        }

    }

}
package io.nekohasekai.group.handler

import cn.hutool.core.date.SystemClock
import cn.hutool.core.util.CharUtil
import io.nekohasekai.group.database.UserFirstMessage.FirstMessageMap
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.defaultLog
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdException
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.*
import io.nekohasekai.ktlib.td.extensions.*
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.isChatAdmin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import td.TdApi

/**
 * 机器人检查
 * 由于诸多检查, 不应被删除服务消息覆盖.
 */
class SimpleAntiSpamHandler : TdHandler() {

    val virusAbs = ".*\\.(cmd|bat|exe|ps1)".toRegex()

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

        if (userId == 0 || isChatAdmin(chatId, userId)) return

        val action = (global.groupConfigs.fetch(chatId).value?.takeIf { it.simpleAs != 0 } ?: return).simpleAs

        if (message.isServiceMessage) {
            if (message.content is TdApi.MessageChatJoinByLink) {
                GlobalScope.launch(Dispatchers.IO) {
                    delay(1000L)
                    if (!getChatMember(chatId, userId).isMember) sudo delete message
                }
            }
            return
        }

        val content = message.content

        // 首次加入检查

        var isFirstMessage = false

        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) {
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                try {
                    val userMessages = searchChatMessages(
                        chatId,
                        "",
                        TdApi.MessageSenderUser(userId),
                        0,
                        0,
                        100,
                        TdApi.SearchMessagesFilterEmpty(),
                        0
                    )
                    isFirstMessage =
                        userMessages.messages.none { !it.isServiceMessage && message.date - it.date > 3 * 60 * 60 }
                    if (!isFirstMessage) {
                        userFirstMessage.set(userMessages.messages.filter { !it.isServiceMessage }
                            .minByOrNull { it.date }!!.date)
                    }
                } catch (e: TdException) {
                    defaultLog.warn(e)
                }
            }
        } else {
            isFirstMessage = message.date - userFirstMessage.value!! > 3 * 60 * 60
        }

        if (userFirstMessage.value == null) {
            isFirstMessage = true
            userFirstMessage.set(message.date)
        }

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
            userFirstMessage.set(null)
            finishEvent()
        }

        if (content is TdApi.MessageDocument) {
            if (content.document.fileName.matches(virusAbs)) exec()
        } else if (content is TdApi.MessageContact) {
            exec()
        } else if (message.forwardInfo != null && message.textOrCaption != null && message.textOrCaption!!.count {
                CharUtil.isEmoji(
                    it
                )
            } > 2) {
            exec()
        }

        val isSafe = (content is TdApi.MessageText &&
                content.text.entities.none {
                    when (it.type) {
                        is TdApi.TextEntityTypeUrl,
                        is TdApi.TextEntityTypeTextUrl,
                        is TdApi.TextEntityTypeMention -> true
                        else -> false
                    }
                } &&
                content.text.text.count { CharUtil.isEmoji(it) } < 3)

        if (!isSafe) {
            sudo delete message
        } else {
            userFirstMessage.set(message.date)
        }

    }

}
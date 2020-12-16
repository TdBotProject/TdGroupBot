package io.nekohasekai.group.handler.special

import cn.hutool.core.util.NumberUtil
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.defaultLog
import io.nekohasekai.ktlib.td.core.TdException
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.core.raw.searchChatMessages
import io.nekohasekai.ktlib.td.core.raw.setChatMemberStatus
import io.nekohasekai.ktlib.td.extensions.displayName
import io.nekohasekai.ktlib.td.extensions.fromSuperGroup
import io.nekohasekai.ktlib.td.extensions.isServiceMessage
import io.nekohasekai.ktlib.td.extensions.text
import io.nekohasekai.ktlib.td.i18n.FN_SUPER_GROUP_ONLY
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi
import java.io.File

class SP2 : TdHandler() {

    lateinit var configFile: File
    val enabled = HashSet<Long>()

    override fun onLoad() {
        configFile = File(sudo.options.databaseDirectory, "sp2_enable")
        if (configFile.isFile) {
            runCatching {
                val enabledList = configFile.readText()
                    .split("\n")
                    .mapNotNull { line -> line.trim().takeIf { it.isNotBlank() } }
                    .map { it.toLong() }
                enabled.addAll(enabledList)
            }
        }

        initFunction("sp2")
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

        if (!NumberUtil.isLong(param) && !message.fromSuperGroup) {

            sudo makeHtml localeFor(userId).FN_SUPER_GROUP_ONLY replyTo message
            return

        }

        val targetChat = if (NumberUtil.isLong(param)) param.toLong() else chatId

        if (global.admin != userId && checkChatAdmin(targetChat, userId, message)) return

        when (param) {
            "on" -> enabled.add(chatId)
            "off" -> enabled.remove(chatId)
            else -> {
                sudo make "Unknown subcommand" onSuccess deleteDelay(message) replyTo message
                return
            }
        }

        val data = enabled.joinToString("\n") { "$it" }.trim()
        if (data.isNotBlank()) {
            configFile.delete()
        } else {
            configFile.writeText(data)
        }
        sudo make "Saved." replyTo message
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {
        if (message.isServiceMessage || userId == 0 || !enabled.contains(chatId)) return

        val userName = getUser(userId).displayName

        suspend fun exec() {
            if (isUserAgentAvailable(chatId)) {
                with(userAgent!!) {
                    deleteChatMessagesFromUser(chatId, userId)
                }
            } else {
                delete(message)
            }
            setChatMemberStatus(
                chatId, userId, TdApi.ChatMemberStatusRestricted(
                    true, 0,
                    TdApi.ChatPermissions()
                )
            )
            finishEvent()
        }

        if (userName.contains("(無名氏|无名氏)".toRegex())) exec()

        if (isUserAgentAvailable(chatId)) with(userAgent!!) {
            var isFirstMessage = true
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
                isFirstMessage = userMessages.messages.none { !it.isServiceMessage && message.date - it.date > 3 * 60 }
            } catch (e: TdException) {
                defaultLog.warn(e)
            }
            if (isFirstMessage && message.text?.contains("(某人|直男|屌癌)".toRegex()) == true) exec()
        }
    }

}
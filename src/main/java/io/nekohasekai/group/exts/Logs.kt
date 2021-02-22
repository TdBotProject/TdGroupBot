package io.nekohasekai.group.exts

import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChatOrNull
import io.nekohasekai.ktlib.td.core.raw.getUserFullInfo
import io.nekohasekai.ktlib.td.core.raw.getUserOrNull
import io.nekohasekai.ktlib.td.core.raw.parseTextEntities
import io.nekohasekai.ktlib.td.extensions.*
import io.nekohasekai.ktlib.td.utils.makeForward
import io.nekohasekai.ktlib.td.utils.makeHtml
import td.TdApi

private suspend fun TdHandler.fmtMsg(chatId: Long, messageId: Long, userId: Int, actions: Array<out String>): String {
    var log = ""
    if (chatId != 0L) {
        val chat = getChatOrNull(chatId)
        if (chat != null) {
            log += "ChatId: ".htmlBold
            log += chat.id.htmlCode
            log += "\n"
            log += "Chat: ".htmlBold
            val username = getChatUsername(chat)
            if (username == null) {
                log += "<Private>".htmlCode
                log += "\n"
            } else {
                log += chat.title.htmlLink("https://t.me/$username")
                log += "\n"
            }
        }
        if (messageId != 0L) {
            log += "MessageId: ".htmlBold
            log += messageId.htmlCode
            log += "\n"
        }
    }
    if (userId != 0) {
        log += "UserId: ".htmlBold
        log += "$userId".htmlInlineMention(userId)
        log += "\n"
        val user = getUserOrNull(userId)
        val userInfo = getUserFullInfo(userId)
        if (user != null) {
            log += "Name: ".htmlBold
            log += user.displayName
            log += "\n"
            if (!userInfo.bio.isNullOrBlank()) {
                log += "Desc: ".htmlBold
                log += getUserFullInfo(userId).bio.htmlCode
                log += "\n"
            }
        }
    }
    for (index in actions.indices) {
        log += if (index % 2 == 0) {
            (actions[index] + ": ").htmlBold
        } else {
            actions[index].htmlCode + "\n"
        }
    }
    return log
}

suspend fun TdHandler.postLog(
    message: TdApi.Message,
    vararg actions: String
) {
    val log = fmtMsg(message.chatId, message.id, message.senderUserId, actions)
    clientLog.info(parseTextEntities(log, TdApi.TextParseModeHTML()).text)

    var forwardedMessageId = 0L
    if (message.canBeForwarded && global.logChannel != 0L) {
        forwardedMessageId = (sudo makeForward message syncTo global.logChannel).id
    }

    if (global.logChannel != 0L) {
        sudo makeHtml log replyAt forwardedMessageId sendTo global.logChannel
    }
}

suspend fun TdHandler.postLog(
    chatId: Long = 0L,
    userId: Int = 0,
    vararg actions: String
) {
    val log = fmtMsg(chatId, 0L, userId, actions)
    clientLog.info(parseTextEntities(log, TdApi.TextParseModeHTML()).text)

    sudo makeHtml log sendTo global.logChannel
}
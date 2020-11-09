package io.nekohasekai.group.manage

import cn.hutool.cache.impl.LFUCache
import cn.hutool.core.util.NumberUtil
import io.nekohasekai.group.*
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.core.shift
import io.nekohasekai.ktlib.td.core.TdException
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChat
import io.nekohasekai.ktlib.td.extensions.*
import io.nekohasekai.ktlib.td.i18n.*
import io.nekohasekai.ktlib.td.utils.*
import kotlinx.coroutines.delay
import td.TdApi

class GroupOptions : TdHandler() {

    companion object {

        const val DATA_ID = DATA_OPTIONS
        const val PAYLOAD = "options"

    }

    fun def() = TdApi.BotCommand(
            "options",
            clientLocale.OPTIONS_DEF
    )

    override fun onLoad() {

        initFunction("options")

        initData(DATA_ID)

        initStartPayload(PAYLOAD)

        sudo addHandler ChannelMessagesOptions()

    }

    class PreSession(
            val chatId: Long,
            val fromMessage: Long
    )

    val preSessions = LFUCache<Int, PreSession>(-1, 3 * 60 * 1000L)

    override suspend fun gc() {

        preSessions.clear()

    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {

        if (!NumberUtil.isLong(param) && !message.fromSuperGroup) {

            sudo makeHtml localeFor(userId).FN_SUPER_GROUP_ONLY replyTo message

            return

        }

        val targetChat = if (NumberUtil.isLong(param)) param.toLong() else chatId

        try {
            getChat(targetChat)
        } catch (e: TdException) {
            sudo make e.message replyTo message
            return
        }

        if (global.admin != userId) {

            if (!message.fromAnonymous && checkChatAdmin(targetChat, userId, message)) return

        }

        if (!hasRequiredPermission(targetChat, me.id,
                        canDeleteMessages = true,
                        canPinMessages = true)) {

            sudo makeMd localeFor(userId).BOT_NOT_ADMIN replyTo message

            return

        }

        if (message.fromPrivate) {

            startSet(userId, chatId, 0L, targetChat, false)

            return

        }

        val L = localeFor(userId)

        sudo delete message

        sudo make L.MENU_SWITCH_PM withMarkup inlineButton {

            dataLine(L.MENU_SWITCH_BUTTON, DATA_ID, byteArrayOf(-1))

        } sendTo chatId

    }

    override suspend fun onNewCallbackQuery(userId: Int, chatId: Long, messageId: Long, queryId: Long, data: Array<ByteArray>) {

        val action = data[0][0].toInt()

        if (action == -1) {

            if (global.admin != userId && checkChatAdmin(chatId, userId, queryId)) return

            preSessions.put(userId, PreSession(chatId, messageId))

            sudo makeAnswerUrl mkStartPayloadUrl(me.username, "options") answerTo queryId

            return

        }

        val targetChat = global.optionChats.fetch(userId to messageId).value

        if (targetChat == null) {

            sudo makeAlert localeFor(userId).SESSION_TIMEOUT answerTo queryId

            delay(1000L)

            delete(chatId, messageId)

            return

        }

        if (action == 0) {

            sudo confirmTo queryId

            startSet(userId, chatId, messageId, targetChat, true)

        } else if (action == 1) {

            findHandler<ChannelMessagesOptions>().onOptionsCallbackQuery(userId, chatId, messageId, queryId, targetChat, data.shift())

        }

    }

    suspend fun startSet(userId: Int, chatId: Long, messageId: Long, targetChat: Long, isEdit: Boolean) {

        val chatCache = global.optionMessages.fetch(userId to chatId)

        val lastMessage = chatCache.value

        if (lastMessage != null && lastMessage > 0L && !isEdit) {

            global.optionChats.fetch(userId to lastMessage).write(null)

            delete(chatId, lastMessage)

        }

        val L = localeFor(userId)

        sudo makeMd L.OPTIONS_MENU.input(getChat(targetChat).title) withMarkup inlineButton {

            dataLine(L.MENU_CHANNEL_MESSAGE, DATA_ID, ChannelMessagesOptions.SUB_ID)

        } onSuccess {

            if (!isEdit) {

                global.optionChats.fetch(userId to it.id).write(chatId)
                global.optionMessages.remove(userId to chatId)

            }

        } at messageId edit isEdit sendOrEditTo chatId

    }

    override suspend fun onStartPayload(userId: Int, chatId: Long, message: TdApi.Message, payload: String, params: Array<String>) {

        val targetChat = preSessions.get(userId)

        if (targetChat == null) {

            sudo make localeFor(userId).SESSION_TIMEOUT sendTo chatId

            return

        }

        preSessions.remove(userId)

        delete(targetChat.chatId, targetChat.fromMessage)

        startSet(userId, chatId, 0L, targetChat.chatId, false)

    }

}
package io.nekohasekai.group.handler

import cn.hutool.cache.impl.LFUCache
import cn.hutool.core.date.SystemClock
import io.nekohasekai.group.MODES
import io.nekohasekai.group.REPORT
import io.nekohasekai.group.SW_INLIST
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.htmlInlineMentionSafe
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.core.raw.setChatMemberStatus
import io.nekohasekai.ktlib.td.extensions.Hours
import io.nekohasekai.ktlib.td.extensions.Minutes
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import io.nekohasekai.spamwatch.SpamWatch
import io.nekohasekai.spamwatch.SpamWatchException
import io.nekohasekai.spamwatch.models.BanRecord
import td.TdApi

class SpamWatchHandler : TdHandler() {

    class CheckResult(val isSpam: Boolean) {
        var record: BanRecord? = null
        var message: String? = null

        constructor(record: BanRecord) : this(true) {
            this.record = record
        }

        constructor(message: String) : this(false) {
            this.message = message
        }
    }

    companion object {
        val checkResult = LFUCache<Int, CheckResult>(-1, 3 * Hours)
        const val functionCheckSelf = "_sw_check_me"
    }

    lateinit var spamWatch: SpamWatch

    override fun onLoad() {

        initFunction(functionCheckSelf)
        spamWatch = SpamWatch(global.spamWatchKey)

    }

    suspend fun readRecord(userId: Int): CheckResult = if (checkResult.containsKey(userId)) {
        checkResult[userId]
    } else try {
        CheckResult(spamWatch.getSpecific(userId))
    } catch (e: SpamWatchException) {
        CheckResult(e.message ?: (e.cause ?: e).javaClass.simpleName)
    }.also {
        checkResult.put(userId, it)
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (message.content !is TdApi.JoinChatByInviteLink) return

        val config = global.groupConfigs.fetch(chatId).value?.takeIf { it.spamWatch > 0 } ?: return

        if (userId == 0 || isChatAdmin(chatId, userId)) return

        val record = readRecord(userId).takeIf { it.isSpam } ?: return

        when (config.spamWatch) {
            1 -> setChatMemberStatus(
                chatId, userId, TdApi.ChatMemberStatusRestricted(
                    true, 0, TdApi.ChatPermissions()
            ))
            2 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusBanned())
            3 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusBanned(((SystemClock.now() + 1 * Minutes) / 1000).toInt()))
        }

        if (isUserAgentAvailable(chatId)) with(userAgent!!) {
            deleteChatMessagesFromUser(chatId, userId)
        } else {
            sudo delete message
        }

        val L = localeFor(chatId)

        sudo makeHtml L.SW_INLIST.input(
                getUser(userId).htmlInlineMentionSafe,
                record.record!!.reason,
                L.MODES.split("|")[config.spamWatch]
        ) withMarkup inlineButton {
            newLine {
                urlButton(L.REPORT, global.reportUrl)
            }
        } onSuccess deleteDelay(timeMs = 3 * Minutes) syncTo chatId

    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {

        if (function == functionCheckSelf) {

            val record = readRecord(userId)
            sudo make "${record.record ?: record.message}" replyTo message

        }

    }

}
package io.nekohasekai.group.handler

import cn.hutool.cache.impl.LFUCache
import io.nekohasekai.group.MODES
import io.nekohasekai.group.REPORT
import io.nekohasekai.group.SW_INLIST
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.isUserAgentAvailable
import io.nekohasekai.group.exts.removeNonASCII
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.extensions.Hours
import io.nekohasekai.ktlib.td.extensions.Minutes
import io.nekohasekai.ktlib.td.extensions.displayName
import io.nekohasekai.ktlib.td.extensions.htmlInlineMention
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import io.nekohasekai.spamwatch.SpamWatch
import io.nekohasekai.spamwatch.SpamWatchException
import io.nekohasekai.spamwatch.models.BanRecord
import td.TdApi

class SpamWatchHandler : TdHandler(), FirstMessageHandler.Interface {

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
        const val functionCheckSelf = "_sw_check"
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

    override suspend fun onFirstMessage(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        config: GroupConfig
    ): Boolean {

        val action = config.spamWatch.takeIf { it > 0 } ?: return false
        val record = readRecord(userId).takeIf { it.isSpam } ?: return false

        if (isUserAgentAvailable(chatId)) with(userAgent!!) {
            deleteChatMessagesFromUser(chatId, userId)
        } else {
            sudo delete message
        }

        when (action) {
            1 -> muteMember(chatId, userId)
            2 -> banChatMember(chatId, userId)
            3 -> kickMember(chatId, userId)
        }

        val L = localeFor(chatId)

        sudo makeHtml L.SW_INLIST.input(
            getUser(userId)
                .displayName
                .removeNonASCII()
                .htmlInlineMention(userId),
            record.record!!.reason,
            L.MODES.split("|")[config.spamWatch]
        ) withMarkup inlineButton {
            newLine {
                urlButton(L.REPORT, global.reportUrl)
            }
        } onSuccess deleteDelay(timeMs = 3 * Minutes) syncTo chatId

        return true

    }

    override suspend fun onFunction(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        function: String,
        param: String,
        params: Array<String>
    ) {

        if (function == functionCheckSelf) {

            val record = readRecord(userId)
            sudo make "${record.record ?: record.message}" replyTo message

        }

    }

}
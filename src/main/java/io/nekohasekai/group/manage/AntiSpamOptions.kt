package io.nekohasekai.group.manage

import io.nekohasekai.group.*
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.userAgent
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.core.toStatusString
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.i18n.BACK_ARROW
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi

class AntiSpamOptions : TdHandler() {

    companion object {

        val SUB_ID = byteArrayOf(2)
        val SIMPLE_AS = byteArrayOf(3)
        val SPAM_WATCH = byteArrayOf(4)

    }

    suspend fun onOptionsCallbackQuery(
        userId: Int,
        chatId: Long,
        messageId: Long,
        queryId: Long,
        targetChat: Long,
        data: Array<ByteArray>,
        subId: Int
    ) {

        val L = localeFor(userId)

        if (subId == 2) {

            sudo make L.AS_INFO withMarkup inlineButton {
                dataLine(L.MENU_AS_SIMPLE, GroupOptions.DATA_ID, SIMPLE_AS)
                dataLine(L.MENU_SPAM_WARCH, GroupOptions.DATA_ID, SPAM_WATCH)
                dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, GroupOptions.BACK)
            } at messageId editTo chatId

            sudo confirmTo queryId
            return

        }

        val config = global.groupConfigs.fetch(targetChat)
        val cache = config.value ?: database.write {
            GroupConfig.new(targetChat, {}).also { config.set(it) }
        }

        if (subId == 3) {

            if (data.isEmpty()) {
                sudo makeMd L.SIMPLE_AS_INFO.input(userAgent?.me?.username ?: "<Not Configured>") withMarkup mkButtons(
                    userId,
                    cache.simpleAs
                ) at messageId editTo chatId
                sudo confirmTo queryId
                return
            }

            val targetAction = data[0][0].toInt()
            if (cache.simpleAs != targetAction) {
                database.write {
                    cache.simpleAs = targetAction
                }
                config.notifyChanged()
            }

            sudo makeInlineButton mkButtons(userId, cache.simpleAs) at messageId editTo chatId

        } else if (subId == 4) {

            if (data.isEmpty()) {
                sudo make L.SW_INFO withMarkup mkButtons(userId, cache.spamWatch) at messageId editTo chatId
                sudo confirmTo queryId
                return
            }

            val targetAction = data[0][0].toInt()
            if (cache.spamWatch != targetAction) {
                database.write {
                    cache.spamWatch = targetAction
                }
                config.notifyChanged()
            }

            sudo makeInlineButton mkButtons(userId, cache.spamWatch) at messageId editTo chatId

        }

        sudo makeAnswer L.FN_SETTING_UPDATED answerTo queryId


    }

    fun mkButtons(userId: Int, curr: Int): TdApi.ReplyMarkupInlineKeyboard {

        val L = localeFor(userId)

        return inlineButton {

            L.MODES.split("|").forEachIndexed { index, it ->
                newLine {
                    dataButton(it, -1)
                    dataButton(
                        (index == curr).toStatusString(true),
                        GroupOptions.DATA_ID,
                        SIMPLE_AS,
                        byteArrayOf(index.toByte())
                    )
                }
            }
            dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, SUB_ID)

        }

    }


}
package io.nekohasekai.group.manage

import io.nekohasekai.group.*
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.core.toStatusString
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.i18n.BACK_ARROW
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi

class AntiSpamOptions : GroupOptions.Handler() {

    companion object {

        val SUB_ID = byteArrayOf(2)
        val SIMPLE_AS = byteArrayOf(3)
        val SPAM_WATCH = byteArrayOf(4)

    }

    override suspend fun onOptionsCallbackQuery(
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

            sudo confirmTo queryId

            sudo make L.AS_INFO withMarkup inlineButton {
                dataLine(L.MENU_AS_SIMPLE, GroupOptions.DATA_ID, SIMPLE_AS)
                dataLine(L.MENU_SPAM_WARCH, GroupOptions.DATA_ID, SPAM_WATCH)
                dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, GroupOptions.BACK)
            } at messageId editTo chatId

            return

        }

        sudo makeAnswer L.FN_SETTING_UPDATED answerTo queryId

        val config = global.groupConfigs.fetch(targetChat)
        val cache = config.value ?: database.write {
            GroupConfig.new(targetChat, {}).also { config.set(it) }
        }

        if (subId == 3) {

            if (data.isEmpty()) {
                sudo makeMd L.SIMPLE_AS_INFO withMarkup mkAsButtons(
                    userId,
                    cache
                ) at messageId editTo chatId
                sudo confirmTo queryId
                return
            }

            val action = data[0][0].toInt()
            val target = data[1][0].toInt()
            if (action == 0) {
                if (cache.simpleAs != target) {
                    database.write {
                        cache.simpleAs = target
                    }
                    config.notifyChanged()
                }
            } else if (target == 0) {
                database.write {
                    cache.adName = !cache.adName
                }
                config.notifyChanged()
            } else if (target == 1) {
                database.write {
                    cache.adContent = !cache.adContent
                }
                config.notifyChanged()
            }

            sudo makeInlineButton mkAsButtons(userId, cache) at messageId editTo chatId

        } else if (subId == 4) {

            if (data.isEmpty()) {
                sudo make L.SW_INFO withMarkup makeSwButtons(userId, cache.spamWatch) at messageId editTo chatId
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

            sudo makeInlineButton makeSwButtons(userId, cache.spamWatch) at messageId editTo chatId

        }

    }

    suspend fun mkAsButtons(userId: Int, config: GroupConfig): TdApi.ReplyMarkupInlineKeyboard {
        val L = localeFor(userId)

        return inlineButton {

            L.MODES.split("|").forEachIndexed { index, it ->
                newLine {
                    textButton(it)
                    dataButton(
                        (index == config.simpleAs).toStatusString(true),
                        GroupOptions.DATA_ID,
                        SIMPLE_AS,
                        byteArrayOf(0),
                        byteArrayOf(index.toByte())
                    )
                }
            }

            textLine(L.AS_OTHER)

            newLine {
                textButton(L.AS_ADNAME)
                dataButton(
                    config.adName.toStatusString(),
                    GroupOptions.DATA_ID,
                    SIMPLE_AS,
                    byteArrayOf(1),
                    byteArrayOf(0)
                )
            }

            newLine {
                textButton(L.AS_ADCONTENT)
                dataButton(
                    config.adContent.toStatusString(),
                    GroupOptions.DATA_ID,
                    SIMPLE_AS,
                    byteArrayOf(1),
                    byteArrayOf(1)
                )
            }

            dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, SUB_ID)
        }
    }

    suspend fun makeSwButtons(userId: Int, curr: Int): TdApi.ReplyMarkupInlineKeyboard {

        val L = localeFor(userId)

        return inlineButton {

            L.MODES.split("|").forEachIndexed { index, it ->
                newLine {
                    dataButton(it, -1)
                    dataButton(
                        (index == curr).toStatusString(true),
                        GroupOptions.DATA_ID,
                        SPAM_WATCH,
                        byteArrayOf(index.toByte())
                    )
                }
            }

            dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, SPAM_WATCH)

        }

    }


}
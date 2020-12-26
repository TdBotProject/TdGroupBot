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

class MemberPolicyOptions : GroupOptions.Handler() {

    companion object {
        val SUB_ID = byteArrayOf(6)
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

        if (data.isEmpty()) {
            sudo confirmTo queryId
            sudo make L.MP_INFO withMarkup mpButtons(userId, targetChat) at messageId editTo chatId
            return
        }

        sudo makeAnswer L.FN_SETTING_UPDATED answerTo queryId

        val config = global.groupConfigs.fetch(targetChat)
        val cache = config.value ?: database.write {
            GroupConfig.new(targetChat, {}).also { config.set(it) }
        }

        val targetAction = data[0][0].toInt()
        if (cache.memberPolicy != targetAction) {
            database.write {
                cache.memberPolicy = targetAction
            }
            config.notifyChanged()
        }

        sudo makeInlineButton mpButtons(userId, targetChat) at messageId editTo chatId

    }

    fun mpButtons(userId: Int, targetChat: Long): TdApi.ReplyMarkupInlineKeyboard {

        val L = localeFor(userId)

        val config = global.groupConfigs.fetch(targetChat).value

        return inlineButton {

            newLine {

                dataButton(L.MODES.split("|")[0], -1)
                dataButton(
                    (config?.memberPolicy ?: 0 == 0).toStatusString(true),
                    GroupOptions.DATA_ID, SUB_ID, byteArrayOf(0)
                )

            }

            newLine {

                dataButton(L.MP_KICK, -1)
                dataButton(
                    (config?.memberPolicy ?: 0 == 1).toStatusString(true),
                    GroupOptions.DATA_ID, SUB_ID, byteArrayOf(1)
                )

            }

            newLine {

                dataButton(L.MP_REQUIRE_JOIN, -1)
                dataButton(
                    (config?.memberPolicy ?: 0 == 2).toStatusString(true),
                    GroupOptions.DATA_ID, SUB_ID, byteArrayOf(2)
                )

            }

            dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, GroupOptions.BACK)

        }
    }

}
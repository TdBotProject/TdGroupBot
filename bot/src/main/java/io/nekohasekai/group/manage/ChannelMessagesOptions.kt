package io.nekohasekai.group.manage

import io.nekohasekai.group.*
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.ktlib.core.toStatusString
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.i18n.BACK_ARROW
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi

class ChannelMessagesOptions : TdHandler() {

    companion object {

        val SUB_ID = byteArrayOf(1)

    }

    fun cmMenu(userId: Int, chatId: Long, messageId: Long, targetChat: Long, isEdit: Boolean) {

        val L = localeFor(userId)

        sudo make L.MENU_CM_HELP withMarkup cmButtons(userId, targetChat) at messageId edit isEdit sendOrEditTo chatId

    }

    fun cmButtons(userId: Int, targetChat: Long): TdApi.ReplyMarkupInlineKeyboard {

        val L = localeFor(userId)

        val config = global.groupConfigs.fetch(targetChat).value

        return inlineButton {

            newLine {

                dataButton(L.CM_MODE_ASIS, -1)
                dataButton((config?.cmMode ?: 0 == 0).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(1))

            }

            newLine {

                dataButton(L.CM_MODE_UNPIN, -1)
                dataButton((config?.cmMode ?: 0 == 3).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(4))

            }


            newLine {

                dataButton(L.CM_MODE_DELETE, -1)
                dataButton((config?.cmMode == 1).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(2))

            }

            newLine {

                dataButton(L.CM_MODE_FORWARD_AND_DELETE, -1)
                dataButton((config?.cmMode == 2).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(3))

            }

            dataLine(L.BACK_ARROW, GroupOptions.DATA_ID, byteArrayOf(0))

        }

    }

    suspend fun onOptionsCallbackQuery(userId: Int, chatId: Long, messageId: Long, queryId: Long, targetChat: Long, data: Array<ByteArray>) {

        sudo confirmTo queryId

        if (data.isEmpty()) {

            cmMenu(userId, chatId, messageId, targetChat, true)

            return

        }

        val config = global.groupConfigs.fetch(targetChat)

        val cache = config.value

        val newMode = data[0][0].toInt() - 1

        database.write {

            if (cache == null) {

                config.set(GroupConfig.new(targetChat) { cmMode = newMode })

            } else if (cache.cmMode != 2) {

                cache.cmMode = newMode
                config.notifyChanged()

            }

        }

        sudo makeInlineButton cmButtons(userId, targetChat) at messageId editTo chatId

    }

}
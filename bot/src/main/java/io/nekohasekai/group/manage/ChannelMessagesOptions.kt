package io.nekohasekai.group.manage

import io.nekohasekai.group.*
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.database.LastPinned
import io.nekohasekai.ktlib.core.toStatusString
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChatWith
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

                dataButton(L.CM_MODE_DELETE, -1)
                dataButton((config?.cmMode == 1).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(2))

            }

            newLine {

                dataButton(L.CM_MODE_FORWARD_AND_DELETE, -1)
                dataButton((config?.cmMode == 2).toStatusString(true), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(3))

            }

            newLine {

                dataButton(L.CM_KEEP_PIN, -1)
                dataButton((config?.keepPin == true).toStatusString(), GroupOptions.DATA_ID, SUB_ID, byteArrayOf(4))

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

        when (data[0][0].toInt()) {

            1 -> database.write {

                if (config.value == null) {

                    config.value = GroupConfig.new(targetChat) { cmMode = 0 }

                } else {

                    config.value!!.cmMode = 0
                    config.changed = true

                }

            }

            2 -> database.write {

                if (config.value == null) {

                    config.value = GroupConfig.new(targetChat) { cmMode = 1 }

                } else {

                    config.value!!.cmMode = 1
                    config.changed = true

                }

            }

            3 -> database.write {

                if (config.value == null) {

                    config.value = GroupConfig.new(targetChat) { cmMode = 2 }

                } else {

                    config.value!!.cmMode = 2
                    config.changed = true

                }

            }

            4 -> database.write {

                if (config.value == null) {

                    config.value = GroupConfig.new(targetChat) { keepPin = true }

                } else {

                    config.value!!.keepPin = !config.value!!.keepPin
                    config.changed = true

                }

                if (config.value!!.keepPin) {

                    val cache = global.lastPinneds.fetch(targetChat)

                    getChatWith(targetChat) {

                        onSuccess {

                            if (cache.value == null) {

                                cache.value = LastPinned.new(chatId) { pinnedMessage = it.pinnedMessageId }

                            } else {

                                cache.value!!.pinnedMessage = it.pinnedMessageId
                                cache.changed = true

                            }

                        }

                    }

                }

            }

        }

        sudo makeInlineButton cmButtons(userId, targetChat) at messageId editTo chatId

    }

}
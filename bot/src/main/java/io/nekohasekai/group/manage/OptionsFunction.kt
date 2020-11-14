package io.nekohasekai.group.manage

import cn.hutool.core.util.NumberUtil
import io.nekohasekai.group.*
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.fromPrivate
import io.nekohasekai.ktlib.td.extensions.fromSuperGroup
import io.nekohasekai.ktlib.td.i18n.FN_SUPER_GROUP_ONLY
import io.nekohasekai.ktlib.td.i18n.localeFor
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi

class OptionsFunction : TdHandler() {

    override fun onLoad() {

        initFunction("write_option", "wo")

    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {

        if (!NumberUtil.isLong(param) && !message.fromSuperGroup) {

            sudo makeHtml localeFor(userId).FN_SUPER_GROUP_ONLY replyTo message
            return

        }

        val targetChat = if (NumberUtil.isLong(param)) param.toLong() else chatId

        if (global.admin != userId && checkChatAdmin(targetChat, userId, message)) return

        if (param.isBlank()) {

            sudo make localeFor(targetChat, userId).FN_MISSING_ACTION onSuccess deleteDelayIf(!message.fromPrivate, message) replyTo message

            return

        }

        val config = global.groupConfigs.fetch(targetChat)

        val cache = config.value ?: database.write {

            GroupConfig.new(targetChat, {}).also { config.set(it) }

        }

        val intOptions = arrayOf(
                "cm_mode",
                "bot_check",
                "spam_watch"
        )

        val action = params[0]

        if (action in intOptions) {

            val newMode = try {
                params[1].toInt()
            } catch (e: Exception) {
                sudo make localeFor(targetChat, userId).FN_INVALID_PARAM onSuccess deleteDelayIf(!message.fromPrivate, message) replyTo message
                return
            }

            when (intOptions.indexOf(params[0])) {
                0 -> if (cache.cmMode != newMode) {
                    database.write {
                        cache.cmMode = newMode
                    }
                    config.notifyChanged()
                }
                1 -> if (cache.botCheck != newMode) {
                    database.write {
                        cache.botCheck = newMode
                    }
                    config.notifyChanged()
                }
                2 -> if (cache.spamWatch != newMode) {
                    database.write {
                        cache.spamWatch = newMode
                    }
                    config.notifyChanged()
                }
            }

        } else {

            sudo make localeFor(targetChat, userId).FN_UNKNOWN_ACTION onSuccess deleteDelayIf(!message.fromPrivate, message) replyTo message
            return

        }

        sudo make localeFor(targetChat, userId).FN_SETTING_UPDATED replyTo message

    }

}
package io.nekohasekai.group

import io.nekohasekai.ktlib.td.cli.TdCli
import td.TdApi

object TestLauncher : TdCli() {

    override val loginType = LoginType.USER

    @JvmStatic
    fun main(args: Array<String>) {

        launch(args)

        start()

    }

    override fun onLoad() {

        options apiId 971882
        options apiHash "1232533dd027dc2ec952ba91fc8e3f27"

        options databaseDirectory "data/test"

    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (userId != 162726413) return

        println(message)

    }

    override suspend fun onMessageEdited(chatId: Long, messageId: Long, editDate: Int, replyMarkup: TdApi.ReplyMarkup?) {

        if (chatId != 162726413L || replyMarkup == null) return

        println(replyMarkup)

        val buttons = (replyMarkup as? TdApi.ReplyMarkupInlineKeyboard)?.rows ?: return

        buttons.forEach { row ->

            row.forEach {

                val data = it.type as? TdApi.InlineKeyboardButtonTypeCallback ?: return

                println("${it.text}: ${String(data.data)}")

            }

        }

    }

}
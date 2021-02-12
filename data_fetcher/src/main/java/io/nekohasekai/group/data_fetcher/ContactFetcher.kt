package io.nekohasekai.group.data_fetcher

import io.nekohasekai.ktlib.td.core.raw.getMarkdownText
import io.nekohasekai.ktlib.td.core.raw.searchPublicChat
import io.nekohasekai.ktlib.td.utils.fetchMessages
import td.TdApi
import java.io.File
import java.util.*
import kotlin.collections.HashSet

object ContactFetcher : TestClient() {

    @JvmStatic
    fun main(args: Array<String>) = start(args)

    override suspend fun onLogin() {

        val chat = settings.getItemOrPut("chat_scp_log") { searchPublicChat("SCP_079_LOGGING").id }

        var count = 0

        val messages = LinkedList<TdApi.Message>()

        fetchMessages(chat) {
            count += it.size
            messages.addAll(it)
            count < 40 * 1000
        }

        val names = HashSet<String>()

        for (message in messages) {
            if (message.forwardInfo != null) continue
            val content = message.content as? TdApi.MessageText ?: continue
            val mdText = getMarkdownText(content.text).text
            if (!mdText.contains("联系方式")) continue
            val name = mdText.substringAfter("联系方式：`").substringBefore('`')

            names.add(name)
        }

        println(names)

        File("src/main/resources/ad_contact.txt").writeText(
            names.joinToString("\n")
        )

        waitForClose()

    }

    class WordWrap(val word: String, val length: Int) : Comparable<WordWrap> {
        override fun compareTo(other: WordWrap): Int {
            if (word == other.word) return 0
            return other.length - length
        }
    }

}
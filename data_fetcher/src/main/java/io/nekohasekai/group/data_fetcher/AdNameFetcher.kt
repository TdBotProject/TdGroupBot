package io.nekohasekai.group.data_fetcher

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.utility.TextUtility
import io.nekohasekai.ktlib.cc.CCConverter
import io.nekohasekai.ktlib.cc.CCTarget
import io.nekohasekai.ktlib.td.core.raw.getMarkdownText
import io.nekohasekai.ktlib.td.core.raw.searchPublicChat
import io.nekohasekai.ktlib.td.utils.fetchMessages
import td.TdApi
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object AdNameFetcher : TestClient() {

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
        val seg = HanLP.newSegment()
        val cc = CCConverter(CCTarget.SC)

        val words = HashMap<String, Int>()
        val phrases = HashMap<String, Int>()

        for (message in messages) {
            if (message.forwardInfo != null) continue
            val content = message.content as? TdApi.MessageText ?: continue
            val mdText = getMarkdownText(content.text).text
            if (!mdText.contains("`昵称检查`")) continue
            val name = mdText.substringAfter("用户昵称：`").substringBefore('`')
                .filter { TextUtility.isChinese(it) }
                .let { cc.convert(it) }

            for (w in seg.seg(name)) {
                words[w.word] = (words[w.word] ?: 0) + 1
            }
            for (k in HanLP.extractPhrase(name, -1)) {
                phrases[k] = (phrases[k] ?: 0) + 1
            }
        }

        val sorted = TreeSet(words.map { WordWrap(it.key, it.value) }.filter { wrap ->
            wrap.word.length > 1 && wrap.length > 10
        })

        val sortedP = TreeSet(phrases.map { WordWrap(it.key, it.value) }.filter { wrap ->
            wrap.word.length > 1 && !sorted.any { wrap.word.contains(it.word) } && wrap.length > 10
        })

        for (word in sorted) {
            println(word.word + "/" + word.length)
        }

        for (word in sortedP) {
            println(word.word + "/" + word.length)
        }

        File("src/main/resources/ad_name.txt").writeText(
            (sorted + sortedP).joinToString("\n") { it.word }
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
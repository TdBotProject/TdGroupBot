package io.nekohasekai.group.data_fetcher

import cn.hutool.core.util.CharUtil
import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.utility.TextUtility
import io.nekohasekai.ktlib.cc.CCConverter
import io.nekohasekai.ktlib.cc.CCTarget
import io.nekohasekai.ktlib.td.core.raw.getMarkdownText
import io.nekohasekai.ktlib.td.core.raw.searchPublicChat
import io.nekohasekai.ktlib.td.extensions.textOrCaption
import io.nekohasekai.ktlib.td.utils.fetchMessages
import td.TdApi
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object AdTextFetcher : TestClient() {

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

        val texts = HashSet<String>()
        val seg = HanLP.newSegment()
        val cc = CCConverter(CCTarget.SC)

        val words = HashMap<String, Int>()
        val phrases = HashMap<String, Int>()

        for (message in messages) {
            val content = message.textOrCaption ?: continue
            val text = if (message.forwardInfo != null) content else {
                val mdText = getMarkdownText(((message.content as? TdApi.MessageText) ?: continue).text).text
                if (!mdText.contains("`简介检查`")) continue
                mdText.substringAfter("用户简介：`").substringBefore('`')
            }.filter { TextUtility.isChinese(it) || CharUtil.isLetter(it) }.let { cc.convert(it) }

            for (w in seg.seg(text)) {
                words[w.word] = (words[w.word] ?: 0) + 1
            }
            for (k in HanLP.extractPhrase(text, -1)) {
                phrases[k] = (phrases[k] ?: 0) + 1
            }
        }

        val sorted = TreeSet(words.map { AdNameFetcher.WordWrap(it.key, it.value) }.filter { wrap ->
            wrap.word.length > 1 && wrap.length > 10
        })

        val sortedP = TreeSet(phrases.map { AdNameFetcher.WordWrap(it.key, it.value) }.filter { wrap ->
            wrap.word.length > 1 && !sorted.any { wrap.word.contains(it.word) } && wrap.length > 10
        })

        for (word in sorted) {
            println(word.word + "/" + word.length)
        }

        for (word in sortedP) {
            println(word.word + "/" + word.length)
        }

        File("src/main/resources/ad_content.txt").writeText(
            (sorted + sortedP).joinToString("\n") { it.word }
        )

        waitForClose()

    }


}
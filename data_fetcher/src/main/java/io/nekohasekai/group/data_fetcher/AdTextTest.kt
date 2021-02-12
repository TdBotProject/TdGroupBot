package io.nekohasekai.group.data_fetcher

import com.hankcs.hanlp.HanLP
import io.nekohasekai.ktlib.cc.CCConverter
import io.nekohasekai.ktlib.cc.CCTarget
import io.nekohasekai.ktlib.td.extensions.textOrCaption
import io.nekohasekai.ktlib.td.utils.fetchMessages
import io.nekohasekai.ktlib.td.utils.getChats
import java.io.File
import java.util.*
import kotlin.collections.HashMap

object AdTextTest : TestClient() {

    @JvmStatic
    fun main(args: Array<String>) = start(args)

    override suspend fun onLogin() {

        val adContents = File("src/main/resources/ad_content.txt").readLines()

        val chatToTest = -1001094615131

        getChats()
        val seg = HanLP.newSegment()
        val cc = CCConverter(CCTarget.SC)

        val words = HashMap<String, Int>()
        var count = 0

        fetchMessages(chatToTest) { messages ->
            for (message in messages) {
                val content = (message.textOrCaption ?: continue).let { cc.convert(it) }

                for (w in seg.seg(content)) {
                    if (w.word in adContents) {
                        words[w.word] = (words[w.word] ?: 0) + 1
                    }
                }
                for (k in HanLP.extractPhrase(content, -1)) {
                    if (k in adContents) {
                        words[k] = (words[k] ?: 0) + 1
                    }
                }
            }
            count += messages.size
            if (count % 1000 < 100) println("=> $count")
            count < 20 * 1000
        }

        val sorted = TreeSet(words.map { AdNameFetcher.WordWrap(it.key, it.value) })
        for (word in sorted) {
            println(word.word + "/" + word.length)
        }


    }

}
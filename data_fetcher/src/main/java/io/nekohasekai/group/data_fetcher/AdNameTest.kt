package io.nekohasekai.group.data_fetcher

import cn.hutool.core.lang.Console
import io.nekohasekai.ktlib.cc.CCConverter
import io.nekohasekai.ktlib.cc.CCTarget
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.extensions.displayName
import io.nekohasekai.ktlib.td.utils.banChatMember
import io.nekohasekai.ktlib.td.utils.fetchSupergroupUsers
import io.nekohasekai.ktlib.td.utils.getChats
import java.io.File

object AdNameTest : TestClient() {

    @JvmStatic
    fun main(args: Array<String>) = start(args)

    override suspend fun onLogin() {

        val adNames = File("src/main/resources/ad_name.txt").readLines()

        val chatToTest = -1001432997913

        getChats()
        val cc = CCConverter(CCTarget.SC)

        fetchSupergroupUsers(chatToTest) { members ->
            for (member in members.filter { it.botInfo == null }) {
                val name = getUser(member.userId)
                    .displayName
                    .let { cc.convert(it.toLowerCase()) }

                var nf = true
                for (adName in adNames) {
                    if (name.contains(adName)) {
                        nf = false
                        println("$name => $adName")
                    }
                }
                if (!nf) {
                    print("ban? [y/n] ")
                    if (Console.input() == "y") {
                        banChatMember(chatToTest, member.userId)
                    }
                }
            }
            true
        }

    }

}
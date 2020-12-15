package io.nekohasekai.group.handler.special

import cn.hutool.core.text.csv.CsvUtil
import cn.hutool.core.util.CharsetUtil
import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.htmlCode
import io.nekohasekai.ktlib.td.utils.*
import td.TdApi
import java.io.File

class SP1 : TdHandler() {

    lateinit var cacheFile: File
    var blockList = hashSetOf<String>()

    override fun onLoad() {
        cacheFile = File(sudo.options.databaseDirectory, "sp1_lst.csv")
        if (cacheFile.isFile) {
            blockList = CsvUtil.getReader().read(cacheFile).map { it[0] }.toHashSet()
        }

        initFunction("sp1")
    }

    override suspend fun saveCache() {
        if (blockList.isNotEmpty()) {
            CsvUtil.getWriter(cacheFile, CharsetUtil.CHARSET_UTF_8).write(blockList)
        } else if (cacheFile.isFile) {
            cacheFile.delete()
        }
    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {
        if (userId !in intArrayOf(726643789, global.admin) && chatId != -1001377035755L) rejectFunction()

        if (params.isEmpty()) {

            sudo make """
                usage:
                !sp1 <add/remove/set> [链接 (一行一个)]...
                !sp1 <list/reset>
            """.trimIndent() editOrSendToChat message

            return

        } else when (params[0]) {
            "add", "remove", "set" -> {
                if (params.size < 2) {
                    sudo make """
                        usage:
                        !sp1 add [链接 (一行一个)]...
                    """.trimIndent()
                    return
                }
                val links = param.substringAfter("${params[0]} ").split("\n")
                when (params[0]) {
                    "add" -> {
                        blockList.addAll(links)
                        sudo makeHtml "已添加 ${links.size} 个链接:\n\n " + links.joinToString { it.htmlCode } editOrSendToChat message
                    }
                    "remove" -> {
                        var errors = ""
                        for (link in links) if (!blockList.remove(errors)) {
                            errors += "\n${link.htmlCode}"
                        }
                        errors = errors.trim()
                        errors = if (errors.isBlank()) {
                            "${links.size} 个都成功."
                        } else {
                            val errorSze = errors.split("\n").size
                            "${links.size - errorSze} 个成功, $errorSze 个不在屏蔽列表中:\n\n" + errors
                        }
                        sudo makeHtml "已移除, $errors" editOrSendToChat message
                    }
                    "set" -> {
                        blockList.clear()
                        blockList.addAll(links)
                        sudo makeHtml "已重设为以下 ${links.size} 个链接:\n\n " + links.joinToString { it.htmlCode } editOrSendToChat message
                    }
                }
            }
            "list" -> {
                sudo makeHtml """
                    当前列表包含 ${blockList.size} 个链接:
                    
                    ${blockList.joinToString("\n") { it.htmlCode }}
                """.trimIndent().trim() editOrSendToChat message
            }
            "reset" -> {
                sudo make "已重置." editOrSendToChat message
            }
        }
        saveCache()
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (chatId != -1001377035755L) return

        val lnk = ((((message.replyMarkup as? TdApi.ReplyMarkupInlineKeyboard)
                ?: return).rows[0][0].type as? TdApi.InlineKeyboardButtonTypeUrl) ?: return).url

        if (lnk in blockList) sudo delete message

    }

}
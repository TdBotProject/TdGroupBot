package io.nekohasekai.group.handler

import cn.hutool.core.date.SystemClock
import io.nekohasekai.group.database.UserFirstMessage.NoMessageMap
import io.nekohasekai.group.exts.*
import io.nekohasekai.ktlib.core.defaultLog
import io.nekohasekai.ktlib.td.cli.database
import io.nekohasekai.ktlib.td.core.TdException
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.*
import io.nekohasekai.ktlib.td.extensions.*
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.isChatAdmin
import kotlinx.coroutines.*
import td.TdApi
import java.lang.Character.UnicodeScript

/**
 * 机器人检查
 * 由于诸多检查, 不应被删除服务消息覆盖.
 */
class BotCheckHandler : TdHandler() {

    val adAbs = arrayOf(
            "(广告|服务器|cdn|阿里云|腾讯云|高防|vps|独服|杜甫|引流|吸粉|增粉|加粉|关注|点赞|评论|电销|股民|资源|数据|博彩|亚博|棋牌|bc|菠菜|狗推|人事|兼职|招聘|招募|招代理)".toRegex(),
            "(.件套|黑卡|银行卡|对公|账户|公户|个户|私户|代收|代付|洗钱|资金|转账|套现|换汇|贷款|网贷|送货|跑分|卡商|卡王|查人|定位|开房|查档|社工|苹果签名|iOS签名|企业签名|手表|水鬼|租房|房源)".toRegex(),
            "(点我|加我|联系|咨询|头像|v信|加v)".toRegex(),
            "(真人|实名|认证|解封|担保|专业|实力|高端|专供|供应|批发|现货|货源|安排|菲|全新|一手|二手|团队|工作室|承接)".toRegex()
    )

    val adLike = arrayOf(
            "(usdt|莆田|高仿|大量|支付|暗网|黑产|灰产)".toRegex(),
            "(微信|电报|[tT][gG]|脸书|油管|ins|微信号|企业微信|QQ号|公众号|陌陌号)".toRegex(),
            "(出|售|卖|收|dd|滴滴|私|\\\\+v|vx)".toRegex()
    )

    fun String.reMatch(vararg reArr: Regex): HashSet<String> {

        val result = HashSet<String>()

        for (re in reArr) result.addAll(re.findAll(this).map { it.value })

        return result

    }

    val virusAbs = ".*.(cmd|bat|exe|ps*)".toRegex()
    val virusLike = ".*.(zip|rar|7z|apk)".toRegex()

    fun String.containsChinese() = any { UnicodeScript.of(it.toInt()) == UnicodeScript.HAN }

    lateinit var userFirstMessageMap: NoMessageMap

    override fun onLoad() {
        userFirstMessageMap = NoMessageMap(database)

        initFunction("del_me")
    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {
        if (!isUserAgentAvailable(chatId)) rejectFunction()

        userAgent!!.deleteChatMessagesFromUser(chatId, userId)
        userFirstMessageMap.fetch(chatId.toSupergroupId to userId).write(0)
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        if (userId == 0 || isChatAdmin(chatId, userId)) return
        val config = global.groupConfigs.fetch(chatId).value ?: return
        if (config.botCheck == 0) return

        val user = getUser(userId)

        val name = user.displayName.filterNot { it == ' ' }

        val adNameLike = ((name.reMatch(* adAbs) + name.reMatch(* adLike)).isNotEmpty() ||
                name.length > 10 ||
                name.count { isSymbol(it) } > 3) ||
                name.length in 3..4 && name.all { UnicodeScript.of(it.toInt()) != UnicodeScript.HAN }

        if (message.isServiceMessage) {
            if (message.content is TdApi.MessageChatJoinByLink) {
                if (adNameLike) {
                    GlobalScope.launch(Dispatchers.IO) {
                        delay(3000L)

                        // 闪进闪退
                        val targetStatus = getChatMember(chatId, userId).status
                        if (targetStatus.isMember) return@launch

                        when (config.botCheck) {
                            1 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusRestricted(
                                    true, 0, TdApi.ChatPermissions()
                            ))
                            2 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusBanned())
                            3 -> setChatMemberStatus(chatId, userId, TdApi.ChatMemberStatusBanned(((SystemClock.now() + 1 * Minutes) / 1000).toInt()))
                        }
                    }
                }
            }
        }

        val content = message.content

        // 首次加入检查

        var isFirstMessage = false

        val userFirstMessage = userFirstMessageMap.fetch(chatId.toSupergroupId to userId)

        if (userFirstMessage.value == null) {
            if (isUserAgentAvailable(chatId)) {
                try {
                    val userMessages = searchChatMessages(chatId, "", TdApi.MessageSenderUser(userId), 0, 0, 100, TdApi.SearchMessagesFilterEmpty(), 0)
                    isFirstMessage = userMessages.messages.none { !it.isServiceMessage && it.date < message.date }
                    if (!isFirstMessage) {
                        userFirstMessage.set(userMessages.messages.filter { !it.isServiceMessage }.minByOrNull { it.date }!!.date)
                    }
                } catch (e: TdException) {
                    defaultLog.warn(e)
                }
            }
        } else {
            isFirstMessage = userFirstMessage.value!! - message.date < 10 * 60
        }

        if (userFirstMessage.value == null) {
            isFirstMessage = true
            userFirstMessage.set(message.date)
        }

        if (!isFirstMessage) return

        var isSafe = false
        run predict@{
            if (content is TdApi.MessageText) {
                for (entity in content.text.entities) {
                    when (entity.type) {
                        is TdApi.TextEntityTypeUrl,
                        is TdApi.TextEntityTypeTextUrl,
                        is TdApi.TextEntityTypeMention -> return@predict
                    }
                }
                isSafe = true
            }
        }

        println("[${getChat(chatId).title}] ${getUser(userId).displayNameFormatted}: ${message.textOrCaption ?: content.javaClass.simpleName.substringAfter("Message")}")

        if (!isSafe) {
            println("Unsafe")
            userFirstMessage.set(0)
            sudo delete message
        }

    }

}
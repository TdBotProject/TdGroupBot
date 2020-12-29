package io.nekohasekai.group

import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.database.GroupConfigs
import io.nekohasekai.group.database.OptionMessages
import io.nekohasekai.group.database.UserFirstMessage
import io.nekohasekai.group.handler.*
import io.nekohasekai.group.handler.special.SP1
import io.nekohasekai.group.manage.GroupOptions
import io.nekohasekai.group.manage.OptionsFunction
import io.nekohasekai.ktlib.core.defaultLog
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.db.IdTableCacheMap
import io.nekohasekai.ktlib.db.forceCreateTables
import io.nekohasekai.ktlib.td.cli.TdCli
import io.nekohasekai.ktlib.td.core.TdBridge
import io.nekohasekai.ktlib.td.core.TdClient
import io.nekohasekai.ktlib.td.core.persists.store.DatabasePersistStore
import io.nekohasekai.ktlib.td.extensions.fromPrivate
import io.nekohasekai.ktlib.td.extensions.htmlLink
import io.nekohasekai.ktlib.td.i18n.*
import io.nekohasekai.ktlib.td.i18n.store.DatabaseLocaleStore
import io.nekohasekai.ktlib.td.i18n.store.InMemoryLocaleStore
import io.nekohasekai.ktlib.td.i18n.store.LocaleStore
import io.nekohasekai.ktlib.td.utils.commands.GetIdCommand
import io.nekohasekai.ktlib.td.utils.formatMessage
import io.nekohasekai.ktlib.td.utils.makeHtml
import io.nekohasekai.ktlib.td.utils.makeMd
import io.nekohasekai.ktlib.td.utils.upsertCommands
import kotlinx.coroutines.delay
import td.TdApi
import java.io.File

open class TdGroupBot(tag: String = "main", name: String = "TdGroupBot") : TdCli(tag, name) {

    override val localeList = arrayOf("en_US", "zh_CN")

    companion object : TdGroupBot() {

        const val repoName = "TdGroupBot"
        const val repoUrl = "https://github.com/TdBotProject/TdGroupBot"
        const val licenseUrl = "https://github.com/TdBotProject/TdGroupBot/blob/dev/LICENSE"

        @JvmStatic
        fun main(args: Array<String>) {
            launch(args)
            loadConfig()
            start()
        }

    }

    var admin = 0
    var reportUrl = "https://t.me/TdBotProject"
    var userAgentTag = ""
    var spamWatchKey = ""

    override fun onLoadConfig() {
        super.onLoadConfig()

        admin = intConfig("B0T_OWNER") ?: admin
        reportUrl = stringConfig("REPORT_URL") ?: reportUrl
        userAgentTag = stringConfig("USER_AGENT") ?: userAgentTag
        spamWatchKey = stringConfig("SPAM_WATCH_API_KEY") ?: spamWatchKey
    }

    override var configFile = File("group.yml")

    override fun onLoad() {

        super.onLoad()

        clientLog.debug("Init databases")

        initDatabase("group_data.db")

        database.write {

            forceCreateTables(
                OptionMessages,
                GroupConfigs,
                UserFirstMessage
            )

        }

        if (LocaleStore.store is InMemoryLocaleStore) {

            LocaleStore.setImplement(DatabaseLocaleStore(database))

        }

        persists.setImplement(DatabasePersistStore(database))

        initFunction("help")

        addHandler(LocaleSwitcher(DATA_SWITCH_LOCALE) { userId, chatId, message ->

            onLaunch(userId, chatId, message)

        })

        addHandler(GetIdCommand())
        addHandler(GroupOptions())
        addHandler(OptionsFunction())

        addHandler(FirstMessageHandler())

        addHandler(ChanelMessagesHandler())
        addHandler(SimpleAntiSpamHandler())
        addHandler(SpamWatchHandler())
        addHandler(DeleteServiceMessagesHandler())
        addHandler(MemberPolicyHandler())

        // 给别人定制的东西

        addHandler(SP1())

    }

    val groupConfigs by lazy { IdTableCacheMap(database, GroupConfig) }
    val optionMessages by lazy { OptionMessages.MessagesMap(database) }
    val optionChats by lazy { OptionMessages.ChatsMap(database) }

    var userAgent: TdClient? = null

    override suspend fun beforeLogin() {

        if (userAgentTag == "none") return

        clientLog.info("Launching UserAgent...")

        if (userAgentTag.isNotBlank()) {
            val agent = TdBridge.getClient(userAgentTag)
            if (agent != null) {
                userAgent = agent
            } else {
                clientLog.warn("USER_AGENT specified but not found, launch manually.")
            }
        }

        if (userAgent == null) userAgent = object : TdCli("$tag-agent") {
            override val loginType = LoginType.USER
        }.also {
            it.options = options
            it.dataDir = File(dataDir, "userAgent")
        }.apply {
            waitForLogin()
        }

    }

    override suspend fun onLogin() {

        upsertCommands(
            findHandler<LocaleSwitcher>().def(),
            findHandler<GroupOptions>().def(),
            HELP_COMMAND,
            CANCEL_COMMAND
        )

        scheduleGcAndOptimize()

    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {
        defaultLog.trace(formatMessage(message))

        super.onNewMessage(userId, chatId, message)
    }

    override suspend fun onLaunch(userId: Int, chatId: Long, message: TdApi.Message) {

        if (!message.fromPrivate) return

        val L = localeFor(userId)

        if (LocaleStore.localeRead(chatId) == null) {

            findHandler<LocaleSwitcher>().startSelect(L, chatId, true)

            return

        }

        sudo makeHtml L.LICENSE.input(repoName, licenseUrl, "Github Repo".htmlLink(repoUrl)) syncTo chatId

        delay(600L)

        sudo makeMd L.HELP_MSG sendTo chatId

    }

    override suspend fun onFunction(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        function: String,
        param: String,
        params: Array<String>
    ) {

        val L = localeFor(userId)

        sudo makeMd L.HELP_MSG sendTo chatId

    }

    override suspend fun onUndefinedFunction(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        function: String,
        param: String,
        params: Array<String>
    ) {
        if (!message.fromPrivate) {
            rejectFunction()
        }
        super.onUndefinedFunction(userId, chatId, message, function, param, params)
    }

    override suspend fun skipFloodCheck(senderUserId: Int, message: TdApi.Message) = senderUserId == admin

}
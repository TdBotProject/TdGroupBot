package io.nekohasekai.group

import io.nekohasekai.group.database.*
import io.nekohasekai.group.handler.ChanelMessagesHandler
import io.nekohasekai.group.manage.GroupOptions
import io.nekohasekai.ktlib.core.getValue
import io.nekohasekai.ktlib.core.input
import io.nekohasekai.ktlib.db.IdTableCacheMap
import io.nekohasekai.ktlib.db.forceCreateTables
import io.nekohasekai.ktlib.td.cli.TdCli
import io.nekohasekai.ktlib.td.core.persists.store.DatabasePersistStore
import io.nekohasekai.ktlib.td.extensions.fromPrivate
import io.nekohasekai.ktlib.td.extensions.htmlLink
import io.nekohasekai.ktlib.td.i18n.*
import io.nekohasekai.ktlib.td.i18n.store.*
import io.nekohasekai.ktlib.td.utils.*
import io.nekohasekai.ktlib.td.utils.commands.GetIdCommand
import kotlinx.coroutines.delay
import td.TdApi
import java.io.File

open class TdGroupBot(tag: String = "main", name: String = "TdGroupBot") : TdCli(tag, name) {

    override val localeList = arrayOf("zh_CN")

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

    @Suppress("ObjectPropertyName")
    private var _admin = 0
    val admin by ::_admin

    override var configFile = File("group.yml")

    override fun onLoad() {

        super.onLoad()

        clientLog.debug("Init databases")

        initDatabase("group_data.db")

        database.write {

            forceCreateTables(
                    GroupConfigs,
                    LastPinneds
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

        addHandler(ChanelMessagesHandler())

    }

    val groupConfigs by lazy { IdTableCacheMap(database, GroupConfig) }
    val lastPinneds by lazy { IdTableCacheMap(database, LastPinned) }

    override fun onLoadConfig() {

        super.onLoadConfig()

        _admin = intConfig("B0T_OWNER") ?: _admin

    }

    override suspend fun onLogin() {

        upsertCommands(
                // findHandler<LocaleSwitcher>().def(),
                findHandler<GroupOptions>().def(),
                HELP_COMMAND,
                CANCEL_COMMAND
        )

    }

    override suspend fun onLaunch(userId: Int, chatId: Long, message: TdApi.Message) {

        if (!message.fromPrivate) return

        val L = localeFor(userId)

//        if (LocaleStore.localeRead(chatId) == null) {
//
//            findHandler<LocaleSwitcher>().startSelect(L, chatId, true)
//
//            return
//
//        }

        sudo makeHtml L.LICENSE.input(repoName, licenseUrl, "Github Repo".htmlLink(repoUrl)) syncTo chatId

        delay(600L)

        sudo makeMd L.HELP_MSG sendTo chatId

    }

    override suspend fun onFunction(userId: Int, chatId: Long, message: TdApi.Message, function: String, param: String, params: Array<String>, originParams: Array<String>) {

        val L = localeFor(userId)

        sudo makeMd L.HELP_MSG sendTo chatId

    }

    override suspend fun skipFloodCheck(senderUserId: Int, message: TdApi.Message) = senderUserId == admin.toInt()

}
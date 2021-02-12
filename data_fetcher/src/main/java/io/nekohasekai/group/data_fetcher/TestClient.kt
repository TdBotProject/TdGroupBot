package io.nekohasekai.group.data_fetcher

import com.hankcs.hanlp.dictionary.CustomDictionary
import io.nekohasekai.ktlib.db.pair.mkTable
import io.nekohasekai.ktlib.td.cli.TdCli

abstract class TestClient : TdCli() {

    fun start(args: Array<String>) {
        launch(args)
        loadConfig()
        start()
    }

    override fun onLoad() {
        options databaseDirectory "data/userAgent"

        initDatabase("data_fetcher.db")

        arrayOf(
            "推特",
            "电报",
            "币用",
            "四件套",
            "微信",
            "查档",
            "人肉"
        ).forEach {
            CustomDictionary.add("$it n 1024")
        }

    }

    override val loginType = LoginType.USER
    val settings = mkTable()

}
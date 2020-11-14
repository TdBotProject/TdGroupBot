package io.nekohasekai.spamwatch.models

data class BanRecord(
        var admin: Int = 0,
        var date: Int = 0,
        var id: Int = 0,
        var reason: String = ""
)
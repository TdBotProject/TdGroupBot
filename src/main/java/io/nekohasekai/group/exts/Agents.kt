package io.nekohasekai.group.exts

import io.nekohasekai.ktlib.td.core.TdException
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChatMember
import io.nekohasekai.ktlib.td.extensions.isAdmin

suspend fun TdHandler.isUserAgentAvailable(chatId: Long): Boolean {
    val userAgent = userAgent ?: return false
    return try {
        userAgent.getChatMember(chatId, userAgent.me.id).isAdmin
    } catch (e: TdException) {
        false
    }
}
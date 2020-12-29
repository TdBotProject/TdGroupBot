package io.nekohasekai.group.exts

import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.getChatMember
import io.nekohasekai.ktlib.td.core.raw.getChatOrNull
import io.nekohasekai.ktlib.td.extensions.isAdmin

suspend fun TdHandler.isUserAgentAvailable(chatId: Long): Boolean {
    return with(userAgent ?: return false) {
        getChatOrNull(chatId) ?: return false
        getChatMember(chatId, me.id).isAdmin
    }
}
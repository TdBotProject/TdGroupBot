package io.nekohasekai.group.exts

import io.nekohasekai.group.TdGroupBot
import io.nekohasekai.ktlib.td.core.TdHandler

val TdHandler.global get() = sudo as TdGroupBot
val TdHandler.userAgent get() = global.userAgent?.takeIf { it.auth }
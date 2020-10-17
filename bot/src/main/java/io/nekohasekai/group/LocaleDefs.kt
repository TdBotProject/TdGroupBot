
package io.nekohasekai.group

import io.nekohasekai.ktlib.td.i18n.LocaleController

private typealias L = LocaleController

private val L.group by L.receiveLocaleSet("group")
private val string = L.receiveLocaleString { group }

internal val L.HELP_MSG by string
internal val L.OPTIONS_DEF by string
internal val L.BOT_NOT_ADMIN by string
internal val L.SESSION_TIMEOUT by string
internal val L.MENU_SWITCH_PM by string
internal val L.MENU_SWITCH_BUTTON by string
internal val L.OPTIONS_MENU by string
internal val L.MENU_CHANNEL_MESSAGE by string
internal val L.MENU_CM_HELP by string
internal val L.CM_MODE_ASIS by string
internal val L.CM_MODE_DELETE by string
internal val L.CM_MODE_FORWARD_AND_DELETE by string
internal val L.CM_KEEP_PIN by string

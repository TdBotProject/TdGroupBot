package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.id.IdTable

object GroupConfigs : IdTable<Long>("group_config") {

    val groupId = long("group_id").entityId()
    val cmMode = integer("cm_mode").default(0)

    // 0->ignore, 1->mute, 2-> kick, 3->ban

    val botCheck = integer("bot_check").default(0)
    val spamWatch = integer("spam_watch").default(0)

    override val id = groupId
    override val primaryKey = PrimaryKey(groupId, name = "id")

}
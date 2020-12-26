package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.id.IdTable

object GroupConfigs : IdTable<Long>("group_config") {

    val groupId = long("group_id").entityId()
    val cmMode = integer("cm_mode").default(0)

    val deleteServiceMessages = integer("delete_service_messages").default(0)
    val simpleAs = integer("simple_as").default(0)
    val spamWatch = integer("spam_watch").default(0)

    val memberPolicy = integer("member_policy").default(0)

    override val id = groupId
    override val primaryKey = PrimaryKey(groupId, name = "id")

}
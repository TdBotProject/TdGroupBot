package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.id.IdTable

object GroupConfigs : IdTable<Long>("group_config") {

    val groupId = long("group_id").entityId()

    val cmMode = integer("cm_mode").default(0)

    override val id = groupId
    override val primaryKey = PrimaryKey(groupId, name = "id")

}
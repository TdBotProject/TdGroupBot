package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GroupConfig(id: EntityID<Long>) : Entity<Long>(id) {

    var cmMode by GroupConfigs.cmMode

    companion object : EntityClass<Long, GroupConfig>(GroupConfigs)

}
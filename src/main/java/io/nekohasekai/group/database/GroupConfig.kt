package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class GroupConfig(id: EntityID<Long>) : Entity<Long>(id) {

    var cmMode by GroupConfigs.cmMode

    var deleteServiceMessages by GroupConfigs.deleteServiceMessages
    var simpleAs by GroupConfigs.simpleAs
    var adName by GroupConfigs.adName
    var adContent by GroupConfigs.adContent

    var spamWatch by GroupConfigs.spamWatch

    var memberPolicy by GroupConfigs.memberPolicy

    companion object : EntityClass<Long, GroupConfig>(GroupConfigs)

}
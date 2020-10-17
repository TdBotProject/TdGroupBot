package io.nekohasekai.group.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class LastPinned(id: EntityID<Long>) : Entity<Long>(id) {

    var pinnedMessage by LastPinneds.pinnedMessage

    companion object : EntityClass<Long, LastPinned>(LastPinneds)

}
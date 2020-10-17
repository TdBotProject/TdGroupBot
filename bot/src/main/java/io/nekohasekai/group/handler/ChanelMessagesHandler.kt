package io.nekohasekai.group.handler

import cn.hutool.cache.impl.LFUCache
import io.nekohasekai.group.database.LastPinned
import io.nekohasekai.group.global
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.pinChatMessage
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.makeForward
import td.TdApi

class ChanelMessagesHandler : TdHandler() {

    val processed = LFUCache<Long, Boolean>(0, 3 * 1000)

    override suspend fun gc() {

        processed.clear()

    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        val config = global.groupConfigs.fetch(chatId).value ?: return

        val content = message.content

        if (content is TdApi.MessagePinMessage) {

            sudo delete message

            return

        } else if (message.senderChatId == 0L || message.senderChatId == message.chatId) return

        if (message.mediaAlbumId != 0L) {

            if (processed.containsKey(message.mediaAlbumId)) return

            processed.put(message.mediaAlbumId, true)

        }

        when (config.cmMode) {

            1 -> {

                sudo delete message

            }

            2 -> {

                sudo makeForward (message) syncTo chatId

                sudo delete message

            }

        }

        if (config.keepPin) {

            val lastPinned = global.lastPinneds.fetch(chatId).value

            pinChatMessage(chatId, lastPinned?.pinnedMessage ?: 0L, true)

        }

    }

    override suspend fun onChatPinnedMessage(chatId: Long, pinnedMessageId: Long) {

        val cache = global.lastPinneds.fetch(chatId)

        database.write {

            if (cache.value == null) {

                cache.value = LastPinned.new(chatId) { pinnedMessage = pinnedMessageId }

            } else if (cache.value!!.pinnedMessage != pinnedMessageId) {

                cache.value!!.pinnedMessage = pinnedMessageId
                cache.changed = true

            }

        }

    }

}
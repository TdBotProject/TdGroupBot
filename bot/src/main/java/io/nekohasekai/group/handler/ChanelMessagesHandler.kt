package io.nekohasekai.group.handler

import io.nekohasekai.group.database.LastPinned
import io.nekohasekai.group.global
import io.nekohasekai.ktlib.td.core.TdClient
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.forwardMessages
import io.nekohasekai.ktlib.td.core.raw.pinChatMessage
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.makeForward
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import td.TdApi
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask

class ChanelMessagesHandler : TdHandler() {

    val albumMessages = HashMap<Long, AlbumMessages>()

    class AlbumMessages {

        val messages = LinkedList<TdApi.Message>()
        var task: TimerTask? = null

    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {

        val config = global.groupConfigs.fetch(chatId).value ?: return

        val content = message.content

        if (userId == me.id && content is TdApi.MessagePinMessage) {

            sudo delete message

            return

        }

        if (message.senderChatId == 0L || message.senderChatId == message.chatId) return

        val mediaAlbumId = message.mediaAlbumId

        var inAlbum = false

        if (mediaAlbumId != 0L) {

            if (albumMessages[mediaAlbumId] == null) {

                inAlbum = true

                albumMessages[mediaAlbumId] = AlbumMessages()

            }

            albumMessages[mediaAlbumId]!!.apply {

                messages.add(message)

                task?.cancel()

                task = timerTask {

                    GlobalScope.launch(TdClient.eventsContext) {

                        albumMessages.remove(mediaAlbumId)

                        when (config.cmMode) {

                            1 -> {

                                delete(chatId, * messages.map { it.id }.toLongArray())

                            }

                            2 -> {

                                forwardMessages(chatId, chatId, messages.map { it.id }.toLongArray(), TdApi.MessageSendOptions(), sendCopy = false, removeCaption = false)

                                delete(chatId, * messages.map { it.id }.toLongArray())

                            }

                        }

                    }

                }.also {

                    TdClient.timer.schedule(it, 300L)

                }

            }

            if (!inAlbum) return

        }

        if (!inAlbum) {

            when (config.cmMode) {

                1 -> {

                    sudo delete message

                }

                2 -> {

                    sudo makeForward (message) syncTo chatId

                    sudo delete message

                }

            }

        }

        if (config.keepPin) {

            val lastPinned = global.lastPinneds.fetch(chatId).value?.pinnedMessage ?: 0L

            if (lastPinned == 0L) return

            pinChatMessage(chatId, lastPinned, true)

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
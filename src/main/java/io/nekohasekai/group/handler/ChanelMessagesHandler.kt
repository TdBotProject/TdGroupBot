package io.nekohasekai.group.handler

import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.td.core.TdClient
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.forwardMessages
import io.nekohasekai.ktlib.td.core.raw.unpinChatMessage
import io.nekohasekai.ktlib.td.extensions.senderChatId
import io.nekohasekai.ktlib.td.utils.delete
import io.nekohasekai.ktlib.td.utils.makeForward
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
        if (message.senderChatId == 0L || message.senderChatId == message.chatId) return
        val mediaAlbumId = message.mediaAlbumId

        if (mediaAlbumId != 0L) {

            if (config.cmMode in intArrayOf(1, 2)) {

                if (albumMessages[mediaAlbumId] == null) {
                    albumMessages[mediaAlbumId] = AlbumMessages()
                }

                albumMessages[mediaAlbumId]!!.apply {

                    messages.add(message)

                    task?.cancel()

                    task = timerTask {

                        GlobalScope.launch(TdClient.eventsContext) {

                            albumMessages.remove(mediaAlbumId)

                            when (config.cmMode) {

                                1 -> delete(chatId, * messages.map { it.id }.toLongArray())

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

            } else if (config.cmMode == 3 && message.isPinned) {

                delay(1000L)
                unpinChatMessage(chatId, message.id)

            }

            return

        }

        when (config.cmMode) {

            1 -> sudo delete message
            2 -> {
                sudo makeForward message syncTo chatId
                sudo delete message
            }
            3 -> {
                delay(1000L)
                unpinChatMessage(chatId, message.id)
            }

        }

    }

}
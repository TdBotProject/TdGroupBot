package io.nekohasekai.group.handler

import io.nekohasekai.group.exts.global
import io.nekohasekai.ktlib.nsfw.NSFW
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.fromPrivate
import io.nekohasekai.ktlib.td.extensions.htmlBold
import io.nekohasekai.ktlib.td.extensions.htmlCode
import io.nekohasekai.ktlib.td.utils.download
import io.nekohasekai.ktlib.td.utils.make
import io.nekohasekai.ktlib.td.utils.makeHtml
import td.TdApi

class NSFWHandler : TdHandler() {

    override fun onLoad() {
        NSFW.loadModel(global.cacheDir)
    }

    override suspend fun onNewMessage(userId: Int, chatId: Long, message: TdApi.Message) {
        if (!message.fromPrivate) return

        val photo = when (val content = message.content) {
            is TdApi.MessagePhoto -> content.photo.sizes[0].photo
            is TdApi.MessageVideo -> content.video.thumbnail?.file
            is TdApi.MessageAnimation -> content.animation.thumbnail?.file
            is TdApi.MessageSticker -> content.sticker.sticker
            else -> null
        } ?: return

        val status = sudo make "Downloading..." syncReplyTo message

        val photoFile = download(photo)

        sudo make "Predicting..." syncEditTo status

        sudo makeHtml NSFW.predict(photoFile)[0].map { "${it.key.htmlBold}: ${(it.value * 100).toInt().htmlCode}%" }
            .joinToString("\n") syncEditTo status

        finishEvent()

    }

}
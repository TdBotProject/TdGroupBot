package io.nekohasekai.group.handler

import cn.hutool.core.util.CharUtil
import com.hankcs.hanlp.utility.TextUtility
import io.nekohasekai.group.exts.checkTess
import io.nekohasekai.group.exts.global
import io.nekohasekai.group.exts.imageToString
import io.nekohasekai.group.exts.readQR
import io.nekohasekai.ktlib.nsfw.NSFW
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.extensions.fromPrivate
import io.nekohasekai.ktlib.td.extensions.htmlBold
import io.nekohasekai.ktlib.td.extensions.htmlCode
import io.nekohasekai.ktlib.td.utils.download
import io.nekohasekai.ktlib.td.utils.make
import io.nekohasekai.ktlib.td.utils.makeHtml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import td.TdApi

class ImageHandler : TdHandler() {

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

        GlobalScope.launch(Dispatchers.IO) {
            sudo make "Predicting..." syncEditTo status
            sudo makeHtml NSFW.predict(photoFile)[0].map { "${it.key.htmlBold}: ${(it.value * 100).toInt().htmlCode}%" }
                .joinToString("\n") syncEditTo status
        }

        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                if (checkTess()) {
                    var result = imageToString(photoFile)
                    if (result.isNotBlank()) {
                        result = result.filter { CharUtil.isLetter(it) || TextUtility.isChinese(it) }
                            .toCharArray().let(::String)
                        sudo makeHtml "OCR: " + result.htmlCode replyTo message
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                val qrText = readQR(photoFile)
                if (!qrText.isNullOrBlank()) sudo makeHtml "Link: " + qrText.htmlCode replyTo message
            }
        }

        finishEvent()

    }

}
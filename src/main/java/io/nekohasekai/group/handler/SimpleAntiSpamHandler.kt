package io.nekohasekai.group.handler

import cn.hutool.cache.impl.LFUCache
import cn.hutool.core.io.resource.ResourceUtil
import cn.hutool.core.util.CharUtil
import com.hankcs.hanlp.utility.TextUtility
import io.nekohasekai.group.database.GroupConfig
import io.nekohasekai.group.exts.*
import io.nekohasekai.ktlib.cc.CCConverter
import io.nekohasekai.ktlib.cc.CCTarget
import io.nekohasekai.ktlib.core.mkLog
import io.nekohasekai.ktlib.nsfw.NSFW
import io.nekohasekai.ktlib.td.core.TdHandler
import io.nekohasekai.ktlib.td.core.raw.deleteChatMessagesFromUser
import io.nekohasekai.ktlib.td.core.raw.getUser
import io.nekohasekai.ktlib.td.core.raw.getUserFullInfo
import io.nekohasekai.ktlib.td.core.raw.reportSupergroupSpam
import io.nekohasekai.ktlib.td.extensions.*
import io.nekohasekai.ktlib.td.utils.*
import kotlinx.coroutines.*
import td.TdApi
import java.util.*
import kotlin.collections.ArrayList

@Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
class SimpleAntiSpamHandler : TdHandler(), FirstMessageHandler.Interface {

    val log = mkLog("AntiSpam")

    val virusAbs = ".*\\.(cmd|bat|exe|ps1|rar|zip|lha|lzh)".toRegex()

    companion object {

        val adNames = ResourceUtil.readUtf8Str("ad_name.txt").split("\n").toHashSet()
        val adContents = ResourceUtil.readUtf8Str("ad_content.txt").split("\n").toHashSet()
        val adContacts = ResourceUtil.readUtf8Str("ad_contact.txt").split("\n").toHashSet()

        val spamDict = LFUCache<Int, Unit>(-1, 1 * Days)
        val cc = CCConverter(CCTarget.SP)

    }

    override suspend fun onFirstMessage(
        userId: Int,
        chatId: Long,
        message: TdApi.Message,
        config: GroupConfig
    ): Boolean {

        val action = config.simpleAs.takeIf { it > 0 } ?: return false
        val content = message.content

        suspend fun exec(): Nothing {
            spamDict.put(userId, Unit)

            when (action) {
                1 -> muteMember(chatId, userId)
                2 -> banChatMember(chatId, userId)
                3 -> kickMember(chatId, userId)
            }
            sudo delete message
            if (isUserAgentAvailable(chatId)) with(userAgent!!) {
                reportSupergroupSpam(chatId.toSupergroupId, userId, longArrayOf(message.id))
                deleteChatMessagesFromUser(chatId, userId)
            }
            finishEvent()
        }

        val user = getUser(userId)
        val userInfo = getUserFullInfo(userId)

        val displayName = user.displayName
            .let { cc.convert(it).toLowerCase() }
            .filter { CharUtil.isLetter(it) || TextUtility.isChinese(it) }

        if (message.isServiceMessage) {
            val users = LinkedList<TdApi.User>()

            when (content) {
                is TdApi.MessageChatAddMembers -> for (memberUserId in content.memberUserIds) {
                    if (memberUserId == userId) {
                        users.add(user)
                    } else {
                        users.add(getUser(memberUserId))
                    }
                }
                is TdApi.MessageChatJoinByLink -> users.add(user)
                else -> return false
            }

            var ex = spamDict.containsKey(userId)
            if (!ex) pr@ for (u in users) {
                if (u.isBot) ex = true else {
                    val name = u.displayName
                        .let { cc.convert(it).toLowerCase() }
                        .filter { CharUtil.isLetter(it) || TextUtility.isChinese(it) }

                    for (adContact in adContacts) {
                        if (name.contains(adContact)) {
                            ex = true

                            postLog(chatId, userId, "Type", "Ad Contact", "Contact", adContact)
                            break@pr
                        }
                    }
                    if (!config.adName) for (adName in adNames) {
                        if (name.contains(adName)) {
                            ex = true

                            postLog(chatId, userId, "Type", "Ad Name")
                            break@pr
                        }
                    }
                }
            }
            if (ex) {
                for (u in users) {
                    when (action) {
                        1 -> muteMember(chatId, u.id)
                        2 -> banChatMember(chatId, u.id)
                        3 -> kickMember(chatId, u.id)
                    }
                }
            }
            if (ex) exec()

            return true
        }

        if (content is TdApi.MessageDocument) {
            if (content.document.fileName.matches(virusAbs)) {
                log.debug("virus like file detected")
                postLog(message, "Type", "Bad File")
                exec()
            } else {
                log.debug("else file: ${content.document.fileName}")
            }
        } else if (content is TdApi.MessageContact) {
            postLog(message, "Type", "Contact")
            log.debug("content detected: ${content.contact.displayName}")
            exec()
        } else if (message.forwardInfo != null &&
            (message.textOrCaption == null ||
                    message.textOrCaption!!.count { CharUtil.isEmoji(it) } > 4)
        ) {
            postLog(message, "Type", "Bad Forward")
            log.debug("forward detected")
            exec()
        }

        for (adContact in adContacts) {
            if (displayName.contains(adContact)) {
                postLog(message, "Type", "Ad Contact", "Contact", adContact)
                exec()
            }
        }

        if (config.adName) {

            for (adName in adNames) {
                if (displayName.contains(adName)) {
                    postLog(message, "Type", "Ad Name", "Match", adName)
                    exec()
                }
            }

            val bio = userInfo.bio
                .let { cc.convert(it).toLowerCase() }
                .filter { CharUtil.isLetter(it) || TextUtility.isChinese(it) }

            for (adName in adNames) {
                if (bio.contains(adName)) {
                    postLog(message, "Type", "Ad Desc", "Match", adName)
                    exec()
                }
            }

        }

        val text = message.textOrCaptionObj
        if (text != null) {
            if (text.text.count { CharUtil.isEmoji(it) } > 4) {
                postLog(message, "Type", "Emoji")
                exec()
            }

            for (entity in text.entities) {
                if (entity.type is TdApi.TextEntityTypeMention || entity.type is TdApi.TextEntityTypeTextUrl) {
                    val link = text.text.substring(entity.offset, entity.offset + entity.length)

                    postLog(message, "Type", "Link", "Link", link)
                    exec()
                }
            }

            val txt = text.text
                .let { cc.convert(it).toLowerCase() }
                .filter { CharUtil.isLetter(it) || TextUtility.isChinese(it) }

            for (adContact in adContacts) {
                if (txt.contains(adContact)) {
                    postLog(message, "Type", "Ad Contact", "Contact", adContact)
                    exec()
                }
            }

            if (config.adContent) {

                for (adContent in adContents) {
                    if (txt.contains(adContent)) {
                        postLog(message, "Type", "Ad Content", "Match", adContent)
                        exec()
                    }
                }

            }
        }

        val deferreds = ArrayList<Deferred<Pair<(suspend () -> Unit)?, Boolean>>>()

        if (user.profilePhoto != null) {

            val profilePhoto = GlobalScope.async(Dispatchers.IO) {
                download(user.profilePhoto!!.big)
            }

            deferreds.add(GlobalScope.async(Dispatchers.IO) {
                val qrText = readQR(profilePhoto.await())
                suspend {
                    postLog(
                        message,
                        "Type",
                        "Qr Code (User Photo)",
                        "QR Text",
                        qrText!!
                    )
                } to !qrText.isNullOrBlank()
            })

            if (checkTess()) deferreds.add(
                GlobalScope.async(
                    Dispatchers.IO
                ) {
                    val result = imageToString(profilePhoto.await())
                        .let { cc.convert(it).toLowerCase() }
                        .filter { TextUtility.isChinese(it) || CharUtil.isLetter(it) }

                    for (adContact in adContacts) {
                        if (result.contains(adContact)) {
                            return@async suspend {
                                postLog(
                                    message,
                                    "Type",
                                    "Ad Contact",
                                    "OCR",
                                    result,
                                    "Contact",
                                    adContact
                                )
                            } to true
                        }
                    }

                    if (config.adName) for (adName in adNames) {
                        if (result.contains(adName)) {
                            return@async suspend {
                                postLog(
                                    message,
                                    "Type",
                                    "Ad Name",
                                    "OCR",
                                    result,
                                    "Match",
                                    adName
                                )
                            } to true
                        }
                    }
                    null to false
                })

        }

        val photoDocument = if (content is TdApi.MessagePhoto) {
            content.photo.sizes.map { it.photo }.maxByOrNull { it.expectedSize }!!
        } else if (content is TdApi.MessageVideo && content.video.thumbnail != null) {
            content.video.thumbnail!!.file
        } else if (content is TdApi.MessageVideoNote && content.videoNote.thumbnail != null) {
            content.videoNote.thumbnail!!.file
        } else null

        if (photoDocument != null) {

            val photoFile = GlobalScope.async { download(photoDocument) }

            deferreds.add(GlobalScope.async(Dispatchers.IO) {
                val qrText = readQR(photoFile.await())
                suspend { postLog(message, "Type", "Qr Code", "QR Text", qrText!!) } to !qrText.isNullOrBlank()
            })

            deferreds.add(GlobalScope.async(Dispatchers.IO) {
                val nsfw = NSFW.predict(photoFile.await())[0]
                suspend { postLog(message, "Type", "PORN", "Value", "${nsfw[NSFW.PORN]}") } to (nsfw[NSFW.PORN]!! > 0.6)
            })

            if (checkTess()) {
                deferreds.add(GlobalScope.async(Dispatchers.IO) {
                    val result = imageToString(photoFile.await())
                        .let { cc.convert(it).toLowerCase() }
                        .filter { TextUtility.isChinese(it) || CharUtil.isLetter(it) }

                    for (adContact in adContacts) {
                        if (result.contains(adContact)) {
                            return@async suspend {
                                postLog(
                                    message,
                                    "Type",
                                    "Ad Contact",
                                    "OCR", result,
                                    "Contact",
                                    adContact
                                )
                            } to true
                        }
                    }

                    for (adName in adContents) {
                        if (result.contains(adName)) {
                            return@async suspend {
                                postLog(
                                    message,
                                    "Type",
                                    "Ad Content",
                                    "OCR",
                                    result,
                                    "Match",
                                    adName
                                )
                            } to true
                        }
                    }
                    null to false
                })
            }
        }

        for ((print, result) in deferreds.awaitAll()) {
            if (!result) continue
            print?.invoke()
            exec()
        }

        val isSafe = content is TdApi.MessageSticker ||
                content is TdApi.MessageAudio ||
                content is TdApi.MessageAnimation ||
                message.forwardInfo == null &&
                content is TdApi.MessageText &&
                content.text.entities.isEmpty() &&
                content.text.text.count { CharUtil.isEmoji(it) } < 5

        if (!isSafe) {
            postLog(message, "Type", "Unsafe")
            sudo delete message
            return true
        } else if (spamDict.containsKey(userId)) {
            postLog(message, "Type", "Cache")
            sudo delete message
            return true
        }

        return false
    }

}
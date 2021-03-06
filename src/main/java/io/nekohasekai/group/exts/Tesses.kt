package io.nekohasekai.group.exts

import cn.hutool.core.util.RuntimeUtil
import io.nekohasekai.ktlib.core.defaultLog
import java.io.File

private lateinit var tessInited: Any

fun checkTess(): Boolean {
    if (::tessInited.isInitialized) {
        return tessInited as Boolean
    }

    try {
        val proc = Runtime.getRuntime().exec(arrayOf("tesseract", "--version"))
        tessInited = proc.waitFor() == 0
        return tessInited as Boolean
    } catch (ignored: Exception) {
    }
    tessInited = false
    return false
}

fun imageToString(imageFile: File, language: String = "chi_sim+chi_tra"): String {
    val result = RuntimeUtil.getResult(
        Runtime.getRuntime().exec(
            arrayOf(
                "tesseract", "-l", language, imageFile.canonicalPath, "-"
            )
        )
    )
    defaultLog.debug("[tesseract] " + imageFile.path + " => " + result.trim())
    return result
}
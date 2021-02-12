package io.nekohasekai.group.exts

import cn.hutool.core.util.CharUtil


fun String.removeNonASCII(maxLength: Int = 10): String {

    var result = map { if (CharUtil.isAsciiPrintable(it)) it else '■' }
        .toCharArray().let(::String)

    if (result.length > maxLength) result = result.substring(0, maxLength) + "..."

    return result

}

fun isSymbol(ch: Char): Boolean {
    if (isCnSymbol(ch)) return true
    if (isEnSymbol(ch)) return true
    if (ch.toInt() in 0x2010..0x2017) return true
    if (ch.toInt() in 0x2020..0x2027) return true
    if (ch.toInt() in 0x2B00..0x2BFF) return true
    if (ch.toInt() in 0xFF03..0xFF06) return true
    if (ch.toInt() in 0xFF08..0xFF0B) return true
    if (ch.toInt() == 0xFF0D || ch.toInt() == 0xFF0F) return true
    if (ch.toInt() in 0xFF1C..0xFF1E) return true
    if (ch.toInt() == 0xFF20 || ch.toInt() == 0xFF65) return true
    if (ch.toInt() in 0xFF3B..0xFF40) return true
    if (ch.toInt() in 0xFF5B..0xFF60) return true
    if (ch.toInt() == 0xFF62 || ch.toInt() == 0xFF63) return true
    return ch.toInt() == 0x0020 || ch.toInt() == 0x3000
}

fun isCnSymbol(ch: Char): Boolean {
    if (ch.toInt() in 0x3004..0x301C) return true
    return ch.toInt() in 0x3020..0x303F
}

fun isEnSymbol(ch: Char): Boolean {
    if (ch.toInt() == 0x40) return true
    if (ch.toInt() == 0x2D || ch.toInt() == 0x2F) return true
    if (ch.toInt() in 0x23..0x26) return true
    if (ch.toInt() in 0x28..0x2B) return true
    if (ch.toInt() in 0x3C..0x3E) return true
    if (ch.toInt() in 0x5B..0x60) return true
    return ch.toInt() in 0x7B..0x7E
}

fun isPunctuation(ch: Char): Boolean {
    if (isCjkPunc(ch)) return true
    if (isEnPunc(ch)) return true
    if (ch.toInt() in 0x2018..0x201F) return true
    if (ch.toInt() == 0xFF01 || ch.toInt() == 0xFF02) return true
    if (ch.toInt() == 0xFF07 || ch.toInt() == 0xFF0C) return true
    if (ch.toInt() == 0xFF1A || ch.toInt() == 0xFF1B) return true
    if (ch.toInt() == 0xFF1F || ch.toInt() == 0xFF61) return true
    if (ch.toInt() == 0xFF0E) return true
    return ch.toInt() == 0xFF65
}

fun isEnPunc(ch: Char): Boolean {
    if (ch.toInt() in 0x21..0x22) return true
    if (ch.toInt() == 0x27 || ch.toInt() == 0x2C) return true
    if (ch.toInt() == 0x2E || ch.toInt() == 0x3A) return true
    return ch.toInt() == 0x3B || ch.toInt() == 0x3F
}

fun isCjkPunc(ch: Char): Boolean {
    if (ch.toInt() in 0x3001..0x3003) return true
    return ch.toInt() in 0x301D..0x301F
}

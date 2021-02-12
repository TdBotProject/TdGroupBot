package io.nekohasekai.group.exts

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.File
import javax.imageio.ImageIO

val decoder = QRCodeReader()

fun readQR(photoFile: File): String? {
    val source = BufferedImageLuminanceSource(ImageIO.read(photoFile))

    return runCatching {
        decoder.decode(
            BinaryBitmap(GlobalHistogramBinarizer(source)), mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to null
            )
        ).text
    }.recoverCatching {
        decoder.decode(
            BinaryBitmap(GlobalHistogramBinarizer(source.invert())), mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to null
            )
        ).text
    }.getOrNull()
}
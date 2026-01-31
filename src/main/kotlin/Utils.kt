package io.github.d0ublew.bapp.starter

import java.util.Base64
import javax.swing.UIManager

fun encode(bytes: ByteArray): String {
    return Base64.getEncoder().encodeToString(bytes)
}

fun isDarkTheme(): Boolean {
    val bg = UIManager.getColor("Panel.background") ?: return false
    val brightness = (bg.red + bg.green + bg.blue) / 3
    return brightness < 128
}

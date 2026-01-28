package io.github.d0ublew.bapp.starter

import java.util.Base64

fun encode(bytes: ByteArray): String {
    return Base64.getEncoder().encodeToString(bytes)
}
package com.stanley.tcpip.utils.extensions

internal fun ByteArray.readShort(index: Int) = this[index].toInt().and(0xFF)
    .shl(8).or(this[index + 1].toInt().and(0xFF)).toShort()

internal fun ByteArray.readInt(index: Int) = this[index].toInt().and(0xFF).shl(24)
    .or(this[index + 1].toInt().and(0xFF).shl(16))
    .or(this[index + 2].toInt().and(0xFF).shl(8))
    .or(this[index + 3].toInt().and(0xFF))

internal fun ByteArray.writeShort(index: Int, value: Short) {
    this[index] = value.toInt().shr(8).toByte()
    this[index + 1] = value.toInt().toByte()
}

internal fun ByteArray.writeInt(index: Int, value: Int) {
    this[index] = value.shr(24).toByte()
    this[index + 1] = value.shr(16).toByte()
    this[index + 2] = value.shr(8).toByte()
    this[index + 3] = value.toByte()
}

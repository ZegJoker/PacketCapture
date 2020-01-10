package com.stanley.tcpip.utils.extensions

 internal fun ByteArray.readShort(index: Int) = this[index].toInt()
    .shl(8).or(this[index + 1].toInt().and(0x0FF)).toShort()

 internal fun ByteArray.readInt(index: Int) = this[index].toInt().shl(24)
    .or(this[index + 1].toInt().shl(16))
    .or(this[index + 2].toInt().shl(8))
    .or(this[index + 3].toInt())

 internal fun ByteArray.writeShort(index: Int, value: Short) {
    this[index] = value.toInt().shr(8).and(0x0FF).toByte()
    this[index + 1] = value.toInt().and(0x0FF).toByte()
}

internal fun ByteArray.writeInt(index: Int, value: Int) {
    this[index] = value.shr(24).and(0x0FF).toByte()
    this[index + 1] = value.shr(16).and(0x0FF).toByte()
    this[index + 2] = value.shr(8).and(0x0FF).toByte()
    this[index + 3] = value.and(0x0FF).toByte()
}

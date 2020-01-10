package com.stanley.tcpip.constants

internal object IPHeaderOffsets {
    /**
     * 前4位
     */
    const val VERSION = 0
    /**
     * 后4位
     */
    const val HEADER_LENGTH = 0
    const val TYPE_OF_SERVICE = 1
    const val TOTAL_LENGTH = 2
    const val IDENTIFICATION = 4
    /**
     * 前3位
     */
    const val FLAGS = 6
    /**
     * 后5位以及之后的8位(1个字节)
     */
    const val FRAGMENT_OFFSET = 6
    const val TTL = 8
    const val PROTOCOL = 9
    const val CHECKSUM = 10
    const val SOURCE_ADDRESS = 12
    const val DEST_ADDRESS = 16
    const val OPTIONS = 20
}
package com.stanley.tcpip.constants

internal object TCPHeaderOffsets {
    const val SOURCE_PORT = 0
    const val DEST_PORT = 2
    const val SEQ_NUM = 4
    const val ACK_NUM = 8
    /**
     * 前4位
     */
    const val DATA_OFFSET = 12
    /**
     * 后6位，依次为URG, ACK, PSH, RST, SYN, FIN
     */
    const val CODE = 13
    const val WINDOW = 14
    const val CHECKSUM = 16
    const val URGENT_POINTER = 18
    /**
     * 注意： 若options字段不为4个字节则需在其后补充0使其至4个字节
     */
    const val OPTIONS = 20
}
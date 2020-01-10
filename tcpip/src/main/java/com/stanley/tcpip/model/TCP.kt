package com.stanley.tcpip.model

import com.stanley.tcpip.constants.TCPHeaderOffsets
import com.stanley.tcpip.utils.extensions.readInt
import com.stanley.tcpip.utils.extensions.readShort
import com.stanley.tcpip.utils.extensions.writeInt
import com.stanley.tcpip.utils.extensions.writeShort

/**
 * TCP协议
 */
class TCP(val ip: IP): Packet() {

    var sourcePort = ip.packet.readShort(ip.headerLength + TCPHeaderOffsets.SOURCE_PORT)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + TCPHeaderOffsets.SOURCE_PORT, value)
        }
    var destPort = ip.packet.readShort(ip.headerLength + TCPHeaderOffsets.DEST_PORT)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + TCPHeaderOffsets.DEST_PORT, value)
        }
    var seqNum = ip.packet.readInt(ip.headerLength + TCPHeaderOffsets.SEQ_NUM)
        set(value) {
            field = value
            ip.packet.writeInt(ip.headerLength + TCPHeaderOffsets.SEQ_NUM, value)
        }
    var ackNum = ip.packet.readInt(ip.headerLength + TCPHeaderOffsets.ACK_NUM)
        set(value) {
            field = value
            ip.packet.writeInt(ip.headerLength + TCPHeaderOffsets.ACK_NUM, value)
        }
    var dataOffset =
        ip.packet[ip.headerLength + TCPHeaderOffsets.DATA_OFFSET].toInt().shr(4).and(0x0F).times(4).toByte()
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.DATA_OFFSET] =
                value.toInt().div(4).shl(4).toByte()
        }
    var URG = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().shr(5).and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x1F)
                    .or(value.shl(5)).toByte()
        }
    var ACK = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().shr(4).and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x2F)
                    .or(value.shl(4)).toByte()
        }
    var PSH = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().shr(3).and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x37)
                    .or(value.shl(3)).toByte()
        }
    var RST = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().shr(2).and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x3B)
                    .or(value.shl(2)).toByte()
        }
    var SYN = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().shr(1).and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x3D)
                    .or(value.shl(1)).toByte()
        }
    var FIN = ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x01)
        set(value) {
            field = value
            ip.packet[ip.headerLength + TCPHeaderOffsets.CODE] =
                ip.packet[ip.headerLength + TCPHeaderOffsets.CODE].toInt().and(0x3E).or(value)
                    .toByte()
        }
    var window = ip.packet.readShort(ip.headerLength + TCPHeaderOffsets.WINDOW)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + TCPHeaderOffsets.WINDOW, value)
        }
    var checksum = ip.packet.readShort(ip.headerLength + TCPHeaderOffsets.CHECKSUM)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + TCPHeaderOffsets.CHECKSUM, value)
        }
    var urgentPointer = ip.packet.readShort(ip.headerLength + TCPHeaderOffsets.URGENT_POINTER)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + TCPHeaderOffsets.URGENT_POINTER, value)
        }
    var options
        set(value) {
            value.copyInto(ip.packet, ip.headerLength + TCPHeaderOffsets.OPTIONS)
        }
        get() = ip.packet.copyOfRange(
            ip.headerLength + TCPHeaderOffsets.OPTIONS,
            dataOffset.toInt()
        )
    var data
        set(value) {
            value.copyInto(ip.packet, ip.headerLength + dataOffset.toInt())
        }
        get() = ip.packet.copyOfRange(ip.headerLength + dataOffset.toInt(), ip.totalLength.toInt())
}
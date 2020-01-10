package com.stanley.tcpip.model

import com.stanley.tcpip.constants.UDPHeaderOffsets
import com.stanley.tcpip.utils.extensions.readShort
import com.stanley.tcpip.utils.extensions.writeShort

/**
 * UDP协议
 */
class UDP(val ip: IP): Packet() {

    companion object {
        /**
         * UDP header length is constantly 8
         */
        const val UDP_HEADER_TOTAL_LENGTH = 8
    }

    var sourcePort = ip.packet.readShort(ip.headerLength + UDPHeaderOffsets.SOURCE_PORT)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + UDPHeaderOffsets.SOURCE_PORT, value)
        }
    var destPort = ip.packet.readShort(ip.headerLength + UDPHeaderOffsets.DEST_PORT)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + UDPHeaderOffsets.DEST_PORT, value)
        }
    var length = ip.packet.readShort(ip.headerLength + UDPHeaderOffsets.LENGTH)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + UDPHeaderOffsets.LENGTH, value)
        }
    var checksum = ip.packet.readShort(ip.headerLength + UDPHeaderOffsets.CHECKSUM)
        set(value) {
            field = value
            ip.packet.writeShort(ip.headerLength + UDPHeaderOffsets.CHECKSUM, value)
        }
    var data
        get() = ip.packet.copyOfRange(
            ip.headerLength + UDP_HEADER_TOTAL_LENGTH,
            ip.totalLength.toInt()
        )
        set(value) {
            value.copyInto(ip.packet, ip.headerLength + UDP_HEADER_TOTAL_LENGTH)
        }
}
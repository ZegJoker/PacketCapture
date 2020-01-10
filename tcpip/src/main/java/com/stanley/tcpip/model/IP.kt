package com.stanley.tcpip.model

import com.stanley.tcpip.constants.IPHeaderOffsets
import com.stanley.tcpip.utils.extensions.readInt
import com.stanley.tcpip.utils.extensions.readShort
import com.stanley.tcpip.utils.extensions.writeInt
import com.stanley.tcpip.utils.extensions.writeShort

/**
 * IP协议
 */
class IP(val packet: ByteArray): Packet() {

    var version: Byte = packet[IPHeaderOffsets.VERSION].toInt().shr(4).and(0x0F).toByte()
        set(value) {
            field = value
            packet[IPHeaderOffsets.VERSION] = value.toInt().shl(4)
                .or(packet[IPHeaderOffsets.HEADER_LENGTH].toInt().and(0x0F)).toByte()
        }
    var headerLength = packet[IPHeaderOffsets.HEADER_LENGTH].toInt().and(0x0F).times(4).toByte()
        set(value) {
            field = value
            packet[IPHeaderOffsets.HEADER_LENGTH] =
                packet[IPHeaderOffsets.VERSION].toInt().and(0xF0).or(value.toInt().div(4)).toByte()
        }
    var typeOfService = packet[IPHeaderOffsets.TYPE_OF_SERVICE]
        set(value) {
            field = value
            packet[IPHeaderOffsets.TYPE_OF_SERVICE] = value
        }
    var totalLength = packet.readShort(IPHeaderOffsets.TOTAL_LENGTH)
        set(value) {
            field = value
            packet.writeShort(IPHeaderOffsets.TOTAL_LENGTH, value)
        }
    var identification = packet.readShort(IPHeaderOffsets.IDENTIFICATION)
        set(value) {
            field = value
            packet.writeShort(IPHeaderOffsets.IDENTIFICATION, value)
        }
    var flags = packet[IPHeaderOffsets.FLAGS].toInt().shr(5).and(0x07).toByte()
        set(value) {
            field = value
            packet[IPHeaderOffsets.FLAGS] =
                value.toInt().shl(5).or(packet[IPHeaderOffsets.FRAGMENT_OFFSET].toInt().and(0x1F))
                    .toByte()
        }
    var fragmentOffset = packet[IPHeaderOffsets.FRAGMENT_OFFSET].toInt().and(0x1F).shl(8)
        .or(packet[IPHeaderOffsets.FRAGMENT_OFFSET + 1].toInt()).toShort()
        set(value) {
            field = value
            packet[IPHeaderOffsets.FRAGMENT_OFFSET] =
                packet[IPHeaderOffsets.FLAGS].toInt().and(0xE0).or(value.toInt().shr(8).and(0x1F))
                    .toByte()
            packet[IPHeaderOffsets.FRAGMENT_OFFSET + 1] = value.toInt().and(0xFF).toByte()
        }
    var ttl = packet[IPHeaderOffsets.TTL]
        set(value) {
            field = value
            packet[IPHeaderOffsets.TTL] = value
        }
    var protocol = packet[IPHeaderOffsets.PROTOCOL]
        set(value) {
            field = value
            packet[IPHeaderOffsets.PROTOCOL] = value
        }
    var checksum = packet.readShort(IPHeaderOffsets.CHECKSUM)
        set(value) {
            field = value
            packet.writeShort(IPHeaderOffsets.CHECKSUM, value)
        }
    var sourceAddress = packet.readInt(IPHeaderOffsets.SOURCE_ADDRESS)
        set(value) {
            field = value
            packet.writeInt(IPHeaderOffsets.SOURCE_ADDRESS, value)
        }
    var destAddress = packet.readInt(IPHeaderOffsets.DEST_ADDRESS)
        set(value) {
            field = value
            packet.writeInt(IPHeaderOffsets.DEST_ADDRESS, value)
        }
    var options
        set(value) {
            value.copyInto(packet, IPHeaderOffsets.OPTIONS)
        }
        get() = packet.copyOfRange(IPHeaderOffsets.OPTIONS, headerLength.toInt())
    var data
        set(value) {
            value.copyInto(packet, headerLength.toInt())
        }
        get() = packet.copyOfRange(headerLength.toInt(), totalLength.toInt())
}
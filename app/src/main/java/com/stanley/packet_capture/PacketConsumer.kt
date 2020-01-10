package com.stanley.packet_capture

import com.stanley.tcpip.model.Packet

interface PacketConsumer<T: Packet> {
    fun consumePacket(packet: T)
}
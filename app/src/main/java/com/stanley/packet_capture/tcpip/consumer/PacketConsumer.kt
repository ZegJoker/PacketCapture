package com.stanley.packet_capture.tcpip.consumer

import com.stanley.tcpip.model.Packet

interface PacketConsumer<T: Packet> {
    fun consumePacket(packet: T)
}
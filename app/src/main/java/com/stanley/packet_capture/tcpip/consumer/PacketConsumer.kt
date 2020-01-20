package com.stanley.packet_capture.tcpip.consumer

import com.stanley.tcpip.model.Packet
import java.io.Closeable

interface PacketConsumer<T: Packet>: Closeable {
    fun consumePacket(packet: T)
}
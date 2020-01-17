package com.stanley.packet_capture.tcpip.consumer

import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.UDP
import java.util.concurrent.ConcurrentLinkedQueue

class UDPPacketConsumer(private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>):
    PacketConsumer<UDP> {
    override fun consumePacket(packet: UDP) {
    }

}
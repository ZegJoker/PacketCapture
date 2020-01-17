package com.stanley.packet_capture.tcpip

import com.stanley.packet_capture.tcpip.consumer.TCPPacketConsumer
import com.stanley.packet_capture.tcpip.consumer.UDPPacketConsumer
import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.TCP
import com.stanley.tcpip.model.UDP
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue

class PacketDistributor(
    private val pendingReadPacketQueue: ConcurrentLinkedQueue<IP>,
    private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>
) : Closeable {

    private var running = false

    private val tcpConsumer by lazy {
        TCPPacketConsumer(
            pendingWritePacketQueue
        )
    }
    private val udpConsumer by lazy {
        UDPPacketConsumer(
            pendingWritePacketQueue
        )
    }

    fun startDispatch() {
        running = true
        startInternal()
    }

    fun stopDispatch() {
        running = false
    }

    private fun startInternal() {
        Thread {
            while (running) {
                val ip = pendingReadPacketQueue.poll()
                if (ip != null) {
                    dispatchPacket(ip)
                }
            }
        }.start()
    }

    private fun dispatchPacket(ip: IP) =
        when (ip.protocol) {
            ProtocolCodes.TCP -> dispatchTCPPacket(TCP(ip))
            ProtocolCodes.UDP -> {
                dispatchUDPPacket(UDP(ip))
            }
            else -> {
            }
        }


    private fun dispatchTCPPacket(tcp: TCP) {
        tcpConsumer.consumePacket(tcp)
    }

    private fun dispatchUDPPacket(udp: UDP) {
        udpConsumer.consumePacket(udp)
    }

    override fun close() {
        stopDispatch()
    }

}
package com.stanley.packet_capture

import com.stanley.tcpip.model.IP
import java.io.Closeable
import java.io.FileInputStream
import java.util.concurrent.ConcurrentLinkedQueue

class PacketReader(private val reader: FileInputStream): Closeable {
    val pendingPacketQueue by lazy { ConcurrentLinkedQueue<IP>() }

    fun readIPPacket() {
        val packet = ByteArray(Config.MTU_SIZE)
        val readSize = reader.read(packet)
        if (readSize > 0) {
            pendingPacketQueue.add(IP(if (readSize == Config.MTU_SIZE) packet else packet.copyOfRange(0, readSize)))
        }
    }

    override fun close() {
        pendingPacketQueue.clear()
        reader.close()
    }

}
package com.stanley.packet_capture

import android.util.Log
import com.stanley.tcpip.model.IP
import java.io.Closeable
import java.io.FileOutputStream
import java.lang.Exception
import java.util.concurrent.ConcurrentLinkedQueue

class PacketWriter(private val writer: FileOutputStream): Closeable {
    val pendingPacketQueue by lazy { ConcurrentLinkedQueue<IP>() }

    fun writePacket() {
        if (pendingPacketQueue.isNotEmpty()) {
            val ip = pendingPacketQueue.poll()
            if (ip != null) {
                try {
                    writer.write(ip.packet, 0, ip.totalLength.toInt())
                } catch (e: Exception) {
                    Log.e("Stanley", e.toString(), e)
                }
            }
        }
    }

    override fun close() {
        pendingPacketQueue.clear()
        writer.close()
    }
}
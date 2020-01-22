package com.stanley.packet_capture.tcpip

import android.util.Log
import com.stanley.packet_capture.utils.TAG
import com.stanley.tcpip.model.IP
import java.io.Closeable
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Stanley on 2020-01-10.
 */
class PacketWriter(private val writer: FileOutputStream): Closeable {
    val pendingPacketQueue by lazy { ConcurrentLinkedQueue<IP>() }

    fun writePacket() {
        if (pendingPacketQueue.isNotEmpty()) {
            val ip = pendingPacketQueue.poll()
            if (ip != null) {
                Log.d(TAG, "Write packet into client")
                writer.write(ip.packet, 0, ip.totalLength.toInt())
            }
        }
    }

    override fun close() {
        pendingPacketQueue.clear()
        writer.close()
    }
}
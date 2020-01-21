package com.stanley.packet_capture.tcpip.tunnel

import android.util.SparseArray
import androidx.core.util.forEach
import java.io.Closeable

/**
 * Created by Stanley on 2020-01-20.
 */
class RemoteCommunicator(private val tunnels: SparseArray<TCPTunnel>) : Thread(), Closeable {
    private var running = false
    override fun start() {
        if (running) return
        running = true
        super.start()
    }

    override fun run() {
        while (running) {
            tunnels.forEach { _, tunnel ->
                if (tunnel.socket.isConnected) {
                    if (tunnel.pendingWritePacketQueue.isNotEmpty()) {
                        val sendData = tunnel.pendingWritePacketQueue.poll()
                        tunnel.socket.getOutputStream().write(sendData)
                    }
                    val receiveData = ByteArray(tunnel.window)
                    val readSize = tunnel.socket.getInputStream().read(receiveData)
                    if (readSize > 0) {
                        tunnel.receiveData(receiveData.copyOfRange(0, readSize))
                    }
                }
            }

        }
    }

    override fun close() {
        running = false
    }

}
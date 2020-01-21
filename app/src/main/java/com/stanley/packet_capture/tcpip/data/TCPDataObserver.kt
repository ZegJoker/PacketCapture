package com.stanley.packet_capture.tcpip.data

import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnel
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnelSelector
import com.stanley.packet_capture.utils.TAG
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPDataObserver(private val tunnels: SparseArray<TCPTunnel>) : Thread(), Closeable {
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
                        val requestData = String(sendData!!)
                        Log.d(TAG, "send data: $requestData")
                        tunnel.socket.getOutputStream().write(sendData)
                    }
                    val receiveData = ByteArray(tunnel.window)
                    val readSize = tunnel.socket.getInputStream().read(receiveData)
                    if (readSize > 0) {
                        val responseData = String(receiveData, 0, readSize)
                        Log.d(TAG, "received data: $responseData")
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
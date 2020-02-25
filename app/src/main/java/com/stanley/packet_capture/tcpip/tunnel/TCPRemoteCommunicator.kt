package com.stanley.packet_capture.tcpip.tunnel

import android.os.SystemClock
import com.stanley.packet_capture.tcpip.constants.TCPStatus
import java.io.Closeable

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPRemoteCommunicator(private val tunnels: Map<Short, TCPTunnel>) : Thread(), Closeable {
    private var running = false
    override fun start() {
        if (running) return
        running = true
        super.start()
    }

    override fun run() {
        while (running) {
            val iterator = tunnels.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                val tunnel = next.value
                if (tunnel.socket.isConnected) {
                    tunnel.status = TCPStatus.TRANSFERRING_CONNECTED
                    if (tunnel.pendingWritePacketQueue.isNotEmpty()) {
                        val sendData = tunnel.pendingWritePacketQueue.poll()
                        tunnel.socket.getOutputStream().write(sendData!!)
                    }
                    val receiveData = ByteArray(tunnel.window)
                    val readSize = tunnel.socket.getInputStream().read(receiveData)
                    if (readSize > 0) {
                        tunnel.lastActiveTime = SystemClock.elapsedRealtime()
                        tunnel.receiveData(receiveData.copyOfRange(0, readSize))
                    }
                } else if(tunnel.status == TCPStatus.TRANSFERRING_CONNECTED) {
                    tunnel.closeTunnelFromServer()
                }
            }
        }
    }

    override fun close() {
        running = false
    }

}
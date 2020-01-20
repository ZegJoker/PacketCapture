package com.stanley.packet_capture.tcpip.data

import com.stanley.packet_capture.tcpip.tunnel.TCPTunnel
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnelSelector
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPDataObserver: Thread(), Closeable {

    override fun run() {
        while (TCPTunnelSelector.selector.isOpen) {
            val selected = TCPTunnelSelector.selector.select(2000)
            val selectedKeys = TCPTunnelSelector.selector.selectedKeys()
            selectedKeys.forEach {
                val channel = it.channel() as SocketChannel
                val tunnel = it.attachment() as TCPTunnel
                if (it.isWritable) {
                    if (tunnel.pendingWritePacketQueue.isNotEmpty()) {
                        channel.write(ByteBuffer.wrap(tunnel.pendingWritePacketQueue.poll()!!))
                    }
                }
                if (it.isReadable) {
                    val buffer = ByteBuffer.allocate(32737)
                    val readSize = channel.read(buffer)
                    val data = ByteArray(readSize)
                    buffer.get(data, 0, readSize)
                    tunnel.receiveData(data)
                }
            }
        }
    }

    override fun close() {
        TCPTunnelSelector.selector.close()
    }

}
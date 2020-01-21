package com.stanley.packet_capture.tcpip.data

import android.util.Log
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnel
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnelSelector
import com.stanley.packet_capture.utils.TAG
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPDataObserver(private val selector: Selector): Thread(), Closeable {

    override fun run() {
        while (selector.isOpen) {
            val selected = selector.select(2000)
            if (selected > 0) {
                val selectedKeys = selector.selectedKeys()
                selectedKeys.forEach {
                    val channel = it.channel() as SocketChannel
                    val tunnel = it.attachment() as TCPTunnel
                    if (it.isValid && it.isWritable && channel.isConnected) {
                        if (tunnel.pendingWritePacketQueue.isNotEmpty()) {
                            val sendData = tunnel.pendingWritePacketQueue.poll()
                            val requestData = String(sendData!!)
                            Log.d(TAG, "send data: $requestData")
                            channel.write(ByteBuffer.wrap(sendData))
                            if (tunnel.pendingWritePacketQueue.size == 0) {
                                channel.keyFor(selector).interestOps(channel.keyFor(selector).interestOps().and(SelectionKey.OP_WRITE.inv()))
                            }
                        }
                    }
                    if (it.isValid && it.isReadable && channel.isConnected) {
                        val buffer = ByteBuffer.allocate(32737)
                        val readSize = channel.read(buffer)
                        val data = ByteArray(readSize)
                        buffer.get(data, 0, readSize)
                        val responseData = String(data)
                        Log.d(TAG, "received data: $responseData")
                        tunnel.receiveData(data)
                    }
                }
            }
        }
    }

    override fun close() {
        TCPTunnelSelector.selector.close()
    }

}
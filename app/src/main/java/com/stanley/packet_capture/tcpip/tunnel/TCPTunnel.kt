package com.stanley.packet_capture.tcpip.tunnel

import com.stanley.packet_capture.tcpip.constants.TCPStatus
import com.stanley.tcpip.utils.intIpToStr
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.nio.channels.SelectionKey
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPTunnel(val sourceAddress: Int, val sourcePort: Int, val destAddress: Int, val destPort: Int): Tunnel {

    private val socket = Socket(Proxy.NO_PROXY)
    var status = TCPStatus.PREPARE
    var closeListener: Tunnel.Callback? = null
    var seqNum = 1
    var ackNum = 1
    val pendingWritePacketQueue =  ConcurrentLinkedQueue<ByteArray>()
    var tcpOption = ByteArray(0)

    override fun connect() {
        if (!socket.isConnected) {
            socket.connect(InetSocketAddress(intIpToStr(destAddress), destPort))
            socket.channel.register(TCPTunnelSelector.selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE, this)
        }
    }

    override fun transferData(data: ByteArray) {
        pendingWritePacketQueue.add(data)
    }

    override fun receiveData(data: ByteArray) {
        closeListener?.onDataReceived(this, data)
        seqNum += data.size
    }

    override fun close() {
        if (!socket.isClosed) socket.close()
        closeListener?.onTunnelClosed(this)
    }
}
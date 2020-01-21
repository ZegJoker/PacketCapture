package com.stanley.packet_capture.tcpip.tunnel

import com.stanley.packet_capture.tcpip.constants.TCPStatus
import com.stanley.packet_capture.utils.VPNUtils
import com.stanley.tcpip.utils.intIpToStr
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPTunnel(val sourceAddress: Int, val sourcePort: Int, val destAddress: Int, val destPort: Int, private val selector: Selector): Tunnel {

    private var socketChannel: SocketChannel? = null
    var status = TCPStatus.PREPARE
    var closeListener: Tunnel.Callback? = null
    var seqNum = 1
    var ackNum = 1
    val pendingWritePacketQueue =  ConcurrentLinkedQueue<ByteArray>()
    var tcpOption = ByteArray(0)
    private var isConnected = false

    override fun connect() {
        if (!isConnected) {
            isConnected = true
            Thread {
                socketChannel = SocketChannel.open()
                socketChannel!!.configureBlocking(false)
                socketChannel!!.register(selector, SelectionKey.OP_READ or SelectionKey.OP_CONNECT or SelectionKey.OP_WRITE, this)
                VPNUtils.protect(socketChannel!!.socket())
                socketChannel!!.connect(InetSocketAddress(intIpToStr(destAddress), destPort))
            }.start()
        }
    }

    override fun transferData(data: ByteArray) {
        pendingWritePacketQueue.add(data)
        socketChannel?.keyFor(selector)?.interestOps(socketChannel!!.keyFor(selector).interestOps().and(SelectionKey.OP_WRITE))
    }

    override fun receiveData(data: ByteArray) {
        closeListener?.onDataReceived(this, data)
        seqNum += data.size
    }

    override fun close() {
        if (socketChannel != null && socketChannel!!.isOpen) socketChannel!!.close()
        closeListener?.onTunnelClosed(this)
    }
}
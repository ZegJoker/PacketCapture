package com.stanley.packet_capture.tcpip.tunnel

import com.stanley.packet_capture.tcpip.constants.TCPStatus
import com.stanley.packet_capture.utils.VPNUtils
import com.stanley.tcpip.utils.intIpToStr
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Stanley on 2020-01-20.
 */
class TCPTunnel(val sourceAddress: Int, val sourcePort: Int, val destAddress: Int, val destPort: Int): Tunnel {

    var socket = Socket(Proxy.NO_PROXY)
    var status = TCPStatus.PREPARE
    var tunnelCallback: Tunnel.Callback? = null
    var seqNum = 1
    var ackNum = 1
    val pendingWritePacketQueue =  ConcurrentLinkedQueue<ByteArray>()
    private val pendingWriteSeqNum = HashSet<Int>()
    var tcpOption = ByteArray(0)
    var window = 1023
    private var isConnected = false

    override fun connect() {
        if (!isConnected) {
            isConnected = true
            Thread {
                VPNUtils.protect(socket)
                socket.connect(InetSocketAddress(intIpToStr(destAddress), destPort))
            }.start()
        }
    }

    override fun transferData(data: ByteArray) {
        pendingWritePacketQueue.add(data)
    }

    fun transferData(seqNum: Int, data: ByteArray) {
        if (!pendingWriteSeqNum.contains(seqNum)) {
            pendingWriteSeqNum.add(seqNum)
            transferData(data)
        }
    }

    override fun receiveData(data: ByteArray) {
        tunnelCallback?.onDataReceived(this, data)
        seqNum += data.size
    }

    override fun close() {
        if (socket.isConnected) socket.close()
        tunnelCallback?.onTunnelClosed(this)
    }
}
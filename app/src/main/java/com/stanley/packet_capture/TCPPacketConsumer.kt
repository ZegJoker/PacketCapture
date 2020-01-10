package com.stanley.packet_capture

import android.util.Log
import android.util.SparseArray
import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.TCP
import com.stanley.tcpip.utils.calcIPChecksum
import com.stanley.tcpip.utils.calcTCPChecksum
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

class TCPPacketConsumer(private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>) :
    PacketConsumer<TCP> {
    private val sourcePortStatus: SparseArray<TCPStatus> = SparseArray()
    override fun consumePacket(packet: TCP) {
        when (checkAndSetTcpStatus(packet)) {
            TCPStatus.TRANSFERING -> transferData(packet)
            TCPStatus.CLOSING -> closeTunnel(packet)
            else -> handshake(packet)
        }
    }

    private fun checkAndSetTcpStatus(tcp: TCP): TCPStatus {
        if (sourcePortStatus.get(tcp.sourcePort.toInt()) == null) {
            sourcePortStatus.put(tcp.sourcePort.toInt(), TCPStatus.PREPARE)
        }
        return sourcePortStatus.get(tcp.sourcePort.toInt())
    }

    private fun handshake(tcp: TCP) {
        if (tcp.SYN == 1 && tcp.ACK == 0 && tcp.FIN == 0) {
            if (sourcePortStatus.get(tcp.sourcePort.toInt()) == TCPStatus.PREPARE) {
                sourcePortStatus.put(tcp.sourcePort.toInt(), TCPStatus.HANDSHAKING)
                val packet = ByteArray(60)
                val ip = IP(packet)
                ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
                ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
                ip.totalLength = 60
                ip.identification =
                    Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
                ip.flags = tcp.ip.flags
                ip.fragmentOffset = 0
                ip.ttl = 60
                ip.protocol = ProtocolCodes.TCP
                ip.sourceAddress = tcp.ip.destAddress
                ip.destAddress = tcp.ip.sourceAddress
                val handshakeRsp = TCP(ip)
                handshakeRsp.sourcePort = tcp.destPort
                handshakeRsp.destPort = tcp.sourcePort
                handshakeRsp.dataOffset = 40
                handshakeRsp.seqNum = Random(System.currentTimeMillis()).nextInt()
                handshakeRsp.ackNum = tcp.seqNum + 1
                handshakeRsp.SYN = 1
                handshakeRsp.ACK = 1
                handshakeRsp.window = tcp.window
                handshakeRsp.options = tcp.options
                ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
                handshakeRsp.checksum = calcTCPChecksum(
                    handshakeRsp.ip.packet,
                    handshakeRsp.dataOffset.toInt(),
                    handshakeRsp.ip.sourceAddress,
                    handshakeRsp.ip.destAddress,
                    handshakeRsp.ip.headerLength
                )
                pendingWritePacketQueue.add(ip)
            }
        } else if (tcp.RST == 1) {
            sourcePortStatus.remove(tcp.sourcePort.toInt())
        } else if (tcp.SYN == 1 && tcp.FIN == 1) {
            Log.d(TAG, "Request to close tunnel")
            closeTunnel(tcp)
        } else if (tcp.ACK == 1) {
            sourcePortStatus.put(tcp.sourcePort.toInt(), TCPStatus.TRANSFERING)
        }
    }

    private fun transferData(tcp: TCP) {
        val requestData = String(tcp.data)
        Log.d(TAG, "data: $requestData")
        val packet = ByteArray(60)
        val ip = IP(packet)
        ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
        ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
        ip.totalLength = 60
        ip.identification =
            Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
        ip.flags = tcp.ip.flags
        ip.fragmentOffset = 0
        ip.ttl = 60
        ip.protocol = ProtocolCodes.TCP
        ip.sourceAddress = tcp.ip.destAddress
        ip.destAddress = tcp.ip.sourceAddress
        val handshakeRsp = TCP(ip)
        handshakeRsp.sourcePort = tcp.destPort
        handshakeRsp.destPort = tcp.sourcePort
        handshakeRsp.dataOffset = 40
        handshakeRsp.seqNum = tcp.ackNum
        handshakeRsp.ackNum = tcp.seqNum + tcp.ip.totalLength - tcp.ip.headerLength - tcp.dataOffset
        handshakeRsp.ACK = 1
        handshakeRsp.window = tcp.window
        handshakeRsp.options = tcp.options
        ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
        handshakeRsp.checksum = calcTCPChecksum(
            handshakeRsp.ip.packet,
            handshakeRsp.dataOffset.toInt(),
            handshakeRsp.ip.sourceAddress,
            handshakeRsp.ip.destAddress,
            handshakeRsp.ip.headerLength
        )
        pendingWritePacketQueue.add(ip)
    }

    private fun closeTunnel(tcp: TCP) {

    }

}
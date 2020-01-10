package com.stanley.packet_capture

import android.util.Log
import android.util.SparseArray
import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.TCP
import com.stanley.tcpip.utils.calcIPChecksum
import com.stanley.tcpip.utils.calcTCPChecksum
import com.stanley.tcpip.utils.intIpToStr
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.random.Random

class TCPPacketConsumer(private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>) : PacketConsumer<TCP> {
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
            sourcePortStatus.put(tcp.sourcePort.toInt(), TCPStatus.HANDSHAKING)
        }
        return sourcePortStatus.get(tcp.sourcePort.toInt())
    }

    private fun handshake(tcp: TCP) {
        if (tcp.SYN == 1) {
            Log.d(TAG, "in ip checksum: " + tcp.ip.checksum)
            Log.d(TAG, "in tcp checksum: " + tcp.checksum)
            tcp.ip.checksum = 0
            tcp.ip.checksum = calcIPChecksum(tcp.ip.packet, tcp.ip.headerLength.toInt())
            tcp.checksum = 0
            tcp.checksum = calcTCPChecksum(tcp.ip.packet, tcp.dataOffset.toInt(), tcp.ip.sourceAddress, tcp.ip.destAddress, tcp.ip.headerLength)
            Log.d(TAG, "after in ip checksum: " + tcp.ip.checksum)
            Log.d(TAG, "after in tcp checksum: " + tcp.checksum)
            val packet =
                ByteArray(44)
            val ip = IP(packet)
            ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
            ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
            ip.totalLength =
                (TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH + TCPIPConstants.DEFAULT_TCP_PACKET_HEADER_LENGTH).toShort()
            ip.identification =
                abs(Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE.toInt())).toShort()
            ip.flags = 0
            ip.fragmentOffset = 0
            ip.ttl = 60
            ip.protocol = ProtocolCodes.TCP
            Log.d(TAG, "in: " + intIpToStr(tcp.ip.sourceAddress))
            Log.d(TAG, "out: " + intIpToStr(tcp.ip.destAddress))
            ip.sourceAddress = tcp.ip.destAddress
            ip.destAddress = tcp.ip.sourceAddress
            ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
            Log.d(TAG, "in: " + intIpToStr(ip.sourceAddress))
            Log.d(TAG, "out: " + intIpToStr(ip.destAddress))
            val handshakeRsp = TCP(ip)
            handshakeRsp.sourcePort = tcp.destPort
            handshakeRsp.destPort = tcp.sourcePort
            handshakeRsp.dataOffset = TCPIPConstants.DEFAULT_TCP_PACKET_HEADER_LENGTH.toByte()
            handshakeRsp.seqNum = 1
            handshakeRsp.ackNum = tcp.seqNum + 1
            handshakeRsp.SYN = 1
            handshakeRsp.ACK = 1
            handshakeRsp.window = Short.MAX_VALUE.dec()
            handshakeRsp.checksum = calcTCPChecksum(
                handshakeRsp.ip.packet,
                handshakeRsp.dataOffset.toInt(),
                handshakeRsp.ip.sourceAddress,
                handshakeRsp.ip.destAddress,
                handshakeRsp.ip.headerLength
            )
            pendingWritePacketQueue.add(ip)
        } else {
            sourcePortStatus.put(tcp.sourcePort.toInt(), TCPStatus.TRANSFERING)
        }
    }

    private fun transferData(tcp: TCP) {
        val requestData = String(tcp.data)
    }

    private fun closeTunnel(tcp: TCP) {

    }

}
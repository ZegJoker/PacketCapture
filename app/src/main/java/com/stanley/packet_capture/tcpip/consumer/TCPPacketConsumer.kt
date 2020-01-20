package com.stanley.packet_capture.tcpip.consumer

import android.util.Log
import android.util.SparseArray
import androidx.core.util.forEach
import com.stanley.packet_capture.tcpip.constants.TCPIPConstants
import com.stanley.packet_capture.tcpip.constants.TCPStatus
import com.stanley.packet_capture.tcpip.data.TCPDataObserver
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnel
import com.stanley.packet_capture.tcpip.tunnel.Tunnel
import com.stanley.packet_capture.utils.TAG
import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.TCP
import com.stanley.tcpip.utils.calcIPChecksum
import com.stanley.tcpip.utils.calcTCPChecksum
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

/**
 * Created by Stanley on 1/10/2020
 */
class TCPPacketConsumer(private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>) :
    PacketConsumer<TCP>, Tunnel.Callback {
    private val tunnelArray: SparseArray<TCPTunnel> = SparseArray()
    private val dataObserver = TCPDataObserver()
    init {
        dataObserver.start()
    }
    override fun consumePacket(packet: TCP) {
        when (checkAndSetTcpStatus(packet).status) {
            TCPStatus.TRANSFERRING -> transferData(packet)
            TCPStatus.CLOSING -> closeTunnel(packet)
            else -> handshake(packet)
        }
    }

    private fun checkAndSetTcpStatus(tcp: TCP): TCPTunnel {
        if (tunnelArray.get(tcp.sourcePort.toInt()) == null) {
            tunnelArray.put(tcp.sourcePort.toInt(),
                TCPTunnel(tcp.ip.sourceAddress, tcp.sourcePort.toInt(), tcp.ip.destAddress, tcp.destPort.toInt())
            )
        }
        return tunnelArray.get(tcp.sourcePort.toInt())
    }

    private fun handshake(tcp: TCP) {
        val tunnel = tunnelArray.get(tcp.sourcePort.toInt())
        if (tcp.SYN == 1 && tcp.ACK == 0 && tcp.FIN == 0) {
            if (tunnel.status == TCPStatus.PREPARE) {
                tunnel.status = TCPStatus.HANDSHAKING
                tunnel.connect()
                tunnel.tcpOption = tcp.options
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
                tunnel.seqNum = handshakeRsp.seqNum
                handshakeRsp.ackNum = tcp.seqNum + 1
                tunnel.ackNum = handshakeRsp.ackNum
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
            tunnel.close()
        } else if (tcp.SYN == 1 && tcp.FIN == 1) {
            Log.d(TAG, "Request to close tunnel")
            closeTunnel(tcp)
        } else if (tcp.ACK == 1) {
            tunnel.status = TCPStatus.TRANSFERRING
        }
    }

    private fun transferData(tcp: TCP) {
        val tunnel = tunnelArray.get(tcp.sourcePort.toInt())
        if (tcp.ACK == 1) {
            if (tcp.data.isNotEmpty()) {
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
                val confirmRsp = TCP(ip)
                confirmRsp.sourcePort = tcp.destPort
                confirmRsp.destPort = tcp.sourcePort
                confirmRsp.dataOffset = 40
                confirmRsp.seqNum = tcp.ackNum
                confirmRsp.ackNum = tcp.seqNum + tcp.ip.totalLength - tcp.ip.headerLength - tcp.dataOffset
                if (tunnel.seqNum < confirmRsp.seqNum) {
                    tunnel.seqNum = confirmRsp.seqNum
                    tunnel.ackNum = confirmRsp.ackNum
                }
                confirmRsp.ACK = 1
                confirmRsp.window = tcp.window
                confirmRsp.options = tunnel.tcpOption
                ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
                confirmRsp.checksum = calcTCPChecksum(
                    confirmRsp.ip.packet,
                    confirmRsp.dataOffset.toInt(),
                    confirmRsp.ip.sourceAddress,
                    confirmRsp.ip.destAddress,
                    confirmRsp.ip.headerLength
                )
                pendingWritePacketQueue.add(ip)
                tunnel.transferData(tcp.data)
            }
        } else if (tcp.FIN == 1) {
            tunnel.status = TCPStatus.CLOSING
            closeTunnel(tcp)
        }
    }

    private fun closeTunnel(tcp: TCP) {
        val tunnel = tunnelArray.get(tcp.sourcePort.toInt())
        if (tcp.FIN == 1) {
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
            val waveConfirmRsp = TCP(ip)
            waveConfirmRsp.sourcePort = tcp.destPort
            waveConfirmRsp.destPort = tcp.sourcePort
            waveConfirmRsp.dataOffset = 40
            waveConfirmRsp.seqNum = tcp.ackNum
            tunnel.seqNum = waveConfirmRsp.seqNum
            waveConfirmRsp.ackNum = tcp.seqNum + 1
            tunnel.ackNum = waveConfirmRsp.ackNum
            waveConfirmRsp.ACK = 1
            waveConfirmRsp.window = tcp.window
            waveConfirmRsp.options = tcp.options
            ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
            waveConfirmRsp.checksum = calcTCPChecksum(
                waveConfirmRsp.ip.packet,
                waveConfirmRsp.dataOffset.toInt(),
                waveConfirmRsp.ip.sourceAddress,
                waveConfirmRsp.ip.destAddress,
                waveConfirmRsp.ip.headerLength
            )
            pendingWritePacketQueue.add(ip)
            val packet2 = ByteArray(60)
            val ip2 = IP(packet2)
            ip2.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
            ip2.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
            ip2.totalLength = 60
            ip2.identification =
                Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
            ip2.flags = tcp.ip.flags
            ip2.fragmentOffset = 0
            ip2.ttl = 60
            ip2.protocol = ProtocolCodes.TCP
            ip2.sourceAddress = tcp.ip.destAddress
            ip2.destAddress = tcp.ip.sourceAddress
            val waveRequestRsp = TCP(ip2)
            waveRequestRsp.sourcePort = tcp.destPort
            waveRequestRsp.destPort = tcp.sourcePort
            waveRequestRsp.dataOffset = 40
            waveRequestRsp.seqNum = tcp.ackNum
            tunnel.seqNum = waveRequestRsp.seqNum
            waveRequestRsp.ackNum = tcp.seqNum + 1
            tunnel.ackNum = waveRequestRsp.ackNum
            waveRequestRsp.ACK = 1
            waveRequestRsp.window = tcp.window
            waveRequestRsp.options = tcp.options
            ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
            waveRequestRsp.checksum = calcTCPChecksum(
                waveRequestRsp.ip.packet,
                waveRequestRsp.dataOffset.toInt(),
                waveRequestRsp.ip.sourceAddress,
                waveRequestRsp.ip.destAddress,
                waveRequestRsp.ip.headerLength
            )
            pendingWritePacketQueue.add(ip2)
        } else if (tcp.ACK == 1) {
            tunnel.close()
        }
    }

    override fun onTunnelClosed(tunnel: Tunnel) {
        if (tunnel !is TCPTunnel) return
        tunnelArray.remove(tunnel.sourcePort)
    }

    override fun onDataReceived(tunnel: Tunnel, data: ByteArray) {
        if (tunnel !is TCPTunnel) return
        val packet = ByteArray(60 + data.size)
        val ip = IP(packet)
        ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
        ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
        ip.totalLength = (60 + data.size).toShort()
        ip.identification =
            Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
        ip.flags = 2
        ip.fragmentOffset = 0
        ip.ttl = 60
        ip.protocol = ProtocolCodes.TCP
        ip.sourceAddress = tunnel.destAddress
        ip.destAddress = tunnel.sourceAddress
        val serverRsp = TCP(ip)
        serverRsp.sourcePort = tunnel.destPort.toShort()
        serverRsp.destPort = tunnel.sourceAddress.toShort()
        serverRsp.dataOffset = 40
        serverRsp.seqNum = tunnel.seqNum
        serverRsp.ackNum = tunnel.ackNum
        serverRsp.ACK = 1
        serverRsp.window = Short.MAX_VALUE.dec()
        serverRsp.options = tunnel.tcpOption
        serverRsp.data = data
        ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
        serverRsp.checksum = calcTCPChecksum(
            serverRsp.ip.packet,
            serverRsp.dataOffset.toInt(),
            serverRsp.ip.sourceAddress,
            serverRsp.ip.destAddress,
            serverRsp.ip.headerLength
        )
        pendingWritePacketQueue.add(ip)
    }

    override fun close() {
        dataObserver.close()
        tunnelArray.forEach { _, tunnel ->
            tunnel.close()
        }
    }

}
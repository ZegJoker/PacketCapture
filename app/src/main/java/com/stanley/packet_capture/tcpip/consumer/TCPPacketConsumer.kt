package com.stanley.packet_capture.tcpip.consumer

import android.annotation.SuppressLint
import android.util.Log
import com.stanley.packet_capture.tcpip.constants.TCPIPConstants
import com.stanley.packet_capture.tcpip.constants.TCPStatus
import com.stanley.packet_capture.tcpip.tunnel.TCPRemoteCommunicator
import com.stanley.packet_capture.tcpip.tunnel.TCPTunnel
import com.stanley.packet_capture.tcpip.tunnel.Tunnel
import com.stanley.packet_capture.utils.TAG
import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.model.IP
import com.stanley.tcpip.model.TCP
import com.stanley.tcpip.utils.calcIPChecksum
import com.stanley.tcpip.utils.calcTCPChecksum
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

/**
 * Created by Stanley on 1/10/2020
 */
class TCPPacketConsumer(private val pendingWritePacketQueue: ConcurrentLinkedQueue<IP>) :
    PacketConsumer<TCP>, Tunnel.Callback {
    @SuppressLint("UseSparseArrays")
    private val tunnelArray = ConcurrentHashMap<Short, TCPTunnel>()
    val dataObserver =
        TCPRemoteCommunicator(tunnelArray)

    override fun consumePacket(packet: TCP) {
        when (checkAndSetTcpStatus(packet).status) {
            TCPStatus.TRANSFERRING_PENDING_CONNECTION, TCPStatus.TRANSFERRING_CONNECTED -> transferData(packet)
            TCPStatus.CLOSING_CLIENT, TCPStatus.CLOSING_SERVER -> closeTunnel(packet)
            else -> handshake(packet)
        }
    }

    private fun checkAndSetTcpStatus(tcp: TCP): TCPTunnel {
        if (tunnelArray[tcp.sourcePort] == null) {
            Log.d(TAG, "Cannot find tunnel, create a new for port: ${tcp.sourcePort}, map size: ${tunnelArray.size}")
            val tunnel = TCPTunnel(
                tcp.ip.sourceAddress,
                tcp.sourcePort,
                tcp.ip.destAddress,
                tcp.destPort
            )
            tunnel.tunnelCallback = this
            tunnelArray[tcp.sourcePort] = tunnel
        }
        return tunnelArray[tcp.sourcePort]!!
    }

    private fun handshake(tcp: TCP) {
        val tunnel = tunnelArray[tcp.sourcePort]!!
        Log.d(TAG, "tcp handshake, source port: ${tcp.sourcePort}, dest port: ${tcp.destPort}, tunnel_status=${tunnel.status.name}")
        if (tcp.SYN == 1 && tcp.ACK == 0 && tcp.FIN == 0) {
            if (tunnel.status == TCPStatus.PREPARE) {
                tunnel.status = TCPStatus.HANDSHAKING
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
                tunnel.tcpOption = tcp.options
            }
        } else if (tcp.RST == 1) {
            tunnel.close()
        } else if (tcp.SYN == 1 && tcp.FIN == 1) {
            Log.d(TAG, "Request to close tunnel")
            closeTunnel(tcp)
        } else if (tcp.ACK == 1) {
            tunnel.status = TCPStatus.TRANSFERRING_PENDING_CONNECTION
            tunnel.connect()
        }
    }

    private fun transferData(tcp: TCP) {
        val tunnel = tunnelArray[tcp.sourcePort]!!
        if (tcp.ACK == 1 && tcp.PSH == 1) {
            tunnel.tcpOption = tcp.options
            if (tcp.data.isNotEmpty()) {
                /*Log.d(TAG, "origin checksum: ip=${tcp.ip.checksum}, tcp=${tcp.checksum}")
                tcp.ip.checksum = 0
                tcp.checksum = 0
                tcp.ip.checksum = calcIPChecksum(tcp.ip.packet, tcp.ip.headerLength.toInt())
                tcp.checksum = calcTCPChecksum(tcp.ip.packet, tcp.ip.totalLength - tcp.ip.headerLength, tcp.ip.sourceAddress, tcp.ip.destAddress, tcp.ip.headerLength)
                Log.d(TAG, "re-calc checksum: ip=${tcp.ip.checksum}, tcp=${tcp.checksum}")*/

                Log.d(TAG, "received client's data: size=${tcp.data.size}, source_port=${tcp.sourcePort}, seqNum=${tcp.seqNum}, ackNum=${tcp.ackNum}")
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
                confirmRsp.ackNum =
                    tcp.seqNum + tcp.ip.totalLength - tcp.ip.headerLength - tcp.dataOffset
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
                if (tunnel.seqNum < tcp.ackNum) {
                    tunnel.seqNum = tcp.ackNum
                    tunnel.ackNum = tcp.seqNum + tcp.ip.totalLength - tcp.ip.headerLength - tcp.dataOffset
                }
                tunnel.window = tcp.window.toInt()
                tunnel.transferData(tcp.seqNum, tcp.data)
            }
        } else if (tcp.ACK == 1 && tcp.FIN != 1) {
            Log.d(TAG, "received client's confirmation: ACK==1, seqNum=${tcp.seqNum}, ackNum=${tcp.ackNum}")
            if (tunnel.seqNum < tcp.ackNum) {
                tunnel.seqNum = tcp.ackNum
                tunnel.ackNum = tcp.seqNum + 1
            }
            tunnel.window = tcp.window.toInt()
        } else if (tcp.FIN == 1) {
            tunnel.status = TCPStatus.CLOSING_CLIENT
            closeTunnel(tcp)
        }
    }

    private fun closeTunnel(tcp: TCP) {
        val tunnel = tunnelArray[tcp.sourcePort]!!
        if (tunnel.status == TCPStatus.CLOSING_CLIENT) {
            clientClosingProcess(tcp)
        } else if (tunnel.status == TCPStatus.CLOSING_SERVER) {
            serverClosingProcess(tcp)
        }
    }

    private fun clientClosingProcess(tcp: TCP) {
        val tunnel = tunnelArray[tcp.sourcePort]!!
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
            tunnel.window = tcp.window.toInt()
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

    private fun serverClosingProcess(tcp: TCP) {
        val tunnel = tunnelArray[tcp.sourcePort]!!
        if (tcp.FIN == 1) {
            val packet = ByteArray(20 + 20 + tunnel.tcpOption.size)
            val ip = IP(packet)
            ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
            ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
            ip.totalLength = (20 + 20 + tunnel.tcpOption.size).toShort()
            ip.identification =
                Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
            ip.flags = 2
            ip.fragmentOffset = 0
            ip.ttl = 60
            ip.protocol = ProtocolCodes.TCP
            ip.sourceAddress = tunnel.destAddress
            ip.destAddress = tunnel.sourceAddress
            val serverRsp = TCP(ip)
            serverRsp.sourcePort = tunnel.destPort
            serverRsp.destPort = tunnel.sourceAddress.toShort()
            serverRsp.dataOffset = (20 + tunnel.tcpOption.size).toByte()
            serverRsp.seqNum = tcp.ackNum
            serverRsp.ackNum = tcp.seqNum + 1
            serverRsp.ACK = 1
            serverRsp.window = Short.MAX_VALUE.dec()
            serverRsp.options = tunnel.tcpOption
            ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
            serverRsp.checksum = calcTCPChecksum(
                serverRsp.ip.packet,
                serverRsp.dataOffset.toInt(),
                serverRsp.ip.sourceAddress,
                serverRsp.ip.destAddress,
                serverRsp.ip.headerLength
            )
            pendingWritePacketQueue.add(ip)
            tunnel.close()
        }
    }

    override fun onTunnelClosed(tunnel: Tunnel) {
        if (tunnel !is TCPTunnel) return
        tunnelArray.remove(tunnel.sourcePort)
    }

    override fun onDataReceived(tunnel: Tunnel, data: ByteArray) {
        if (tunnel !is TCPTunnel) return
        val packet = ByteArray(20 + 20 + tunnel.tcpOption.size + data.size)
        val ip = IP(packet)
        ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
        ip.headerLength = 20
        ip.totalLength = (20 + 20 + tunnel.tcpOption.size + data.size).toShort()
        ip.identification =
            Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
        ip.flags = 2
        ip.fragmentOffset = 0
        ip.ttl = 60
        ip.protocol = ProtocolCodes.TCP
        ip.sourceAddress = tunnel.destAddress
        ip.destAddress = tunnel.sourceAddress
        val serverRsp = TCP(ip)
        serverRsp.sourcePort = tunnel.destPort
        serverRsp.destPort = tunnel.sourcePort
        serverRsp.dataOffset = (20 + tunnel.tcpOption.size).toByte()
        serverRsp.seqNum = tunnel.seqNum
        serverRsp.ackNum = tunnel.ackNum
        serverRsp.ACK = 1
        serverRsp.PSH = 1
        serverRsp.window = Short.MAX_VALUE.dec()
        serverRsp.options = tunnel.tcpOption
        serverRsp.data = data
        ip.checksum = calcIPChecksum(ip.packet, ip.headerLength.toInt())
        serverRsp.checksum = calcTCPChecksum(
            serverRsp.ip.packet,
            ip.totalLength - ip.headerLength,
            serverRsp.ip.sourceAddress,
            serverRsp.ip.destAddress,
            serverRsp.ip.headerLength
        )
        pendingWritePacketQueue.add(ip)
        Log.d(TAG, "received data from server: size=${data.size}, dest_port=${serverRsp.destPort}, seqNum=${serverRsp.seqNum}, ackNum=${serverRsp.ackNum}")
    }

    override fun onTunnelClosedFromServer(tunnel: Tunnel) {
        if (tunnel !is TCPTunnel) return
        Log.d(TAG, "Close tunnel from server")
        val packet = ByteArray(20 + 20 + tunnel.tcpOption.size)
        val ip = IP(packet)
        ip.version = TCPIPConstants.IP_PACKET_VERSION_IPV4.toByte()
        ip.headerLength = TCPIPConstants.DEFAULT_IP_PACKET_HEADER_LENGTH.toByte()
        ip.totalLength = (20 + 20 + tunnel.tcpOption.size).toShort()
        ip.identification =
            Random(System.currentTimeMillis()).nextInt(0, Short.MAX_VALUE.toInt()).toShort()
        ip.flags = 2
        ip.fragmentOffset = 0
        ip.ttl = 60
        ip.protocol = ProtocolCodes.TCP
        ip.sourceAddress = tunnel.destAddress
        ip.destAddress = tunnel.sourceAddress
        val serverRsp = TCP(ip)
        serverRsp.sourcePort = tunnel.destPort
        serverRsp.destPort = tunnel.sourceAddress.toShort()
        serverRsp.dataOffset = (20 + tunnel.tcpOption.size).toByte()
        serverRsp.seqNum = tunnel.seqNum + 1
        serverRsp.ackNum = tunnel.ackNum
        serverRsp.ACK = 1
        serverRsp.window = Short.MAX_VALUE.dec()
        serverRsp.options = tunnel.tcpOption
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
        for ((_, tunnel) in tunnelArray) {
            tunnel.close()
        }
    }

}
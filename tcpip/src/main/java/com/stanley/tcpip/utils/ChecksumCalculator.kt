package com.stanley.tcpip.utils

import com.stanley.tcpip.constants.ProtocolCodes
import com.stanley.tcpip.utils.extensions.readShort
import com.stanley.tcpip.utils.extensions.writeInt
import com.stanley.tcpip.utils.extensions.writeShort
import kotlin.experimental.and
import kotlin.experimental.inv

/**
 * IP头校验和固定为16位，当数据按位相加后高16位不为0，那么则将高16位和低16位反复相加，直到高16位为零，这个低16位数的取反则为校验和。
 * 在计算TCP校验和时会存在数据长度为奇数的情况。另外该函数返回值并没有取反，这样可以方便调用该函数重复计算checksum（计算tcp的checksum时会用到），需要自己在函数外取反。
 */
private fun calcIpChecksumInternal(
    packet: ByteArray,
    packetLength: Int,
    initChecksum: Short = 0,
    headerOffset: Byte = 0
): Short {
    var checksum = initChecksum.toLong()
    var index = headerOffset.toInt()
    var length = packetLength
    while(length > 1) {
        checksum += packet.readShort(index).and(0x0FFFF.toShort())
        index += 2
        length -= 2
    }

    if (length > 0) checksum += packet[index].toInt().and(0xFF).shl(8)

    while (checksum.shr(16) > 0) {
        checksum = checksum.and(0x0FFFF).plus(checksum.shr(16))
    }

    return checksum.toShort()
}

/**
 *  计算IP协议头部校验和
 *  注：IP协议校验和只计算头部
 */
fun calcIPChecksum(packet: ByteArray, packetLength: Int) =
    calcIpChecksumInternal(packet, packetLength).inv()

/**
 * 计算TCP/UDP协议校验和
 * 注：TCP以及UDP协议计算校验和时需将其数据也一并计算入校验和里
 */
private fun calcTcpUdpChecksum(
    packet: ByteArray,
    packetLength: Int,
    sourceIp: Int,
    destIp: Int,
    protocol: Byte,
    headerOffset: Byte = 0
): Short {
    var checksum: Short
    val fakeIpHeader = ByteArray(12)
    fakeIpHeader.writeInt(0, sourceIp)
    fakeIpHeader.writeInt(4, destIp)
    fakeIpHeader[8] = 0
    fakeIpHeader[9] = protocol
    fakeIpHeader.writeShort(10, packetLength.toShort())
    checksum = calcIpChecksumInternal(
        fakeIpHeader,
        fakeIpHeader.size,
        0
    )
    checksum =
        calcIpChecksumInternal(packet, packetLength, checksum, headerOffset)
    return checksum.inv()
}

fun calcTCPChecksum(
    packet: ByteArray,
    packetLength: Int,
    sourceIp: Int,
    destIp: Int,
    headerOffset: Byte
) = calcTcpUdpChecksum(packet, packetLength, sourceIp, destIp, ProtocolCodes.TCP, headerOffset)

fun calcUDPChecksum(
    packet: ByteArray,
    packetLength: Int,
    sourceIp: Int,
    destIp: Int,
    headerOffset: Byte
) = calcTcpUdpChecksum(packet, packetLength, sourceIp, destIp, ProtocolCodes.UDP, headerOffset)

package com.stanley.packet_capture.utils

import android.net.VpnService
import java.net.DatagramSocket
import java.net.Socket

/**
 * Created by Stanley on 2020-01-21.
 */
object VPNUtils {
    var vpnService: VpnService? = null
    fun protect(socket: Int) = vpnService?.protect(socket)
    fun protect(socket: Socket) = vpnService?.protect(socket)
    fun protect(socket: DatagramSocket) = vpnService?.protect(socket)
}
package com.stanley.packet_capture.tcpip.tunnel

import java.io.Closeable

/**
 * Created by Stanley on 2020-01-20.
 */
interface Tunnel: Closeable {
    fun connect()
    fun transferData(data: ByteArray)
    fun receiveData(data: ByteArray)
    interface Callback {
        fun onTunnelClosed(tunnel: Tunnel)
        fun onDataReceived(tunnel: Tunnel, data: ByteArray)
        fun onTunnelClosedFromServer(tunnel: Tunnel)
    }
}
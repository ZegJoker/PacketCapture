package com.stanley.packet_capture.tcpip.tunnel

import java.nio.channels.Selector

/**
 * Created by Stanley on 2020-01-20.
 */
object TCPTunnelSelector {
    val selector = Selector.open()
}
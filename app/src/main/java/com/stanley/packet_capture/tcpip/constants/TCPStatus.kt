package com.stanley.packet_capture.tcpip.constants

/**
 * Created by Stanley on 2020-01-10.
 */
enum class TCPStatus {
    PREPARE,
    HANDSHAKING,
    TRANSFERRING_PENDING_CONNECTION,
    TRANSFERRING_CONNECTED,
    CLOSING_CLIENT,
    CLOSING_SERVER
}
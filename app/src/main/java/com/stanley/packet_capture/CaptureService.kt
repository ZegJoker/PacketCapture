package com.stanley.packet_capture

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.stanley.packet_capture.constants.Config
import com.stanley.packet_capture.tcpip.PacketDistributor
import com.stanley.packet_capture.tcpip.PacketReader
import com.stanley.packet_capture.tcpip.PacketWriter
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream

class CaptureService : VpnService(), Closeable {

    var running = false
    private val vpnDescriptor: ParcelFileDescriptor by lazy { establish() }
    private val packetReader: PacketReader by lazy {
        PacketReader(
            FileInputStream(vpnDescriptor.fileDescriptor)
        )
    }
    private val packetWriter: PacketWriter by lazy {
        PacketWriter(
            FileOutputStream(vpnDescriptor.fileDescriptor)
        )
    }
    private val packetDistributor: PacketDistributor by lazy {
        PacketDistributor(
            packetReader.pendingPacketQueue,
            packetWriter.pendingPacketQueue
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            running = true
            establish()
            startVpn()
            packetDistributor.startDispatch()
        }
        return Service.START_STICKY
    }

    /**
     * create a virtual network interface in the system, all of packets which fit the rules will
     * be redirected to this interface, thus the service can capture the data which is sent to the
     * interface
     */
    private fun establish() = Builder()
        .setSession(getString(R.string.session))
        .setBlocking(false)
        .setMtu(Config.MTU_SIZE)
        .addAddress("10.5.37.1", 16)
        .addRoute("0.0.0.0", 0)
        .addAllowedApplication("com.stanley.networktest")
        .establish()

    private fun startVpn() = Thread {
        while (running) {
            packetReader.readIPPacket()
            packetWriter.writePacket()
        }
    }.start()

    override fun close() {
        packetDistributor.close()
        packetReader.close()
        packetWriter.close()
    }


}
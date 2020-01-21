package com.stanley.packet_capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stanley.packet_capture.constants.Config
import com.stanley.packet_capture.tcpip.PacketDistributor
import com.stanley.packet_capture.tcpip.PacketReader
import com.stanley.packet_capture.tcpip.PacketWriter
import com.stanley.packet_capture.utils.VPNUtils
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

    override fun onCreate() {
        super.onCreate()
        VPNUtils.vpnService = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running) {
            val notificationManager = NotificationManagerCompat.from(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(Config.NOTIFICATION_CHANNEL_ID, getString(R.string.session), NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(Config.SERVICE_NOTIFICATION_ID, NotificationCompat.Builder(this, packageName)
                .setChannelId(Config.NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(getString(R.string.session))
                .setContentText(getString(R.string.data_capture))
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build())
            running = true
            establish()
            startVpn()
            packetDistributor.startDispatch()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        VPNUtils.vpnService = null
        NotificationManagerCompat.from(this).cancel(Config.SERVICE_NOTIFICATION_ID)
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
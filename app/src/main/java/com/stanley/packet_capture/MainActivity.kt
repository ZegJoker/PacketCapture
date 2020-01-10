package com.stanley.packet_capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1
        private const val REQUEST_VPN_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnStartCapture.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        checkStoragePermission()
    }

    fun checkStoragePermission() {
        val readExternalStoragePermissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val writeExternalStoragePermissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val notGrantPermissions = arrayOfNulls<String>(2)
        if (readExternalStoragePermissionStatus != PackageManager.PERMISSION_GRANTED
            || writeExternalStoragePermissionStatus != PackageManager.PERMISSION_GRANTED) {
            notGrantPermissions[0] = Manifest.permission.READ_EXTERNAL_STORAGE
            notGrantPermissions[1] = Manifest.permission.WRITE_EXTERNAL_STORAGE
            requestPermissions(notGrantPermissions, REQUEST_PERMISSION_CODE)
        }
    }

    override fun onClick(v: View?) {
        val prepare = VpnService.prepare(this)
        if (prepare == null) startService(Intent(this, CaptureService::class.java))
        else startActivityForResult(prepare, REQUEST_VPN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_CODE && resultCode == Activity.RESULT_OK) {
            startService(Intent(this, CaptureService::class.java))
        }
    }

}
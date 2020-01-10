package com.stanley.packet_capture

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnStartCapture.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val prepare = VpnService.prepare(this)
        if (prepare == null) startService(Intent(this, CaptureService::class.java))
        else startActivityForResult(prepare, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            startService(Intent(this, CaptureService::class.java))
        }
    }

}
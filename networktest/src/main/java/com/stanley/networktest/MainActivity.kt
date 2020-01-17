package com.stanley.networktest

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by Stanley on 2020-01-17.
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnRequest.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        //TODO request network
    }
}

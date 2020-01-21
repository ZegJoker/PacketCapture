package com.stanley.networktest

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException

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
        requestNetwork("http://gank.io")
    }

    private fun requestNetwork(url: String) = OkHttpClient().newCall(
        Request.Builder().url(url).build()
    ).enqueue(object: Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("MainActivity", e.message, e)
            runOnUiThread {
                tvResponse.text = e.message
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.code.let {
                Log.d("MainActivity", "response code: $it")
            }
            response.body?.string()?.let {
                Log.d("MainActivity", it)
                runOnUiThread {
                    tvResponse.text = "Response code: ${response.code}\n$it"
                }
            }
        }
    })
}

package com.stanley.tcpip.utils

fun intIpToStr(ip: Int) =
    ip.shr(24).and(0xFF).toString() + "." +
            ip.shr(16).and(0xFF).toString() + "." +
            ip.shr(8).and(0xFF).toString() + "." +
            ip.and(0xFF).toString()
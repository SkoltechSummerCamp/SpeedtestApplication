package ru.scoltech.openran.speedtest

import android.content.Context
import android.util.Log
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import java.nio.charset.StandardCharsets

enum class RequestType { START, STOP }

suspend fun sendGETRequest(
    address: String,
    requestType: RequestType,
    timeout: Int,
    value: String = ""
): String {
    var currentPort = ApplicationConstants.HTTP_SERVER_PORT;

    var currentAddress: InetAddress = try {
        when {
            !address.contains(':') || (address.first() == '[' && address.last() == ']') ->
                InetAddress.getByName(address) //IPv4 or IPv6
            address.split(":").size == 2 -> {
                val addressAndPort = address.split(":")
                currentPort = addressAndPort[1].toInt()
                InetAddress.getByName(addressAndPort[0])
            }//IPv4 with port
            address.contains("]:") && address.first() == '[' -> {
                val addressAndPort = address.split("]:")
                currentPort = addressAndPort[1].toInt()
                InetAddress.getByName(addressAndPort[0] + ']')
            }//IPv6 with port
            else -> InetAddress.getByName("localhost")
        }
    } catch (e: UnknownHostException) {
        return "error"
    }
    val url = when (requestType) {
        RequestType.START -> "http://${currentAddress.hostAddress}:${currentPort}/start-iperf?args=$value"
        RequestType.STOP -> "http://$currentAddress:${currentPort}/stop-iperf"
    }
    val channel = Channel<String>()
    val connection = try {
        Log.d("url", url)
        URL(url).openConnection() as HttpURLConnection
    } catch (e: IOException) {
        return "error"
    }
    CoroutineScope(Dispatchers.IO).launch {
        try {
            channel.trySend(String(connection.inputStream.readBytes(), StandardCharsets.UTF_8))
        } catch (e: IOException) {
            channel.trySend("error")
        }
        connection.disconnect()
    }
    CoroutineScope(Dispatchers.IO).launch {
        delay(timeout.toLong())
        channel.trySend("error")
        connection.disconnect()
    }
    return channel.receive()
}
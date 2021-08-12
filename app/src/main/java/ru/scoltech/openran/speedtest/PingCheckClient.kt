package ru.scoltech.openran.speedtest

import kotlinx.coroutines.delay
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


class PingCheckClient {
    @Volatile
    var isDone = true
    var inPingChecking = AtomicBoolean(false)

    private fun doPing(address: InetAddress?, port: Int, timeout: Int): Int {
        try {
            DatagramSocket().use { clientSocket ->
                val timeOfPing = System.nanoTime()
                val requestValue =
                    "Ping at $timeOfPing".toByteArray(StandardCharsets.UTF_8)
                val request =
                    DatagramPacket(requestValue, requestValue.size)
                request.address = address
                request.port = port
                val start = System.nanoTime()
                clientSocket.send(request)
                isDone = false
                clientSocket.soTimeout = timeout
                val inputDatagramPacket =
                    DatagramPacket(ByteArray(1024), 1024)
                clientSocket.receive(inputDatagramPacket)
                isDone = true
                val end = System.nanoTime()
                val result = String(
                    Arrays.copyOf(
                        inputDatagramPacket.data,
                        inputDatagramPacket.length
                    ), StandardCharsets.UTF_8
                )
                return if (result == "Ping at $timeOfPing") {
                    (end - start).toInt()
                } else -1
            }
        } catch (e: IOException) {
            isDone = true
            return -1
        }
    }

    suspend fun doPingTest(
        currValueSetter: (String) -> Unit,
        ip: String,
        port: Int = 49121,
        time: Int = 3000,
        delayValue: Long = 100
    ) {
        if (inPingChecking.get()) {
            currValueSetter("Error")
        } else {
            val start = System.currentTimeMillis()
            var number = 0
            var counter = 0
            var mistakesCounter = 0
            inPingChecking.set(true)
            val address: InetAddress? = try {
                InetAddress.getByName(ip)
            } catch (e: UnknownHostException) {
                currValueSetter("Error")
                null
            }
            if (address != null) {

                while (System.currentTimeMillis() - start < time) {
                    number++
                    val currValue = doPing(address, port, 100)
                    if (currValue == -1)
                        mistakesCounter++
                    else {
                        currValueSetter(currValue.toDouble().div(1000_000).toString().format("%.2f"))
                        counter += currValue
                    }
                    delay(delayValue)
                }

                if (mistakesCounter == number) {
                    currValueSetter("Error")
                } else {
                    val result =
                        ((counter.toDouble()) / ((number.toDouble() - mistakesCounter.toDouble()) * 1000_000)).toString()
                    currValueSetter(result.format("%.2f"))
                }
            }
        }
    }
}

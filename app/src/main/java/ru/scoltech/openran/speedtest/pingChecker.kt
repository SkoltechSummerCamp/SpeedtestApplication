package ru.scoltech.openran.speedtest

import kotlinx.coroutines.delay
import java.net.InetAddress

suspend fun doPingTest(
    currValueSetter: (String) -> Unit,
    ip: String,
    port: Int = 7,
    time: Int = 3000,
    delayValue: Long = 100
) {
    try {
        val start = System.currentTimeMillis()
        val pingChecker = PingCheckClient()
        var number = 0
        var counter = 0
        var mistakesCounter = 0
        while (System.currentTimeMillis() - start < time) {
            number++
            val currValue = pingChecker.doPing(InetAddress.getByName(ip), port, 100)
            if (currValue == -1)
                mistakesCounter++
            else {
                currValueSetter(currValue.toString())
                counter += currValue
            }
            delay(delayValue)
        }

        if (mistakesCounter == number)
            currValueSetter("Error")
        else {
            val result =
                ((counter.toDouble()) / (number.toDouble() - mistakesCounter.toDouble())).toString()
            if (result.length > 5)
                currValueSetter(result.substring (0, 5))
            else
                currValueSetter(result)
        }
    } catch (e: Exception) {
        currValueSetter("Error")
    }
}


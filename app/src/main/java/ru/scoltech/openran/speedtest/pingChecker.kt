package ru.scoltech.openran.speedtest

import kotlinx.coroutines.delay
import java.net.InetAddress
import java.net.UnknownHostException

suspend fun doPingTest(
    currValueSetter: (String) -> Unit,
    ip: String,
    port: Int = 49121,
    time: Int = 3000,
    delayValue: Long = 100
) {
    val start = System.currentTimeMillis()
    val pingChecker = PingCheckClient()
    var number = 0
    var counter = 0
    var mistakesCounter = 0
    val address: InetAddress? = try {
        InetAddress.getByName(ip)
    } catch (e: UnknownHostException) {
        currValueSetter("Error")
        null
    }
    if (address != null) {
        val formatter: (String) -> String = {
            var strBuffer = listOf<String>()
            if (it.contains(".") && it.split('.').also { strBuffer = it }[1].length > 2)
                strBuffer[0] + "." + strBuffer[1].substring(0, 2)
            else it
        }

        while (System.currentTimeMillis() - start < time) {
            number++
            val currValue = pingChecker.doPing(address, port, 100)
            if (currValue == -1)
                mistakesCounter++
            else {
                currValueSetter(formatter(currValue.toDouble().div(1000_000).toString()))
                counter += currValue
            }
            delay(delayValue)

        }

        if (mistakesCounter == number)
            currValueSetter("Error")
        else {
            val result =
                ((counter.toDouble()) / ((number.toDouble() - mistakesCounter.toDouble()) * 1000_000)).toString()
            currValueSetter(formatter(result))
        }
    }
}


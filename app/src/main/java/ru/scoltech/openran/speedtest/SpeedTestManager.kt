package ru.scoltech.openran.speedtest

import android.content.Context
import android.util.Log
import com.opencsv.CSVParser
import kotlinx.coroutines.*
import ru.scoltech.openran.speedtest.iperf.IperfRunner
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.roundToLong

class SpeedTestManager
private constructor(
    context: Context,
    private val onPingUpdate: (Long) -> Unit,
    private val onDownloadStart: () -> Unit,
    private val onDownloadSpeedUpdate: (Long) -> Unit,
    private val onDownloadFinish: (LongSummaryStatistics) -> Long,
    private val onUploadStart: () -> Unit,
    private val onUploadSpeedUpdate: (Long) -> Unit,
    private val onUploadFinish: (LongSummaryStatistics) -> Unit,
    private val onFinish: () -> Unit,
    private val onStopped: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private var downloadSpeedStatistics = LongSummaryStatistics()
    private var uploadSpeedStatistics = LongSummaryStatistics()

    @Volatile
    private var state = State.NONE

    @Volatile
    private lateinit var serverAddress: ServerAddr

    private val lock = ReentrantLock()

    private val iperfRunner = IperfRunner.Builder(context.filesDir.absolutePath)
        .stdoutLinesHandler(this::handleIperfStdout)
        .stderrLinesHandler(this::onIperfError)
        .onFinishCallback(this::onIperfFinish)
        .build()

    fun start() {
        lock.lock()
        downloadSpeedStatistics = LongSummaryStatistics()
        uploadSpeedStatistics = LongSummaryStatistics()
        lock.unlock()

        CoroutineScope(Dispatchers.IO).launch {
            // TODO uncomment when balancer api is ready
//            val addresses = ClientApi().clientObtainIp()
//            if (addresses.isEmpty()) {
//                onError("Could not obtain server ip")
//                return@launch
//            }
//
//            serverAddress = addresses[0]

            // TODO remove when balancer api is ready
            val addresses = arrayOf(ServerAddr("localhost", 5000, 5201))

            val serverInfo = addresses.map { it to getPing(it) }
                .minByOrNull { it.second }
                ?: return@launch onError("No servers are available right now")

            onPingUpdate(serverInfo.second)
            serverAddress = serverInfo.first
            startDownload()
        }
    }

    private fun getPing(address: ServerAddr): Long {
        val icmpPing = ICMPPing()

        var ping = ""
        icmpPing.justPingByHost(address.ip) {
            ping = it
            icmpPing.stopExecuting()
        }
        return ping.toDouble().roundToLong()
    }

    suspend fun startDownload() {
        val serverMessage = sendGETRequest(
            "${serverAddress.ip}:${serverAddress.port}",
            RequestType.START,
            1000L,
            "-s -u",
        )

        if (serverMessage == "error") {
            onError(serverMessage)
        } else {
            lock.lock()
            onDownloadStart()
            state = State.DOWNLOAD
            lock.unlock()

            iperfRunner.start(
                "-c ${serverAddress.ip} -p ${serverAddress.portIperf} -i 0.1 -y C -u -b 120m -R"
            )
        }
    }

    suspend fun startUpload() {
        val serverMessage = sendGETRequest(
            "${serverAddress.ip}:${serverAddress.port}",
            RequestType.START,
            1000L,
            "-s -u",
        )

        if (serverMessage == "error") {
            onError(serverMessage)
        } else {
            lock.lock()
            onUploadStart()
            state = State.UPLOAD
            lock.unlock()

            iperfRunner.start(
                "-c ${serverAddress.ip} -p ${serverAddress.portIperf} -y C -i 0.1 -u -b 120m"
            )
        }
    }

    fun stop() {
        lock.lock()
        state = State.STOPPED
        lock.unlock()

        iperfRunner.sendSigKill()
        iperfRunner.killAndWait()
    }

    private fun handleIperfStdout(line: String) {
        lock.lock()
        val speed = CSVParser().parseLine(line)[8].toLong()
        if (state == State.DOWNLOAD) {
            onDownloadSpeedUpdate(speed)
            downloadSpeedStatistics.accept(speed)
        } else if (state == State.UPLOAD) {
            onUploadSpeedUpdate(speed)
            uploadSpeedStatistics.accept(speed)
        }
        lock.unlock()
    }

    private fun onIperfError(error: String) {
        Log.e(LOG_TAG, error)
        lock.lock()
        if (state != State.STOPPED) {
//            state = State.ERROR
            onError(error)
        }
        lock.unlock()
    }

    private fun onIperfFinish() {
        lock.lock()
        when (state) {
            State.DOWNLOAD -> {
                val delayBeforeUpload = onDownloadFinish(downloadSpeedStatistics)
                CoroutineScope(Dispatchers.IO).launch {
                    delay(delayBeforeUpload)
                    startUpload()
                }
            }
            State.UPLOAD -> {
                onUploadFinish(uploadSpeedStatistics)
                onFinish()
            }
            State.STOPPED -> {
                onStopped()
            }
            else -> {}
        }
        lock.unlock()
    }

    private enum class State {
        NONE, DOWNLOAD, UPLOAD, STOPPED, ERROR
    }

    // TODO remove when balancer api is ready
    private data class ServerAddr(val ip: String, val port: Int, val portIperf: Int)

    class Builder(private val context: Context) {
        private var onPingUpdate: (Long) -> Unit = {}
        private var onDownloadStart: () -> Unit = {}
        private var onDownloadSpeedUpdate: (Long) -> Unit = {}
        private var onDownloadFinish: (LongSummaryStatistics) -> Long = { 0 }
        private var onUploadStart: () -> Unit = {}
        private var onUploadSpeedUpdate: (Long) -> Unit = {}
        private var onUploadFinish: (LongSummaryStatistics) -> Unit = {}
        private var onFinish: () -> Unit = {}
        private var onStopped: () -> Unit = {}
        private var onError: (String) -> Unit = {}

        fun build(): SpeedTestManager {
            return SpeedTestManager(
                context,
                onPingUpdate,
                onDownloadStart,
                onDownloadSpeedUpdate,
                onDownloadFinish,
                onUploadStart,
                onUploadSpeedUpdate,
                onUploadFinish,
                onFinish,
                onStopped,
                onError,
            )
        }

        fun onPingUpdate(onPingUpdate: (Long) -> Unit): Builder {
            this.onPingUpdate = onPingUpdate
            return this
        }

        fun onDownloadStart(onDownloadStart: () -> Unit): Builder {
            this.onDownloadStart = onDownloadStart
            return this
        }

        fun onDownloadSpeedUpdate(onDownloadSpeedUpdate: (Long) -> Unit): Builder {
            this.onDownloadSpeedUpdate = onDownloadSpeedUpdate
            return this
        }

        fun onDownloadFinish(onDownloadFinish: (LongSummaryStatistics) -> Long): Builder {
            this.onDownloadFinish = onDownloadFinish
            return this
        }

        fun onUploadStart(onUploadStart: () -> Unit): Builder {
            this.onUploadStart = onUploadStart
            return this
        }

        fun onUploadSpeedUpdate(onUploadSpeedUpdate: (Long) -> Unit): Builder {
            this.onUploadSpeedUpdate = onUploadSpeedUpdate
            return this
        }

        fun onUploadFinish(onUploadFinish: (LongSummaryStatistics) -> Unit): Builder {
            this.onUploadFinish = onUploadFinish
            return this
        }

        fun onFinish(onFinish: () -> Unit): Builder {
            this.onFinish = onFinish
            return this
        }

        fun onStopped(onStopped: () -> Unit): Builder {
            this.onStopped = onStopped
            return this
        }

        fun onError(onError: (String) -> Unit): Builder {
            this.onError = onError
            return this
        }
    }

    companion object {
        private const val LOG_TAG = "SpeedTestManager"
    }
}

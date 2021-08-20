package ru.scoltech.openran.speedtest.iperf

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import kotlin.jvm.Throws

class IperfRunner(writableDir: String) {
    @Volatile
    private var processWaiterThread: Thread? = null
    @Volatile
    private var processPid: Long = 0L

    private val stdoutPipePath = "$writableDir/iperfStdout"
    private val stderrPipePath = "$writableDir/iperfStderr"

    var stdoutHandler: (String) -> Unit = {}
    var stderrHandler: (String) -> Unit = {}
    var onFinishHandler: () -> Unit = {}

    @Throws(IperfException::class)
    fun start(args: String) {
        forceMkfifo(stdoutPipePath, "stdout")
        forceMkfifo(stderrPipePath, "stderr")

        val pidHolder = longArrayOf(0)
        runCheckingErrno("Could not fork process and launch iperf") {
            start(stdoutPipePath, stderrPipePath, splitArgs(args), pidHolder)
        }
        processPid = pidHolder[0]

        val outputHandlers = listOf(
            stderrPipePath to stderrHandler,
            stdoutPipePath to stdoutHandler,
        ).map { (pipePath, handler) ->
            CoroutineScope(Dispatchers.IO).launch { handlePipe(pipePath, handler) }
        }

        processWaiterThread = Thread({
            waitForProcess(processPid)
            onFinish(outputHandlers)
        }, "Iperf Waiter")
            .also { it.start() }
    }

    private fun forceMkfifo(path: String, redirectingFile: String) {
        val previousFile = File(path)
        previousFile.delete()
        if (previousFile.exists()) {
            val type = if (previousFile.isDirectory) "directory" else "file"
            throw IperfException("Could not delete $type $path to create named pipe")
        }
        runCheckingErrno(getMkfifoErrorMessage(path, redirectingFile)) {
            mkfifo(path)
        }
    }

    private fun handlePipe(pipePath: String, handler: (String) -> Unit) {
        try {
            InputStreamReader(FileInputStream(pipePath), Charsets.UTF_8)
                .buffered().use { reader ->
                    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                    var length = 0
                    while (length >= 0) {
                        if (length > 0) {
                            handler(buffer.concatToString(endIndex = length))
                        }
                        length = reader.read(buffer)
                    }
                }
        } catch (e: IOException) {
            val message = "Could not handle iperf output"
            Log.e(LOG_TAG, message, e)
            stderrHandler("$message: ${e::class.simpleName} (${e.message})")
        }
    }

    private fun splitArgs(args: String): Array<String> {
        return args.split(SPACES_REGEX).filter { it.isNotBlank() }.toTypedArray()
    }

    private fun onFinish(outputHandlers: List<Job>) {
        runBlocking {
            outputHandlers.forEach {
                it.join()
            }
        }

        onFinishHandler()
    }

    @Throws(IperfException::class)
    fun killAndWait() {
        // TODO set null on end
        // TODO synchronize on `processWaiterThread` and `processPid`
        if (processWaiterThread != null) {
            sendSigInt()
            processWaiterThread!!.join()
        }
    }

    @Throws(IperfException::class)
    fun sendSigInt() {
        runCheckingErrno(getKillErrorMessage("SIGINT")) { sendSigInt(processPid) }
    }

    @Throws(IperfException::class)
    fun sendSigKill() {
        runCheckingErrno(getKillErrorMessage("SIGKILL")) { sendSigKill(processPid) }
    }

    private fun getMkfifoErrorMessage(pipePath: String, redirectingFile: String) =
        "Could not create named pipe $pipePath for redirecting $redirectingFile"

    private fun getKillErrorMessage(signalName: String) =
        "Could not send $signalName to a process with pid $processPid"

    private inline fun runCheckingErrno(errorMessage: String, block: () -> Int) {
        val errno = block()
        if (errno != 0) {
            throw IperfException(errorMessage, errno)
        }
    }

    private external fun sendSigInt(pid: Long): Int

    private external fun sendSigKill(pid: Long): Int

    private external fun mkfifo(pipePath: String): Int

    private external fun start(
        stdoutPipePath: String,
        stderrPipePath: String,
        args: Array<String>,
        pidHolder: LongArray,
    ): Int

    private external fun waitForProcess(pid: Long)

    companion object {
        private val SPACES_REGEX = Regex("\\s+")
        private const val LOG_TAG = "IperfRunner"

        init {
            System.loadLibrary("iperf2")
        }
    }
}

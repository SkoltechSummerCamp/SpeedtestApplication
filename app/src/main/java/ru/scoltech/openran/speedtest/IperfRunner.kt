package ru.scoltech.openran.speedtest

import android.util.Log
import kotlinx.coroutines.*
import java.io.*

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

    fun start(args: String) {
        // TODO throw on error
        mkfifo(stdoutPipePath)
        // TODO throw on error
        mkfifo(stderrPipePath)

        val outputHandlers = listOf(
            stdoutPipePath to stdoutHandler,
            stderrPipePath to stderrHandler
        ).map { (pipePath, handler) ->
            CoroutineScope(Dispatchers.IO).launch { handlePipe(pipePath, handler) }
        }

        val pidHolder = longArrayOf(0)
        // TODO error "could not fork"
        val returnCode = start(stdoutPipePath, stderrPipePath, splitArgs(args), pidHolder)
        processPid = pidHolder[0]

        processWaiterThread = Thread({
            waitForProcess(processPid)
            onFinish(outputHandlers)
        }, "Iperf Waiter")
            .also { it.start() }
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
            val buffer = StringWriter()
            e.printStackTrace(PrintWriter(buffer))
            Log.e(LOG_TAG, "Could not handle iperf output: $buffer")
            stderrHandler(buffer.toString())
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

    fun killAndWait() {
        // TODO set null on end
        // TODO synchronize on `processWaiterThread` and `processPid`
        if (processWaiterThread != null) {
            sendSigInt()
            processWaiterThread!!.join()
        }
    }

    fun sendSigInt() {
        kill(::sendSigInt)
    }

    fun sendSigKill() {
        kill(::sendSigKill)
    }

    private inline fun kill(block: (Long) -> Unit) {
        // TODO throw on error
        block(processPid)
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

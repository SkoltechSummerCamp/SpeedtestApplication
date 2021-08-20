package ru.scoltech.openran.speedtest.iperf

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.jvm.Throws

class IperfRunner(writableDir: String) {
    /** Set to non null value if and only if process is running */
    private var processWaiterThread: Thread? = null
    private var processPid: Long = 0L

    private val lock: Lock = ReentrantLock()
    private val finishedCondition: Condition = lock.newCondition()

    private val stdoutPipePath = "$writableDir/iperfStdout"
    private val stderrPipePath = "$writableDir/iperfStderr"

    var stdoutHandler: (String) -> Unit = {}
    var stderrHandler: (String) -> Unit = {}
    var onFinishHandler: () -> Unit = {}

    @Throws(IperfException::class, InterruptedException::class)
    fun start(args: String) {
        lock.lock()
        if (processWaiterThread != null) {
            sendSigKill()
            finishedCondition.await()
        }

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
            waitForProcessNoDestroy(processPid)
            onFinish(outputHandlers)
        }, "Iperf Waiter")
            .also { it.start() }
        lock.unlock()
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
        lock.lock()
        waitForProcess(processPid)

        runBlocking {
            outputHandlers.forEach {
                it.join()
            }
        }

        onFinishHandler()
        processWaiterThread = null
        processPid = 0
        finishedCondition.signalAll()
        lock.unlock()
    }

    @Throws(IperfException::class, InterruptedException::class)
    fun killAndWait() {
        lock.lock()
        if (processWaiterThread != null) {
            sendSigInt()
            finishedCondition.await()
        }
        lock.unlock()
    }

    @Throws(IperfException::class)
    fun sendSigInt() {
        kill("SIGINT", ::sendSigInt)
    }

    @Throws(IperfException::class)
    fun sendSigKill() {
        kill("SIGKILL", ::sendSigKill)
    }

    private fun kill(signalName: String, sendSignal: (Long) -> Int) {
        lock.lock()
        if (processWaiterThread != null) {
            runCheckingErrno(getKillErrorMessage(signalName)) { sendSignal(processPid) }
        }
        lock.unlock()
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

    private external fun waitForProcessNoDestroy(pid: Long) : Int

    private external fun waitForProcess(pid: Long): Int

    companion object {
        private val SPACES_REGEX = Regex("\\s+")
        private const val LOG_TAG = "IperfRunner"

        init {
            System.loadLibrary("iperf2")
        }
    }
}

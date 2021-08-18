package ru.scoltech.openran.speedtest

import java.io.FileReader

class IperfRunner(writableDir: String) {
    private var processWaiterThread: Thread? = null
    private var inputHandlerThreads: List<Thread>? = null

    private val stdoutPipePath = "$writableDir/iperfStdout"
    private val stderrPipePath = "$writableDir/iperfStderr"

    var stdoutHandler: (String) -> Unit = {}
    var stderrHandler: (String) -> Unit = {}
    var onFinishHandler: () -> Unit = {}

    fun start(args: String) {
        mkfifo(stdoutPipePath)
        mkfifo(stderrPipePath)

        inputHandlerThreads = listOf(
            Triple(stdoutPipePath, "Stdout Handler", stdoutHandler),
            Triple(stderrPipePath, "Stderr Handler", stderrHandler)
        ).map { (pipePath, name, handler) ->
            Thread({
                FileReader(pipePath).buffered().useLines { lines ->
                    lines.forEach {
                        handler(it + System.lineSeparator())
                    }
                }
            }, name).also { it.start() }
        }

        val argsArray = splitArgs(args)
        start(stdoutPipePath, stderrPipePath, argsArray)
        processWaiterThread = Thread({
            waitForProcess()
            onFinish()
        }, "Iperf Waiter")
            .also { it.start() }
    }

    private fun splitArgs(args: String): Array<String> {
        return args.split(SPACES_REGEX).filter { it.isNotBlank() }.toTypedArray()
    }

    private fun onFinish() {
        inputHandlerThreads!!.forEach { it.interrupt() }
        inputHandlerThreads!!.forEach { it.join() }
        inputHandlerThreads = null

        onFinishHandler()
    }

    fun killAndWait() {
        if (processWaiterThread != null) {
            sendSigInt()
            processWaiterThread!!.join()
        }
    }

    external fun sendSigInt()

    external fun sendSigKill()

    private external fun mkfifo(pipePath: String)

    private external fun start(stdoutPipePath: String, stderrPipePath: String, args: Array<String>): Int

    private external fun waitForProcess()

    companion object {
        private val SPACES_REGEX = Regex("\\s+")

        init {
            System.loadLibrary("iperf2")
        }
    }
}

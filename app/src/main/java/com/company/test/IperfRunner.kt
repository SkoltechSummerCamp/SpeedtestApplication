package com.company.test

import java.io.FileReader
import java.io.FileWriter

class IperfRunner(writableFolder: String) {
    private var iperfThread: Thread? = null
    private var inputHandlerThreads: List<Thread>? = null

    private val stdoutPipePath = "$writableFolder/iperfStdout"
    private val stderrPipePath = "$writableFolder/iperfStderr"

    var stdoutHandler: (String) -> Unit = {}
    var stderrHandler: (String) -> Unit = {}

    fun start(args: String) {
        mkfifo(stdoutPipePath)
        mkfifo(stderrPipePath)

        val argsArray = parseIperfArgs(args)
        iperfThread = Thread({
            mainJni(stdoutPipePath, stderrPipePath, argsArray)
        }, "Iperf Thread").also { it.start() }

        inputHandlerThreads = listOf(
            Triple(stdoutPipePath, "Stdout Handler", stdoutHandler),
            Triple(stderrPipePath, "Stderr Handler", stderrHandler)
        ).map { (pipePath, name, handler) ->
            Thread({
                FileReader(pipePath).buffered().useLines { lines ->
                    lines.forEach {
                        if (it == CLOSE_PIPE) {
                            return@useLines
                        }
                        handler(it + System.lineSeparator())
                    }
                }
            }, name).also { it.start() }
        }
    }

    private fun parseIperfArgs(args: String): Array<String> {
        return args.split(Regex("\\s+")).filter { it.isNotBlank() }.toTypedArray()
    }

    fun stop() {
        stopJni()
        iperfThread!!.interrupt()
        iperfThread!!.join()
        cleanupJni()

        inputHandlerThreads!!.forEach { it.interrupt() }
        listOf(stdoutPipePath, stderrPipePath).forEach { pipePath ->
            FileWriter(pipePath).buffered().use { it.appendLine(CLOSE_PIPE) }
        }
        inputHandlerThreads!!.forEach { it.join() }

        iperfThread = null
        inputHandlerThreads = null
    }

    private external fun mkfifo(pipePath: String)

    private external fun mainJni(stdoutPipePath: String, stderrPipePath: String, args: Array<String>): Int

    private external fun stopJni()

    private external fun cleanupJni()

    companion object {
        const val CLOSE_PIPE = "CLOSE PIPE"

        init {
            System.loadLibrary("iperf2")
        }
    }
}

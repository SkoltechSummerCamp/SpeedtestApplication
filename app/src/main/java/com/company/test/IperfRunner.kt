package com.company.test

import android.view.View
import java.io.FileReader
import java.io.FileWriter

class IperfRunner(writableFolder: String, private val mainActivity: MainActivity) {
    private var iperfThread: Thread? = null
    private var inputHandlerThreads: List<Thread>? = null

    private val stdoutPipePath = "$writableFolder/iperfStdout"
    private val stderrPipePath = "$writableFolder/iperfStderr"

    init {
        mainActivity.binding.button.setOnClickListener(::start)
    }

    private fun start(view: View) {
        mkfifo(stdoutPipePath)
        mkfifo(stderrPipePath)

        val argsArray = parseIperfArgs(mainActivity.binding.iperfArgs.text.toString())
        iperfThread = Thread({
            mainJni(stdoutPipePath, stderrPipePath, argsArray)
        }, "Iperf Thread").also { it.start() }

        inputHandlerThreads = listOf(
            stdoutPipePath to "Stdout Handler",
            stderrPipePath to "Stderr Handler"
        ).map { (pipePath, name) ->
            Thread({
                FileReader(pipePath).buffered().useLines { lines ->
                    lines.forEach {
                        if (it == CLOSE_PIPE) {
                            return@useLines
                        }
                        mainActivity.runOnUiThread {
                            mainActivity.binding.iperfOutput.append(it + System.lineSeparator())
                        }
                    }
                }
            }, name).also { it.start() }
        }
        mainActivity.binding.iperfArgs.isEnabled = false
        mainActivity.binding.button.text = mainActivity.applicationContext.getString(R.string.stopIperf)
        mainActivity.binding.button.setOnClickListener(::stop)
    }

    private fun stop(view: View) {
        stopJni()
        iperfThread!!.interrupt()
        iperfThread!!.join()
        cleanupJni()

        inputHandlerThreads!!.forEach { it.interrupt() }
        listOf(stdoutPipePath, stderrPipePath).forEach { pipePath ->
            FileWriter(pipePath).buffered().use { it.appendLine(CLOSE_PIPE) }
        }
        inputHandlerThreads!!.forEach { it.join() }

        // TODO uncomment
//        mainActivity.binding.iperfOutput.text = ""
        mainActivity.binding.button.text = mainActivity.applicationContext.getString(R.string.startIperf)
        mainActivity.binding.iperfArgs.isEnabled = true
        iperfThread = null
        inputHandlerThreads = null
        mainActivity.binding.iperfArgs.text.clear()
        mainActivity.binding.button.setOnClickListener(::start)
    }

    private fun parseIperfArgs(args: String): Array<String> {
        return args.split(Regex("\\s+")).filter { it.isNotBlank() }.toTypedArray()
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

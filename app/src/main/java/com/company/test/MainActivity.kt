package com.company.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import com.company.test.databinding.ActivityMainBinding
import java.io.FileReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fifoPath = "${applicationContext.filesDir.absolutePath}/iperfPipe"
        mkfifo(fifoPath)

        binding.button.setOnClickListener {
            binding.button.isVisible = false

            val argsArray = parseArgs(binding.iperfArgs.text.toString())
            Thread { mainJni(fifoPath, argsArray) }.also { it.start() }

            Thread {
                FileReader(fifoPath).buffered().useLines { lines ->
                    lines.forEach {
                        binding.iperfOutput.post {
                            binding.iperfOutput.append(it + System.lineSeparator())
                        }
                    }
                }
            }.also { it.start() }
        }
    }

    private fun parseArgs(args: String): Array<String> {
        return args.split(Regex("\\s+")).toTypedArray()
    }

    private external fun mkfifo(fifoPath: String)

    private external fun mainJni(fifoPath: String, args: Array<String>): Int

    companion object {
        init {
            System.loadLibrary("iperf2")
        }
    }
}

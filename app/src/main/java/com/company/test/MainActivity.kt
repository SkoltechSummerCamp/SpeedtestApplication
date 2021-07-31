package com.company.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.company.test.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fifoPath = "${applicationContext.filesDir.absolutePath}/helloPipe"
        mkfifo(fifoPath)
        Thread { mainJni(fifoPath) }.also { it.start() }

        Thread {
            FileReader(fifoPath).buffered().useLines { lines ->
                lines.forEach {
                    binding.sampleText.post {
                        binding.sampleText.text = it
                    }
                }
            }
        }.also { it.start() }
    }

    private external fun mkfifo(fifoPath: String)

    private external fun mainJni(fifoPath: String): Int

    companion object {
        init {
            System.loadLibrary("hello_world")
        }
    }
}

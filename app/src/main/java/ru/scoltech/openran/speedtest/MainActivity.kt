package ru.scoltech.openran.speedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import ru.scoltech.openran.speedtest.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var iperfRunner: IperfRunner
    var isPingInChecking = false

    @Volatile
    var pingValueBuffer = "---"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ArrayAdapter.createFromResource(
            this,
            R.array.commands,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter
        }

        binding.refreshButton.setOnClickListener { refreshAddresses() }
        refreshAddresses()

        iperfRunner = IperfRunner(applicationContext.filesDir.absolutePath).also {
            it.stdoutHandler = ::handleIperfOutput
            it.stderrHandler = ::handleIperfOutput
        }
        binding.startStopButton.setOnClickListener { startIperf() }
        binding.pingTestButt.setOnClickListener {
            pingTestButtonAction()
        }
    }

    private fun pingTestButtonAction() = runBlocking {
        if (!isPingInChecking) {
            isPingInChecking = true
            binding.pingTestButt.text = "TESTING..."
            CoroutineScope(Dispatchers.IO).launch {
                doPingTest(
                    { value: String -> pingValueBuffer = value },
                    binding.serverIP.text.toString()
                )
                isPingInChecking = false
            }
            CoroutineScope(Dispatchers.Main).launch {
                do {
                    delay(1)
                    binding.pingValue.text = pingValueBuffer
                } while (isPingInChecking)
                if (binding.pingValue.text != pingValueBuffer)
                    binding.pingValue.text = pingValueBuffer
                binding.pingTestButt.text = "TEST PING"
            }
        }
    }


    private fun refreshAddresses() {
        binding.ipInfo.text = NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.inetAddresses.hasMoreElements() }
            .joinToString(separator = System.lineSeparator()) { networkInterface ->
                val addresses = networkInterface.inetAddresses.toList()
                    .filterIsInstance<Inet4Address>()
                    .joinToString(separator = ", ")
                "${networkInterface.displayName}: $addresses"
            }
    }

    private fun handleIperfOutput(text: String) {
        runOnUiThread {
            binding.iperfOutput.append(text)
        }
    }

    private fun startIperf() {
        iperfRunner.start(binding.iperfArgs.text.toString())

        binding.iperfArgs.isEnabled = false
        binding.startStopButton.text = applicationContext.getString(R.string.stopIperf)
        binding.startStopButton.setOnClickListener { stopIperf() }
    }

    private fun stopIperf() {
        iperfRunner.stop()

        binding.iperfArgs.isEnabled = true
        binding.startStopButton.text = applicationContext.getString(R.string.startIperf)
        binding.startStopButton.setOnClickListener { startIperf() }
    }
}

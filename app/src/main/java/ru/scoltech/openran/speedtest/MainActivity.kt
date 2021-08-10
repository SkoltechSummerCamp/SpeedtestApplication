package ru.scoltech.openran.speedtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ArrayAdapter
import ru.scoltech.openran.speedtest.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val PING_SERVER_UDP_PORT = 49121
    lateinit var binding: ActivityMainBinding
    lateinit var iperfRunner: IperfRunner

    @Volatile
    private lateinit var pcs: PingCheckServer

    private val justStopICMPPingPingFlag = AtomicBoolean(false)
    private val justICMPPingInChecking = AtomicBoolean(false)
    private val isPingInChecking = AtomicBoolean(false)
    private val stopPingFlag = AtomicBoolean(false)
    private val isPingICMPInChecking = AtomicBoolean(false)
    private val pingServerIsRunning = AtomicBoolean(false)

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
        binding.checkIcmpPingButt.setOnClickListener {
            startICMPPing()
        }
        binding.iperfOutput.movementMethod = ScrollingMovementMethod()

        binding.justPingButt.setOnClickListener {
            justICMPPing()
        }
        binding.pingServerButt.setOnClickListener {
            pingServerButtonAction()
        }
    }

    private fun pingTestButtonAction() = runBlocking {
        if (!isPingInChecking.get()) {
            isPingInChecking.set(true)
            binding.pingTestButt.text = getString(R.string.pingTesting)
            CoroutineScope(Dispatchers.IO).launch {
                doPingTest(
                    { value: String -> pingValueBuffer = value },
                    binding.serverIP.text.toString()
                )
                isPingInChecking.set(false)
            }
            CoroutineScope(Dispatchers.Main).launch {
                do {
                    delay(1)
                    binding.pingValue.text = pingValueBuffer
                } while (isPingInChecking.get() || binding.pingValue.text != pingValueBuffer)
                binding.pingTestButt.text = getString(R.string.testPing)
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

    private fun startICMPPing() = runBlocking {
        if (!isPingICMPInChecking.get()) {
            val pinger = ICMPPing()
            isPingICMPInChecking.set(true)
            val host = binding.serverIP.text.toString()
            val stringDeque: ConcurrentLinkedDeque<String> = ConcurrentLinkedDeque()

            CoroutineScope(Dispatchers.IO).launch {
                pinger.performPingWithArgs(host, stringDeque)
                isPingICMPInChecking.set(false)
            }

            CoroutineScope(Dispatchers.Main).launch {
                binding.checkIcmpPingButt.text = getString(R.string.bigStop)
                while ((isPingICMPInChecking.get() || stringDeque.isNotEmpty())) {
                    delay(10)
                    if (stringDeque.isNotEmpty())
                        binding.iperfOutput.append(stringDeque.removeFirst() + "\n")
                    if (stopPingFlag.get()) {
                        pinger.stopExecuting()
                        stopPingFlag.set(false)
                    }
                }
                binding.checkIcmpPingButt.text = getString(R.string.performAsCommand)
            }
        } else {
            stopPingFlag.set(true)
        }
    }

    private fun pingServerButtonAction() = runBlocking {
        if (!pingServerIsRunning.get()) {
            pingServerIsRunning.set(true)
            binding.pingServerButt.text = getString(R.string.bigStop)
            CoroutineScope(Dispatchers.IO).launch {
                pcs = PingCheckServer(PING_SERVER_UDP_PORT)
                pcs.start()
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                if(pcs.isAlive)
                    pcs.interrupt()
                binding.pingServerButt.text = getString(R.string.startUdpPingServer)
                pingServerIsRunning.set(false)
            }
        }
    }

    private fun justICMPPing() = runBlocking {
        if (!justICMPPingInChecking.get()) {
            justICMPPingInChecking.set(true)
            binding.justPingButt.text = getString(R.string.bigStop)
            val pinger = ICMPPing()
            CoroutineScope(Dispatchers.IO).launch {
                pinger.justPingByHost(
                    binding.serverIP.text.toString()
                ) { value: String -> pingValueBuffer = value }
                justICMPPingInChecking.set(false)
            }
            CoroutineScope(Dispatchers.Main).launch {
                do {
                    delay(10)
                    binding.pingValue.text = pingValueBuffer
                    if (justStopICMPPingPingFlag.get()) {
                        pinger.stopExecuting()
                        justStopICMPPingPingFlag.set(false)
                    }
                } while (justICMPPingInChecking.get() || binding.pingValue.text != pingValueBuffer)
                binding.justPingButt.text = getString(R.string.justPing)
            }
        } else {
            justStopICMPPingPingFlag.set(true)
        }
    }
}

package com.company.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.company.test.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        IperfRunner(applicationContext.filesDir.absolutePath, this)
    }
}

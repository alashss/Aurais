package com.auraai.overlay.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auraai.overlay.R
import com.auraai.overlay.databinding.ActivityMainBinding
import com.auraai.overlay.service.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_OVERLAY = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkOverlayPermission()
    }

    private fun setupUI() {
        binding.btnStart.setOnClickListener {
            if (canDrawOverlays()) startOverlay()
            else requestOverlayPermission()
        }

        binding.btnStop.setOnClickListener {
            stopOverlay()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.cardProvider.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkOverlayPermission() {
        if (!canDrawOverlays()) {
            binding.statusCard.setBackgroundResource(R.drawable.bg_status_warning)
            binding.tvStatus.text = "Izin overlay diperlukan"
            binding.tvStatusDesc.text = "Tap tombol di bawah untuk mengaktifkan"
            binding.btnStart.text = "Izinkan Overlay"
        } else {
            updateStatusRunning(OverlayService.isRunning)
        }
    }

    private fun updateStatusRunning(running: Boolean) {
        if (running) {
            binding.statusCard.setBackgroundResource(R.drawable.bg_status_active)
            binding.tvStatus.text = "AuraAI Aktif"
            binding.tvStatusDesc.text = "Bubble berjalan di atas semua app"
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        } else {
            binding.statusCard.setBackgroundResource(R.drawable.bg_status_idle)
            binding.tvStatus.text = "AuraAI Siap"
            binding.tvStatusDesc.text = "Tap mulai untuk mengaktifkan bubble"
            binding.btnStart.visibility = View.VISIBLE
            binding.btnStart.text = "Mulai Bubble"
            binding.btnStop.visibility = View.GONE
        }
    }

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(this)
        else true

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Izin Overlay Dibutuhkan")
            .setMessage("AuraAI perlu izin 'Tampil di atas app lain' agar bubble bisa muncul saat kamu mengerjakan soal ujian.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatusRunning(true)
        // Minimize ke belakang
        moveTaskToBack(true)
    }

    private fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
        updateStatusRunning(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            if (canDrawOverlays()) {
                Toast.makeText(this, "Izin diberikan! Siap dipakai.", Toast.LENGTH_SHORT).show()
                updateStatusRunning(false)
            } else {
                Toast.makeText(this, "Izin diperlukan untuk menjalankan bubble.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
    }
}

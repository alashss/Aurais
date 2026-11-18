package com.auraai.overlay.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auraai.overlay.ai.AiProvider
import com.auraai.overlay.ai.ProviderManager
import com.auraai.overlay.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Pengaturan AuraAI"
            setDisplayHomeAsUpEnabled(true)
        }

        loadSettings()
        setupUI()
    }

    private fun loadSettings() {
        val prefs = ProviderManager.getPrefs(this)
        // Load saved API keys
        binding.etGeminiKey.setText(prefs.getString("key_gemini", ""))
        binding.etGroqKey.setText(prefs.getString("key_groq", ""))
        binding.etMistralKey.setText(prefs.getString("key_mistral", ""))
        binding.etCohereKey.setText(prefs.getString("key_cohere", ""))
        binding.etTogetherKey.setText(prefs.getString("key_together", ""))
        binding.etOpenaiKey.setText(prefs.getString("key_openai", ""))

        // Load provider selection
        val provider = prefs.getString("selected_provider", "auto") ?: "auto"
        val idx = AiProvider.entries.indexOfFirst { it.id == provider }.coerceAtLeast(0)
        binding.spinnerProvider.setSelection(idx)

        // Load KKM
        binding.etKkm.setText(prefs.getInt("kkm", 75).toString())

        // Load stealth mode
        binding.switchStealth.isChecked = prefs.getBoolean("stealth_mode", false)

        // Load language
        binding.switchBahasaIndo.isChecked = prefs.getBoolean("lang_id", true)
    }

    private fun setupUI() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestProvider.setOnClickListener {
            testProvider()
        }
    }

    private fun saveSettings() {
        val prefs = ProviderManager.getPrefs(this)
        val editor = prefs.edit()

        editor.putString("key_gemini", binding.etGeminiKey.text.toString().trim())
        editor.putString("key_groq", binding.etGroqKey.text.toString().trim())
        editor.putString("key_mistral", binding.etMistralKey.text.toString().trim())
        editor.putString("key_cohere", binding.etCohereKey.text.toString().trim())
        editor.putString("key_together", binding.etTogetherKey.text.toString().trim())
        editor.putString("key_openai", binding.etOpenaiKey.text.toString().trim())

        val selectedProvider = AiProvider.entries[binding.spinnerProvider.selectedItemPosition]
        editor.putString("selected_provider", selectedProvider.id)

        val kkm = binding.etKkm.text.toString().toIntOrNull() ?: 75
        editor.putInt("kkm", kkm.coerceIn(0, 100))

        editor.putBoolean("stealth_mode", binding.switchStealth.isChecked)
        editor.putBoolean("lang_id", binding.switchBahasaIndo.isChecked)

        editor.apply()
        Toast.makeText(this, "Pengaturan disimpan ✓", Toast.LENGTH_SHORT).show()
    }

    private fun testProvider() {
        Toast.makeText(this, "Test provider... (cek logcat)", Toast.LENGTH_SHORT).show()
        // Test call di background
        val prefs = ProviderManager.getPrefs(this)
        val provider = AiProvider.entries[binding.spinnerProvider.selectedItemPosition]
        Thread {
            val result = ProviderManager.ask(
                prefs, provider, "Jawab: 1 + 1 = ?"
            )
            runOnUiThread {
                Toast.makeText(this, "Hasil: $result", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

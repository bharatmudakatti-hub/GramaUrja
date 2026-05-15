package com.example.gramaurjaaa

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var timeText: TextView
    private lateinit var zoneSpinner: Spinner
    private lateinit var cropSpinner: Spinner
    private lateinit var acreInput: EditText
    private lateinit var btnOn: Button
    private lateinit var btnOff: Button
    private lateinit var btnCalculate: Button
    private lateinit var timerResultText: TextView

    // Data class to store zone state
    data class ZoneState(val status: String, val lastUpdated: Long)

    // Mock Database: Zone Name -> State
    private val zoneData = mutableMapOf<String, ZoneState>()
    private val zones = arrayOf("Zone A (North Village)", "Zone B (South Village)", "Zone C (West Fields)", "Zone D (Main Transformer)")

    private val handler = Handler(Looper.getMainLooper())
    private val freshnessRunnable = object : Runnable {
        override fun run() {
            updateFreshness()
            handler.postDelayed(this, 10000) // Update every 10 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Mock Data
        zones.forEach { zoneData[it] = ZoneState("OFF", System.currentTimeMillis() - (1000 * 60 * 15)) } // 15 mins ago

        // Initialize Views
        statusText = findViewById(R.id.statusText)
        timeText = findViewById(R.id.timeText)
        zoneSpinner = findViewById(R.id.zoneSpinner)
        cropSpinner = findViewById(R.id.cropSpinner)
        acreInput = findViewById(R.id.acreInput)
        btnOn = findViewById(R.id.btnOn)
        btnOff = findViewById(R.id.btnOff)
        btnCalculate = findViewById(R.id.btnCalculate)
        timerResultText = findViewById(R.id.timerResultText)

        setupSpinners()

        btnOn.setOnClickListener { updateStatus("ON") }
        btnOff.setOnClickListener { updateStatus("OFF") }
        btnCalculate.setOnClickListener { calculatePumpTime() }

        // Change UI when zone changes
        zoneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadZoneState(zones[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Start periodic freshness update
        handler.post(freshnessRunnable)
    }

    private fun setupSpinners() {
        val zoneAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
        zoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        zoneSpinner.adapter = zoneAdapter

        val crops = arrayOf("Rice (Needs High Water)", "Wheat (Needs Medium Water)", "Sugar Cane (Constant Supply)", "Cotton (Low Water Usage)")
        val cropAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, crops)
        cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cropSpinner.adapter = cropAdapter
    }

    private fun loadZoneState(zoneName: String) {
        val state = zoneData[zoneName] ?: return
        displayStatus(state.status, state.lastUpdated)
    }

    private fun updateStatus(status: String) {
        val selectedZone = zoneSpinner.selectedItem.toString()
        val newTime = System.currentTimeMillis()
        
        // Update Mock DB
        zoneData[selectedZone] = ZoneState(status, newTime)
        
        displayStatus(status, newTime)
        
        val message = if (status == "ON") "Power reported ON for $selectedZone" else "Power reported OFF for $selectedZone"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun displayStatus(status: String, lastUpdated: Long) {
        if (status == "ON") {
            statusText.text = "⚡ POWER IS ON"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.power_on_green))
        } else {
            statusText.text = "❌ POWER IS OFF"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.power_off_red))
        }
        updateFreshness()
    }

    private fun updateFreshness() {
        val selectedZone = zoneSpinner.selectedItem?.toString() ?: return
        val lastUpdated = zoneData[selectedZone]?.lastUpdated ?: return
        
        val diff = System.currentTimeMillis() - lastUpdated
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val timeString = when {
            seconds < 30 -> "Updated: Just now"
            seconds < 60 -> "Updated: $seconds sec ago"
            minutes < 60 -> "Updated: $minutes min ago"
            else -> "Updated: $hours hour ago"
        }
        timeText.text = timeString
    }

    private fun calculatePumpTime() {
        val acresStr = acreInput.text.toString()
        if (acresStr.isEmpty()) {
            Toast.makeText(this, "Please enter land area in acres", Toast.LENGTH_SHORT).show()
            return
        }

        val acres = acresStr.toDoubleOrNull() ?: 0.0
        val selectedCrop = cropSpinner.selectedItem.toString()
        
        // Base hours per acre
        val hoursPerAcre = when {
            selectedCrop.contains("Rice") -> 3.0
            selectedCrop.contains("Wheat") -> 1.5
            selectedCrop.contains("Sugar") -> 4.0
            else -> 0.75
        }

        val totalHours = hoursPerAcre * acres
        val formattedResult = String.format("%.1f", totalHours)
        
        timerResultText.text = "Estimated Pump Run Time for $acres Acres of $selectedCrop: $formattedResult Hours"
        timerResultText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(freshnessRunnable)
    }
}

package com.example.lbsapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lbsapp.dashboard.DashboardActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Direkt zum Dashboard weiterleiten
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish() // MainActivity beenden
    }
}
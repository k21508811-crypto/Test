package ru.proverka29.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.versionText).text = "Версия приложения: 1.0 (1)"
        findViewById<Button>(R.id.supportButton).setOnClickListener {
            // Адрес почты не был предоставлен — не подставляем выдуманный адрес.
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:?subject=Поддержка%20Проверка.29")))
        }
        findViewById<Button>(R.id.callButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+78000000000")))
        }
        findViewById<Button>(R.id.browserButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://proverka29.ru")))
        }
    }
}

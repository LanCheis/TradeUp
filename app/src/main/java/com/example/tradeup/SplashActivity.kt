package com.example.tradeup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tradeup.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lấy đối tượng Firebase Auth
        val auth = FirebaseAuth.getInstance()

        // Kiểm tra xem có người dùng nào đang đăng nhập không
        if (auth.currentUser == null) {
            // Nếu KHÔNG, chuyển đến màn hình đăng nhập
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Nếu CÓ, chuyển đến màn hình chính
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Quan trọng: Kết thúc SplashActivity để người dùng không thể
        // nhấn nút "Back" để quay lại màn hình này.
        finish()
    }
}
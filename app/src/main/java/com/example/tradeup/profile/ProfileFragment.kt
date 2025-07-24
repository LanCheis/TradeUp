package com.example.tradeup.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tradeup.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val textView = TextView(context)
        textView.text = "Profile Feature Coming Soon"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}
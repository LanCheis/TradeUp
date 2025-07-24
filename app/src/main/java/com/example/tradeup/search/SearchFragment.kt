package com.example.tradeup.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tradeup.R

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val textView = TextView(context)
        textView.text = "Search Feature Coming Soon"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}
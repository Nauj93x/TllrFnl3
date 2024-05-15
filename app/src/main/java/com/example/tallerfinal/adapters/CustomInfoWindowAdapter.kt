package com.example.tallerfinal.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.tallerfinal.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {
    override fun getInfoContents(p0: Marker): View? {
        return null
    }

    override fun getInfoWindow(p0: Marker): View? {

        val view = LayoutInflater.from(context).inflate(R.layout.marker_with_address, null)


        val addressTextView = view.findViewById<TextView>(R.id.addressText)
        addressTextView.text = p0.snippet

        val title = p0.snippet
        val titleText = view.findViewById<TextView>(R.id.textTitleMarkerView)
        titleText.text = title
        return view
    }
}


package com.bojie.weatherbo.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bojie.weatherbo.R

class AlertDialogFragment : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.error_title))
            .setMessage(context.getString(R.string.error_message))
            .setPositiveButton(context.getString(R.string.error_ok_button_text), null)
            
        return builder.create()
    }
} 
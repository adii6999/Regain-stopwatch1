package com.regain.stopwatch

import android.content.Context
import android.app.AlertDialog

object AlertDialogCompat {
    fun show(context: Context, message: String) {
        AlertDialog.Builder(context).setTitle("Regain History").setMessage(message).setPositiveButton("OK", null).show()
    }
}

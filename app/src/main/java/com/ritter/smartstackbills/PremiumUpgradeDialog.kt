package com.ritter.smartstackbills

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

object PremiumUpgradeDialog {
    fun show(
        activity: AppCompatActivity,
        @StringRes messageResId: Int,
        userEmail: String? = null,
        finishHost: Boolean = false
    ) {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_premium_upgrade)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setOnCancelListener {
            if (finishHost) activity.finish()
        }

        dialog.findViewById<TextView>(R.id.tvPremiumUpgradeMessage).setText(messageResId)
        dialog.findViewById<Button>(R.id.btnPremiumUpgrade).setOnClickListener {
            val intent = Intent(activity, Premium::class.java)
            userEmail?.let { intent.putExtra(AuthUtils.EXTRA_USER_EMAIL, it) }
            activity.startActivity(intent)
            dialog.dismiss()
            if (finishHost) activity.finish()
        }
        dialog.findViewById<Button>(R.id.btnPremiumUpgradeCancel).setOnClickListener {
            dialog.dismiss()
            if (finishHost) activity.finish()
        }

        dialog.show()
        val width = (activity.resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

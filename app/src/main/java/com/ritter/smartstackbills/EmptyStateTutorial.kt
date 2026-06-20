package com.ritter.smartstackbills

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

data class EmptyStateConfig(
    val preferenceKey: String,
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val addActionRes: Int,
    val requiresPremium: Boolean = false,
    @StringRes val premiumActionRes: Int = R.string.explore_premium,
    @StringRes val compactTitleRes: Int = titleRes,
    @StringRes val compactMessageRes: Int = messageRes
)

object EmptyStateTutorial {
    private const val PREFS_NAME = "empty_state_tutorials"

    fun bind(
        activity: AppCompatActivity,
        container: View,
        config: EmptyStateConfig,
        hasAnyEntries: Boolean,
        hasFilteredEntries: Boolean,
        hasPremiumAccess: Boolean,
        onAdd: () -> Unit,
        onPremium: () -> Unit
    ) {
        if (hasFilteredEntries) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        val image = container.findViewById<ImageView>(R.id.emptyStateImage)
        val title = container.findViewById<TextView>(R.id.emptyStateTitle)
        val message = container.findViewById<TextView>(R.id.emptyStateMessage)
        val action = container.findViewById<Button>(R.id.emptyStateAction)
        val help = container.findViewById<ImageButton>(R.id.emptyStateHelp)

        if (hasAnyEntries) {
            image.visibility = View.GONE
            message.visibility = View.GONE
            action.visibility = View.GONE
            help.visibility = View.GONE
            title.setText(R.string.no_entries_match_filter)
            return
        }

        image.visibility = View.VISIBLE
        message.visibility = View.VISIBLE
        action.visibility = View.VISIBLE
        help.visibility = View.VISIBLE
        image.setImageResource(config.imageRes)
        title.setText(config.compactTitleRes)
        message.setText(config.compactMessageRes)
        val canAdd = !config.requiresPremium || hasPremiumAccess
        action.setText(if (canAdd) config.addActionRes else config.premiumActionRes)
        action.setOnClickListener { if (canAdd) onAdd() else onPremium() }
        help.setOnClickListener {
            showDialog(activity, config, hasPremiumAccess, onAdd, onPremium)
        }
    }

    fun showFirstTimeIfNeeded(
        activity: AppCompatActivity,
        config: EmptyStateConfig,
        hasPremiumAccess: Boolean,
        onAdd: () -> Unit,
        onPremium: () -> Unit
    ) {
        val preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getBoolean(config.preferenceKey, false)) return
        preferences.edit().putBoolean(config.preferenceKey, true).apply()
        showDialog(activity, config, hasPremiumAccess, onAdd, onPremium)
    }

    private fun showDialog(
        activity: AppCompatActivity,
        config: EmptyStateConfig,
        hasPremiumAccess: Boolean,
        onAdd: () -> Unit,
        onPremium: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.dialog_empty_state_tutorial)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<ImageView>(R.id.tutorialImage).setImageResource(config.imageRes)
        dialog.findViewById<TextView>(R.id.tutorialTitle).setText(config.titleRes)
        dialog.findViewById<TextView>(R.id.tutorialMessage).setText(config.messageRes)

        val canAdd = !config.requiresPremium || hasPremiumAccess
        dialog.findViewById<Button>(R.id.tutorialPrimaryAction).apply {
            setText(if (canAdd) config.addActionRes else config.premiumActionRes)
            setOnClickListener {
                dialog.dismiss()
                if (canAdd) onAdd() else onPremium()
            }
        }
        dialog.findViewById<Button>(R.id.tutorialSecondaryAction).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        val width = (activity.resources.displayMetrics.widthPixels * 0.9f).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

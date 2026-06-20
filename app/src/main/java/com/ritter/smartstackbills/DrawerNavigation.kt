package com.ritter.smartstackbills

import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

object DrawerNavigation {
    fun setup(activity: AppCompatActivity, drawerLayout: DrawerLayout, navView: NavigationView) {
        val premiumItem = navView.menu.findItem(R.id.nav_item_premium)
        premiumItem?.isChecked = false
        premiumItem?.setTitle(
            if (PremiumAccess.isPremiumUser(activity)) {
                R.string.premium_active
            } else {
                R.string.unlock_premium
            }
        )
        navView.menu.findItem(R.id.nav_item_currency)?.title =
            activity.getString(
                R.string.currency_menu_title,
                CurrencyPreferences.selectedCode(activity)
            )

        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.navHeaderAccountText)?.text =
            FirebaseAuth.getInstance().currentUser?.email ?: activity.getString(R.string.account)
        header.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)

            when (menuItem.itemId) {
                R.id.nav_item_premium -> activity.startActivity(Intent(activity, Premium::class.java))
                R.id.nav_item_aboutus -> activity.startActivity(Intent(activity, AboutUs::class.java))
                R.id.nav_item_faq -> activity.startActivity(Intent(activity, FAQs::class.java))
                R.id.nav_item_datasec -> activity.startActivity(Intent(activity, Datasecurity::class.java))
                R.id.nav_item_help -> activity.startActivity(Intent(activity, Help::class.java))
                R.id.nav_item_terms -> activity.startActivity(Intent(activity, Terms::class.java))
                R.id.nav_item_data_account -> activity.startActivity(Intent(activity, DataAccountActivity::class.java))
                R.id.nav_item_currency -> showCurrencyDialog(activity)
                R.id.nav_item_logout -> {
                    showLogoutConfirmation(activity)
                }
                else -> return@setNavigationItemSelectedListener false
            }
            true
        }
    }

    private fun showCurrencyDialog(activity: AppCompatActivity) {
        val currencies = CurrencyPreferences.availableCurrencies()
        val selectedCode = CurrencyPreferences.selectedCode(activity)
        val labels = currencies.map { currency ->
            val displayName = currency.getDisplayName(activity.resources.configuration.locales[0])
            "${currency.currencyCode} - $displayName"
        }.toTypedArray()
        val selectedIndex = currencies.indexOfFirst { it.currencyCode == selectedCode }

        AlertDialog.Builder(activity)
            .setTitle(R.string.select_currency)
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                CurrencyPreferences.setSelectedCode(activity, currencies[which].currencyCode)
                dialog.dismiss()
                activity.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLogoutConfirmation(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                FirebaseAuth.getInstance().signOut()
                GoogleSignIn.getClient(
                    activity,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(activity.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                ).signOut()
                activity.startActivity(AuthUtils.loginIntent(activity))
                activity.finishAffinity()
            }
            .show()
    }
}

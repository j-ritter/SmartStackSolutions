package com.ritter.smartstackbills

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class PaymentNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_PAID) return
        val userUid = intent.getStringExtra(EXTRA_USER_UID) ?: return
        val billId = intent.getStringExtra(EXTRA_BILL_ID) ?: return
        val pendingResult = goAsync()
        val db = FirebaseFirestore.getInstance()
        val billRef = db.collection("users").document(userUid).collection("bills").document(billId)

        billRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    finish(context, billId, pendingResult)
                    return@addOnSuccessListener
                }
                val closedPayment = document.data.orEmpty().toMutableMap().apply {
                    put("spendingId", billId)
                    put("billId", billId)
                    put("paid", true)
                }
                val spendingRef = db.collection("users").document(userUid)
                    .collection("spendings").document(billId)
                db.runBatch { batch ->
                    batch.set(spendingRef, closedPayment)
                    batch.delete(billRef)
                }
                    .addOnSuccessListener { finish(context, billId, pendingResult) }
                    .addOnFailureListener { pendingResult.finish() }
            }
            .addOnFailureListener { pendingResult.finish() }
    }

    private fun finish(
        context: Context,
        billId: String,
        result: BroadcastReceiver.PendingResult
    ) {
        PaymentNotificationScheduler.cancelBill(context, billId)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(billId.hashCode())
        result.finish()
    }

    companion object {
        const val ACTION_MARK_PAID = "com.ritter.smartstackbills.MARK_PAYMENT_PAID"
        const val EXTRA_USER_UID = "userUid"
        const val EXTRA_BILL_ID = "billId"
    }
}

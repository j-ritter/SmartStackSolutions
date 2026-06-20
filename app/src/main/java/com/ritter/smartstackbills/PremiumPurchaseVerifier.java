package com.ritter.smartstackbills;

import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;

public final class PremiumPurchaseVerifier {
    private static final String TAG = "PremiumVerifier";
    private static boolean verificationInProgress = false;

    private PremiumPurchaseVerifier() {
    }

    public static synchronized void refresh(Context context) {
        if (verificationInProgress) {
            return;
        }
        verificationInProgress = true;

        Context appContext = context.getApplicationContext();
        BillingClient billingClient = BillingClient.newBuilder(appContext)
                .setListener((billingResult, purchases) -> {
                })
                .enablePendingPurchases(
                        com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .enableAutoServiceReconnection()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Billing verification unavailable: " + billingResult.getDebugMessage());
                    finish(billingClient);
                    return;
                }

                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        (queryResult, purchasesList) -> {
                            if (queryResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                                boolean hasActivePremium = false;
                                String purchaseToken = null;
                                for (Purchase purchase : purchasesList) {
                                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                            && purchase.getProducts().contains(PremiumAccess.PRODUCT_ID)) {
                                        hasActivePremium = true;
                                        purchaseToken = purchase.getPurchaseToken();
                                        break;
                                    }
                                }

                                if (hasActivePremium) {
                                    PremiumAccess.grantPremiumAccess(appContext, purchaseToken);
                                } else {
                                    PremiumAccess.revokePremiumAccess(appContext);
                                }
                            } else {
                                Log.w(TAG, "Purchase query failed: " + queryResult.getDebugMessage());
                            }
                            finish(billingClient);
                        });
            }

            @Override
            public void onBillingServiceDisconnected() {
                finish(billingClient);
            }
        });
    }

    private static synchronized void finish(BillingClient billingClient) {
        verificationInProgress = false;
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}

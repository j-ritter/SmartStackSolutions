package com.ritter.smartstackbills;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.*;

import java.util.Collections;
import java.util.List;

public class Premium extends AppCompatActivity {

    private BillingClient billingClient;
    private ProductDetails premiumProduct;
    private String offerToken = ""; // Offer Token for the subscription

    // Listener for purchase updates
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i("BillingClient", "User canceled the purchase.");
        } else {
            Log.e("BillingClient", "Purchase failed: " + billingResult.getDebugMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        // Initialize UI interactions (handles back button)
        handleUIInteractions();

        // Initialize BillingClient
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        // Connect to Google Play Billing
        startBillingConnection();

        // Subscribe Button
        Button subscribeButton = findViewById(R.id.button_premium);
        subscribeButton.setEnabled(false);

        subscribeButton.setOnClickListener(v -> {
            Log.i("BillingClient", "Subscribe button clicked");

            if (!billingClient.isReady()) {
                Log.e("BillingClient", "Billing Client is not ready. Reconnecting...");
                startBillingConnection();
                return;
            }

            if (premiumProduct != null && !offerToken.isEmpty()) {
                purchaseSubscription();
            } else {
                Log.e("BillingClient", "Product details not loaded yet. Fetching now...");
                queryProducts();
            }
        });

        // Check for existing subscription when app opens
        checkExistingSubscription();

        // Handle the back button click
        ImageView backButton = findViewById(R.id.btnBackPremium);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // This will correctly close the activity and return to the previous screen
            }
        });


        // Feature 1: Open Payments
        Button btnMoreInfo1 = findViewById(R.id.btn_more_info1);
        TextView expandableText1 = findViewById(R.id.feature1_expandable_text);
        TextView feature1Description = findViewById(R.id.feature1_description);
        ImageView iconOpenPayments = findViewById(R.id.icon_open_payments);
        TextView feature1Title = findViewById(R.id.feature1_title);

        btnMoreInfo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText1.getVisibility() == View.GONE) {
                    feature1Description.setVisibility(View.GONE);
                    iconOpenPayments.setVisibility(View.GONE);
                    feature1Title.setVisibility(View.GONE);
                    expandableText1.setVisibility(View.VISIBLE);
                    btnMoreInfo1.setText("Less information");
                } else {
                    feature1Description.setVisibility(View.VISIBLE);
                    iconOpenPayments.setVisibility(View.VISIBLE);
                    feature1Title.setVisibility(View.VISIBLE);
                    expandableText1.setVisibility(View.GONE);
                    btnMoreInfo1.setText("More information");
                }
            }
        });

        // Feature 2: Closed Payments
        Button btnMoreInfo2 = findViewById(R.id.btn_more_info2);
        TextView expandableText2 = findViewById(R.id.feature2_expandable_text);
        TextView feature2Description = findViewById(R.id.feature2_description);
        ImageView iconClosedPayments = findViewById(R.id.icon_closed_payments);
        TextView feature2Title = findViewById(R.id.feature2_title);
        ImageView starIconClosedPayments = findViewById(R.id.star_icon_closed_payments);

        btnMoreInfo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText2.getVisibility() == View.GONE) {
                    feature2Description.setVisibility(View.GONE);
                    iconClosedPayments.setVisibility(View.GONE);
                    feature2Title.setVisibility(View.GONE);
                    starIconClosedPayments.setVisibility(View.GONE);
                    expandableText2.setVisibility(View.VISIBLE);
                    btnMoreInfo2.setText("Less information");
                } else {
                    feature2Description.setVisibility(View.VISIBLE);
                    iconClosedPayments.setVisibility(View.VISIBLE);
                    feature2Title.setVisibility(View.VISIBLE);
                    starIconClosedPayments.setVisibility(View.VISIBLE);
                    expandableText2.setVisibility(View.GONE);
                    btnMoreInfo2.setText("More information");
                }
            }
        });

        // Feature 3: Income Management
        Button btnMoreInfo3 = findViewById(R.id.btn_more_info3);
        TextView expandableText3 = findViewById(R.id.feature3_expandable_text);
        TextView feature3Description = findViewById(R.id.feature3_description);
        ImageView iconIncome = findViewById(R.id.icon_income);
        TextView feature3Title = findViewById(R.id.feature3_title);
        ImageView iconStarIncome = findViewById(R.id.icon_star_income);

        btnMoreInfo3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText3.getVisibility() == View.GONE) {
                    feature3Description.setVisibility(View.GONE);
                    iconIncome.setVisibility(View.GONE);
                    feature3Title.setVisibility(View.GONE);
                    iconStarIncome.setVisibility(View.GONE);
                    expandableText3.setVisibility(View.VISIBLE);
                    btnMoreInfo3.setText("Less information");
                } else {
                    feature3Description.setVisibility(View.VISIBLE);
                    iconIncome.setVisibility(View.VISIBLE);
                    feature3Title.setVisibility(View.VISIBLE);
                    expandableText3.setVisibility(View.GONE);
                    iconStarIncome.setVisibility(View.VISIBLE);
                    btnMoreInfo3.setText("More information");
                }
            }
        });

        // Feature 4: Notifications
        Button btnMoreInfo4 = findViewById(R.id.btn_more_info4);
        TextView expandableText4 = findViewById(R.id.feature4_expandable_text);
        TextView feature4Description = findViewById(R.id.feature4_description);
        ImageView iconNotifications = findViewById(R.id.icon_notifications);
        TextView feature4Title = findViewById(R.id.feature4_title);

        btnMoreInfo4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText4.getVisibility() == View.GONE) {
                    feature4Description.setVisibility(View.GONE);
                    iconNotifications.setVisibility(View.GONE);
                    feature4Title.setVisibility(View.GONE);
                    expandableText4.setVisibility(View.VISIBLE);
                    btnMoreInfo4.setText("Less information");
                } else {
                    feature4Description.setVisibility(View.VISIBLE);
                    iconNotifications.setVisibility(View.VISIBLE);
                    feature4Title.setVisibility(View.VISIBLE);
                    expandableText4.setVisibility(View.GONE);
                    btnMoreInfo4.setText("More information");
                }
            }
        });

        // Feature 5: Calendar
        Button btnMoreInfo5 = findViewById(R.id.btn_more_info5);
        TextView expandableText5 = findViewById(R.id.feature5_expandable_text);
        TextView feature5Description = findViewById(R.id.feature5_description);
        ImageView iconCalendar = findViewById(R.id.icon_calendar);
        TextView feature5Title = findViewById(R.id.feature5_title);

        btnMoreInfo5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText5.getVisibility() == View.GONE) {
                    feature5Description.setVisibility(View.GONE);
                    iconCalendar.setVisibility(View.GONE);
                    feature5Title.setVisibility(View.GONE);
                    expandableText5.setVisibility(View.VISIBLE);
                    btnMoreInfo5.setText("Less information");
                } else {
                    feature5Description.setVisibility(View.VISIBLE);
                    iconCalendar.setVisibility(View.VISIBLE);
                    feature5Title.setVisibility(View.VISIBLE);
                    expandableText5.setVisibility(View.GONE);
                    btnMoreInfo5.setText("More information");
                }
            }
        });
        // Feature 6: Saving Target
        Button btnMoreInfo6 = findViewById(R.id.btn_more_info6);
        TextView expandableText6 = findViewById(R.id.feature6_expandable_text);
        TextView feature6Description = findViewById(R.id.feature6_description);
        ImageView iconSavingTarget = findViewById(R.id.icon_saving_target);
        TextView feature6Title = findViewById(R.id.feature6_title);
        ImageView starIconSavingTarget = findViewById(R.id.star_icon_saving_target);

        btnMoreInfo6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText6.getVisibility() == View.GONE) {
                    // Hide the short description, title, and icon
                    feature6Description.setVisibility(View.GONE);
                    iconSavingTarget.setVisibility(View.GONE);
                    feature6Title.setVisibility(View.GONE);
                    starIconSavingTarget.setVisibility(View.GONE);

                    // Show the expanded text
                    expandableText6.setVisibility(View.VISIBLE);

                    // Update button text
                    btnMoreInfo6.setText("Less information");
                } else {
                    // Show the short description, title, and icon
                    feature6Description.setVisibility(View.VISIBLE);
                    iconSavingTarget.setVisibility(View.VISIBLE);
                    feature6Title.setVisibility(View.VISIBLE);
                    starIconSavingTarget.setVisibility(View.VISIBLE);

                    // Hide the expanded text
                    expandableText6.setVisibility(View.GONE);

                    // Update button text
                    btnMoreInfo6.setText("More information");
                }
            }
        });
    }

    // Connect to Google Play Billing
    private void startBillingConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i("BillingClient", "Billing setup complete");
                    queryProducts();
                    checkExistingSubscription();
                } else {
                    Log.e("BillingClient", "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BillingClient", "Billing service disconnected. Retrying...");
                startBillingConnection();
            }
        });
    }

    // Query product details from Google Play
    private void queryProducts() {
        if (!billingClient.isReady()) {
            Log.e("BillingClient", "Billing Client is not ready. Trying to reconnect...");
            startBillingConnection();
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("smartstacksolutions_01") // Your Subscription ID
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {
                premiumProduct = productDetailsList.get(0);

                // Extract Offer Token
                if (premiumProduct.getSubscriptionOfferDetails() != null && !premiumProduct.getSubscriptionOfferDetails().isEmpty()) {
                    offerToken = premiumProduct.getSubscriptionOfferDetails().get(0).getOfferToken();
                }

                Log.i("BillingClient", "Product details fetched successfully.");

                runOnUiThread(() -> {
                    Button subscribeButton = findViewById(R.id.button_premium);
                    subscribeButton.setEnabled(true);
                });

            } else {
                Log.e("BillingClient", "Error fetching product details: " + billingResult.getDebugMessage());
            }
        });
    }

    // Launch purchase flow with Offer Token
    private void purchaseSubscription() {
        if (!billingClient.isReady()) {
            Log.e("BillingClient", "Billing Client is not ready");
            return;
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(premiumProduct)
                                .setOfferToken(offerToken) // REQUIRED for subscriptions
                                .build()))
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(this, billingFlowParams);
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingClient", "Failed to launch billing flow: " + billingResult.getDebugMessage());
        }
    }

    // Handle purchase result
    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i("BillingClient", "Subscription acknowledged.");
                        grantPremiumAccess();
                    }
                });
            } else {
                Log.i("BillingClient", "Purchase already acknowledged.");
                grantPremiumAccess();
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.i("BillingClient", "Purchase is pending. Waiting for completion.");
        } else {
            Log.w("BillingClient", "Purchase not completed. State: " + purchase.getPurchaseState());
        }
    }

    // Check existing subscriptions
    private void checkExistingSubscription() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                (billingResult, purchasesList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                        for (Purchase purchase : purchasesList) {
                            handlePurchase(purchase);
                        }
                    } else {
                        Log.e("BillingClient", "Failed to query purchases: " + billingResult.getDebugMessage());
                    }
                }
        );
    }

    private void grantPremiumAccess() {
        runOnUiThread(() -> Toast.makeText(this, "Premium Activated 🎉", Toast.LENGTH_LONG).show());
    }
    private void handleUIInteractions() {
        ImageView backButton = findViewById(R.id.btnBackPremium);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed()); // Recommended Fix
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}

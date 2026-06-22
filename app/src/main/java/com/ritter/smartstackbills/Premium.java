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

public class Premium extends AppCompatActivity {

    private BillingClient billingClient;
    private ProductDetails premiumProduct;
    private String offerToken = ""; // Offer Token for the subscription
    private String displayPrice = "";
    private boolean billingConnectionInProgress = false;
    private boolean premiumUnavailable = false;

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
            showBillingFailure(billingResult);
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
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .enableAutoServiceReconnection()
                .build();

        // Connect to Google Play Billing
        startBillingConnection();

        // Subscribe Button
        Button subscribeButton = findViewById(R.id.button_premium);
        updatePremiumButtonState();

        subscribeButton.setOnClickListener(v -> {
            Log.i("BillingClient", "Subscribe button clicked");

            if (!billingClient.isReady()) {
                Log.e("BillingClient", "Billing Client is not ready. Reconnecting...");
                Toast.makeText(this, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
                startBillingConnection();
                updatePremiumButtonState();
                return;
            }

            if (premiumProduct != null && !offerToken.isEmpty()) {
                purchaseSubscription();
            } else {
                Log.e("BillingClient", "Product details not loaded yet. Fetching now...");
                queryProducts();
            }
        });

        // Handle the back button click
        ImageView backButton = findViewById(R.id.btnBackPremium);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // This will correctly close the activity and return to the previous screen
            }
        });


        setupFeatureDisclosure(R.id.feature1_constraint_layout, R.id.feature1_arrow, R.id.feature1_expandable_text);
        setupFeatureDisclosure(R.id.feature2_constraint_layout, R.id.feature2_arrow, R.id.feature2_expandable_text);
        setupFeatureDisclosure(R.id.feature3_constraint_layout, R.id.feature3_arrow, R.id.feature3_expandable_text);
        setupFeatureDisclosure(R.id.feature4_constraint_layout, R.id.feature4_arrow, R.id.feature4_expandable_text);
        setupFeatureDisclosure(R.id.feature5_constraint_layout, R.id.feature5_arrow, R.id.feature5_expandable_text);
        setupFeatureDisclosure(R.id.feature6_constraint_layout, R.id.feature6_arrow, R.id.feature6_expandable_text);
        setupFeatureDisclosure(R.id.feature7_constraint_layout, R.id.feature7_arrow, R.id.feature7_expandable_text);
    }

    private void setupFeatureDisclosure(int cardId, int arrowId, int expandableTextId) {
        View card = findViewById(cardId);
        ImageView arrow = findViewById(arrowId);
        TextView expandedText = findViewById(expandableTextId);

        card.setOnClickListener(v -> {
            boolean shouldExpand = expandedText.getVisibility() == View.GONE;
            expandedText.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
            arrow.animate().rotation(shouldExpand ? 180f : 0f).setDuration(180L).start();
            card.setContentDescription(getString(
                    shouldExpand ? R.string.hide_details : R.string.show_details
            ));
        });
    }

    // Connect to Google Play Billing
    private void startBillingConnection() {
        if (billingClient == null || billingClient.isReady() || billingConnectionInProgress) {
            return;
        }
        billingConnectionInProgress = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                billingConnectionInProgress = false;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i("BillingClient", "Billing setup complete");
                    premiumUnavailable = false;
                    queryProducts();
                    checkExistingSubscription();
                } else {
                    Log.e("BillingClient", "Billing setup failed: " + billingResult.getDebugMessage());
                    premiumUnavailable = true;
                    runOnUiThread(Premium.this::updatePremiumButtonState);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                billingConnectionInProgress = false;
                Log.w("BillingClient", "Billing service disconnected. Automatic reconnection is enabled.");
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
        premiumUnavailable = false;
        runOnUiThread(this::updatePremiumButtonState);

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(
                        QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PremiumAccess.PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsResult) -> {
            java.util.List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {
                premiumProduct = productDetailsList.get(0);

                // Extract Offer Token
                if (premiumProduct.getSubscriptionOfferDetails() != null && !premiumProduct.getSubscriptionOfferDetails().isEmpty()) {
                    ProductDetails.SubscriptionOfferDetails offer =
                            premiumProduct.getSubscriptionOfferDetails().get(0);
                    offerToken = offer.getOfferToken();
                    if (!offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                        java.util.List<ProductDetails.PricingPhase> phases =
                                offer.getPricingPhases().getPricingPhaseList();
                        displayPrice = phases.get(phases.size() - 1).getFormattedPrice();
                    }
                }

                Log.i("BillingClient", "Product details fetched successfully.");

                runOnUiThread(() -> {
                    updatePremiumButtonState();
                });

            } else {
                Log.e("BillingClient", "Error fetching product details: " + billingResult.getDebugMessage());
                premiumUnavailable = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.premium_unavailable, Toast.LENGTH_SHORT).show();
                    updatePremiumButtonState();
                });
            }
        });
    }

    // Launch purchase flow with Offer Token
    private void purchaseSubscription() {
        if (!billingClient.isReady()) {
            Log.e("BillingClient", "Billing Client is not ready");
            Toast.makeText(this, R.string.billing_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        if (premiumProduct == null || offerToken.isEmpty()) {
            Log.e("BillingClient", "Cannot launch billing flow without product details");
            Toast.makeText(this, R.string.premium_still_loading, Toast.LENGTH_SHORT).show();
            queryProducts();
            updatePremiumButtonState();
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
            showBillingFailure(billingResult);
        }
    }

    private void showBillingFailure(BillingResult billingResult) {
        int message = R.string.premium_unavailable;
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            return;
        }
        if (billingResult.getOnPurchasesUpdatedSubResponseCode()
                == BillingClient.OnPurchasesUpdatedSubResponseCode.PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS) {
            message = R.string.billing_insufficient_funds;
        } else if (billingResult.getOnPurchasesUpdatedSubResponseCode()
                == BillingClient.OnPurchasesUpdatedSubResponseCode.USER_INELIGIBLE) {
            message = R.string.billing_user_ineligible;
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Handle purchase result
    private void handlePurchase(Purchase purchase) {
        if (!purchase.getProducts().contains(PremiumAccess.PRODUCT_ID)) {
            return;
        }

        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i("BillingClient", "Subscription acknowledged.");
                        grantPremiumAccess(purchase);
                    }
                });
            } else {
                Log.i("BillingClient", "Purchase already acknowledged.");
                grantPremiumAccess(purchase);
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.i("BillingClient", "Purchase is pending. Waiting for completion.");
        } else {
            Log.w("BillingClient", "Purchase not completed. State: " + purchase.getPurchaseState());
        }
    }

    // Check existing subscriptions
    private void checkExistingSubscription() {
        if (!billingClient.isReady()) {
            return;
        }

        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                (billingResult, purchasesList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchasesList != null) {
                        boolean hasActivePremium = false;
                        for (Purchase purchase : purchasesList) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                                    && purchase.getProducts().contains(PremiumAccess.PRODUCT_ID)) {
                                hasActivePremium = true;
                                handlePurchase(purchase);
                            }
                        }
                        if (!hasActivePremium) {
                            PremiumAccess.revokePremiumAccess(this);
                            runOnUiThread(this::updatePremiumButtonState);
                        }
                    } else {
                        Log.e("BillingClient", "Failed to query purchases: " + billingResult.getDebugMessage());
                    }
                }
        );
    }


    private void grantPremiumAccess(Purchase purchase) {
        PremiumAccess.grantPremiumAccess(this, purchase.getPurchaseToken());
        runOnUiThread(() -> {
            Toast.makeText(this, R.string.premium_activated, Toast.LENGTH_LONG).show();
            updatePremiumButtonState();
        });
    }

    private void updatePremiumButtonState() {
        Button subscribeButton = findViewById(R.id.button_premium);
        if (subscribeButton == null) {
            return;
        }

        if (PremiumAccess.isPremiumUser(this)) {
            subscribeButton.setText(R.string.premium_active);
            subscribeButton.setEnabled(false);
        } else if (premiumUnavailable) {
            subscribeButton.setText(R.string.premium_unavailable);
            subscribeButton.setEnabled(true);
        } else {
            subscribeButton.setText(premiumProduct != null && !offerToken.isEmpty()
                    ? (displayPrice.isEmpty()
                        ? getString(R.string.unlock_button_text)
                        : getString(R.string.unlock_button_with_price, displayPrice))
                    : getString(R.string.premium_loading));
            subscribeButton.setEnabled(true);
        }
    }

    private void handleUIInteractions() {
        ImageView backButton = findViewById(R.id.btnBackPremium);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed()); // Recommended Fix
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (billingClient == null) {
            return;
        }
        if (billingClient.isReady()) {
            checkExistingSubscription();
        } else {
            startBillingConnection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}

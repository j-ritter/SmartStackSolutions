package com.ritter.smartstackbills;

import android.content.Context;
import android.content.SharedPreferences;

public final class PremiumAccess {
    public static final String PRODUCT_ID = "smartstacksolutions_01";

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_IS_PREMIUM = "isPremiumUser";
    private static final String KEY_PURCHASE_TOKEN = "premiumPurchaseToken";
    private static final String KEY_LAST_VERIFIED_AT = "premiumLastVerifiedAt";

    private PremiumAccess() {
    }

    public static boolean isPremiumUser(Context context) {
        return prefs(context).getBoolean(KEY_IS_PREMIUM, false);
    }

    public static void grantPremiumAccess(Context context, String purchaseToken) {
        prefs(context).edit()
                .putBoolean(KEY_IS_PREMIUM, true)
                .putString(KEY_PURCHASE_TOKEN, purchaseToken)
                .putLong(KEY_LAST_VERIFIED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void revokePremiumAccess(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_IS_PREMIUM, false)
                .remove(KEY_PURCHASE_TOKEN)
                .putLong(KEY_LAST_VERIFIED_AT, System.currentTimeMillis())
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

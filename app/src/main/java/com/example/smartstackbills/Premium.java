package com.example.smartstackbills;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Premium extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        // Handle the back button click
        ImageView backButton = findViewById(R.id.btnBackPremium);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();  // Go back to the previous activity
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
}

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

        btnMoreInfo1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText1.getVisibility() == View.GONE) {
                    expandableText1.setVisibility(View.VISIBLE);
                    btnMoreInfo1.setText("Less information");
                } else {
                    expandableText1.setVisibility(View.GONE);
                    btnMoreInfo1.setText("More information");
                }
            }
        });

        // Feature 2: Closed Payments
        Button btnMoreInfo2 = findViewById(R.id.btn_more_info2);
        TextView expandableText2 = findViewById(R.id.feature2_expandable_text);

        btnMoreInfo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText2.getVisibility() == View.GONE) {
                    expandableText2.setVisibility(View.VISIBLE);
                    btnMoreInfo2.setText("Less information");
                } else {
                    expandableText2.setVisibility(View.GONE);
                    btnMoreInfo2.setText("More information");
                }
            }
        });

        // Feature 3: Income Management
        Button btnMoreInfo3 = findViewById(R.id.btn_more_info3);
        TextView expandableText3 = findViewById(R.id.feature3_expandable_text);

        btnMoreInfo3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText3.getVisibility() == View.GONE) {
                    expandableText3.setVisibility(View.VISIBLE);
                    btnMoreInfo3.setText("Less information");
                } else {
                    expandableText3.setVisibility(View.GONE);
                    btnMoreInfo3.setText("More information");
                }
            }
        });

        // Feature 4: Notifications
        Button btnMoreInfo4 = findViewById(R.id.btn_more_info4);
        TextView expandableText4 = findViewById(R.id.feature4_expandable_text);

        btnMoreInfo4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText4.getVisibility() == View.GONE) {
                    expandableText4.setVisibility(View.VISIBLE);
                    btnMoreInfo4.setText("Less information");
                } else {
                    expandableText4.setVisibility(View.GONE);
                    btnMoreInfo4.setText("More information");
                }
            }
        });

        // Feature 5: Calendar
        Button btnMoreInfo5 = findViewById(R.id.btn_more_info5);
        TextView expandableText5 = findViewById(R.id.feature5_expandable_text);

        btnMoreInfo5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText5.getVisibility() == View.GONE) {
                    expandableText5.setVisibility(View.VISIBLE);
                    btnMoreInfo5.setText("Less information");
                } else {
                    expandableText5.setVisibility(View.GONE);
                    btnMoreInfo5.setText("More information");
                }
            }
        });
        // Feature 6: Saving Target
        Button btnMoreInfo6 = findViewById(R.id.btn_more_info6);
        TextView expandableText6 = findViewById(R.id.feature6_expandable_text);

        btnMoreInfo6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expandableText6.getVisibility() == View.GONE) {
                    expandableText6.setVisibility(View.VISIBLE);
                    btnMoreInfo6.setText("Less information");
                } else {
                    expandableText6.setVisibility(View.GONE);
                    btnMoreInfo6.setText("More information");
                }
            }
        });
    }
}

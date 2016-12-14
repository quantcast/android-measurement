/**
 * © Copyright 2012-2014 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License. Unauthorized use of this file constitutes
 * copyright infringement and violation of law.
 */
package com.quantcast.measurement.service;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutQuantcastScreen extends Activity {

    private static final QCLog.Tag TAG = new QCLog.Tag(AboutQuantcastScreen.class);

    private static final String DIALOG_TITLE = "About Quantcast";
    private static final String OPT_OUT_CHECKBOX_TEXT = "Allow data collection for this app";
    private static final String CLOSE_DIALOG_BUTTON_TEXT = "Proceed";

    private static final int DIALOG_VIEW_TOP_PADDING = 10;
    private static final int DIALOG_VIEW_BOTTOM_PADDING = 10;
    private static final int DIALOG_VIEW_LEFT_PADDING = 35;
    private static final int DIALOG_VIEW_RIGHT_PADDING = 35;
    private static final int DIALOG_MESSAGE_PADDING = 5;
    private static final String FORMATTED_DIALOG_MESSAGE = "Quantcast helps us measure the usage of our app so we can better understand our audience.  Quantcast collects anonymous (non-personally identifiable) data from users across apps, such as details of app usage, the number of visits and duration, their device information, city, and settings, to provide this measurement and behavioral advertising.  A full description of Quantcast’s data collection and use practices can be found in its <a href=\"https://www.quantcast.com/privacy\">Privacy Policy</a>, and you can opt out below.  Please also review our %s privacy policy.";

    private CheckBox m_optOutCheckbox;
    private boolean m_ogAllowsCollection;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_ogAllowsCollection = !QCOptOutUtility.isOptedOut(getApplicationContext());
        setTitle(DIALOG_TITLE);

        String appName = QCUtility.getAppName(this);

        LinearLayout dialogView = new LinearLayout(this);
        dialogView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialogView.setOrientation(LinearLayout.VERTICAL);
        dialogView.setPadding(DIALOG_VIEW_LEFT_PADDING, DIALOG_VIEW_TOP_PADDING, DIALOG_VIEW_RIGHT_PADDING, DIALOG_VIEW_BOTTOM_PADDING);

        TextView dialogMessage = new TextView(this);
        dialogMessage.setMovementMethod(LinkMovementMethod.getInstance());
        dialogMessage.setText(Html.fromHtml(String.format(FORMATTED_DIALOG_MESSAGE, appName)));
        dialogMessage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialogMessage.setPadding(DIALOG_MESSAGE_PADDING, DIALOG_MESSAGE_PADDING, DIALOG_MESSAGE_PADDING, DIALOG_MESSAGE_PADDING);
        dialogMessage.setLinksClickable(true);
        dialogMessage.setTextSize(15);
        dialogMessage.setTextColor(Color.rgb(190, 190, 190));
        dialogView.addView(dialogMessage);

        Button proceedButton = new Button(this);
        proceedButton.setTag(600);
        final Activity activity = this;
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
        LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonLayout.setMargins(0, 15, 0, 15);
        proceedButton.setLayoutParams(buttonLayout);
        proceedButton.setText(CLOSE_DIALOG_BUTTON_TEXT);
        proceedButton.setTextSize(25);
        // Base RGB is 57, 60, 57
        int statePressed = android.R.attr.state_pressed;

        StateListDrawable stateList = new StateListDrawable();
        stateList.addState(new int[]{-statePressed}, new BitmapDrawable(this.getResources(), Bitmap.createBitmap(new int[]{Color.rgb(0, 128, 52)}, 1, 1, Bitmap.Config.ARGB_8888)));
        stateList.addState(new int[]{statePressed}, new BitmapDrawable(this.getResources(), Bitmap.createBitmap(new int[]{Color.rgb(0, 64, 26)}, 1, 1, Bitmap.Config.ARGB_8888)));
        proceedButton.setBackgroundDrawable(stateList);
        proceedButton.setTextColor(Color.WHITE);
        dialogView.addView(proceedButton);

        m_optOutCheckbox = new CheckBox(this);
        m_optOutCheckbox.setTag(500);
        m_optOutCheckbox.setChecked(m_ogAllowsCollection);
        m_optOutCheckbox.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        m_optOutCheckbox.setText(OPT_OUT_CHECKBOX_TEXT);
        m_optOutCheckbox.setTextSize(15);
        m_optOutCheckbox.setTextColor(Color.rgb(190, 190, 190));
        dialogView.addView(m_optOutCheckbox);

        setContentView(dialogView);

    }

    @Override
    protected void onStart() {
        super.onStart();
        QuantcastClient.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        boolean allowsCollection = m_optOutCheckbox.isChecked();
        if (m_ogAllowsCollection != allowsCollection) {
            QCLog.i(TAG, "User opt out status changed to " + !allowsCollection);
            QCMeasurement.INSTANCE.setOptOut(null, !allowsCollection);
        }
        QuantcastClient.activityStop();
    }
}

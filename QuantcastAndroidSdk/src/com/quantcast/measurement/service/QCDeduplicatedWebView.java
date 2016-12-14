package com.quantcast.measurement.service;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * © Copyright 2012-2014 Quantcast Corp.
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License. Unauthorized use of this file constitutes
 * copyright infringement and violation of law.
 */
public class QCDeduplicatedWebView extends WebView implements QCNotificationListener {
    private static final String QCMEASUREMNT_UA_PREFIX = " QuantcastSDK";

    static final Pattern userAgentPattern = Pattern.compile(QCMEASUREMNT_UA_PREFIX + "/(\\d+)_(\\d+)_(\\d+)/[a-zA-Z0-9]{16}-[a-zA-Z0-9]{16}");

    public QCDeduplicatedWebView(Context context) {
        super(context);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        appendUserAgent(!QCOptOutUtility.isOptedOut(context));
    }

    public QCDeduplicatedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        appendUserAgent(!QCOptOutUtility.isOptedOut(context));
    }

    public QCDeduplicatedWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        QCNotificationCenter.INSTANCE.addListener(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED, this);
        appendUserAgent(!QCOptOutUtility.isOptedOut(context));
    }


    @Override
    public void notificationCallback(String notificationName, Object o) {
        if (notificationName.equals(QCOptOutUtility.QC_NOTIF_OPT_OUT_CHANGED)) {
            boolean optedOut = (Boolean) o;
            appendUserAgent(!optedOut);
        }
    }


    void appendUserAgent(boolean add) {
        WebSettings settings = this.getSettings();
        String ogUserAgent = settings.getUserAgentString();
        Matcher m = userAgentPattern.matcher(ogUserAgent);
        boolean found = m.find();

        if (!found && add) {
            settings.setUserAgentString(ogUserAgent + QCMEASUREMNT_UA_PREFIX + "/" + QCUtility.API_VERSION + "/" + QCMeasurement.INSTANCE.getApiKey());
        } else if (found && !add) {
            int start = m.start();
            int end = m.end();
            settings.setUserAgentString(ogUserAgent.substring(0, start) + ogUserAgent.substring(end));
        }

    }
}

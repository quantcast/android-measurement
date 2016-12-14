/*
 * © Copyright 2016 Quantcast Corp.
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

import java.util.HashMap;
import java.util.Map;

public class QCAdvertising {
    private static final String QC_EVENT_AD = "ad";
    private static final String QC_CAMPAIGN_KEY = "campaign";
    private static final String QC_MEDIA_KEY = "media";
    private static final String QC_PLACEMENT_KEY = "placement";

    public static void logAdImpression(String campaignOrNull, String mediaOrNull, String placementOrNull, String[] appLabelsOrNull) {

        String[] impressionLabels = new String[4];
        if (campaignOrNull != null) {
            String campaignLabel = "ad-campaign." + campaignOrNull;
            if (mediaOrNull != null) {
                campaignLabel += "." + mediaOrNull;
            }
            impressionLabels[0] = campaignLabel;
        }

        if (mediaOrNull != null) {
            String mediaLabel = "ad-media." + mediaOrNull;
            impressionLabels[1] = mediaLabel;
        }

        if (placementOrNull != null) {
            String baseLabel = "ad-placement." + placementOrNull;
            if (campaignOrNull != null) {
                String placementCampLabel = baseLabel + ".campaign." + campaignOrNull;
                if (mediaOrNull != null) {
                    placementCampLabel += "." + mediaOrNull;
                }
                impressionLabels[2] = placementCampLabel;
            }

            if (mediaOrNull != null) {
                String placementMediaLabel = baseLabel + ".media." + mediaOrNull;
                impressionLabels[3] = placementMediaLabel;
            }
        }

        String[] combinedLabels = QCUtility.combineLabels(appLabelsOrNull, impressionLabels);

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_AD);
        if (campaignOrNull != null)
            params.put(QC_CAMPAIGN_KEY, campaignOrNull);
        if (mediaOrNull != null)
            params.put(QC_MEDIA_KEY, mediaOrNull);
        if (placementOrNull != null)
            params.put(QC_PLACEMENT_KEY, placementOrNull);

        QCMeasurement.INSTANCE.logOptionalEvent(params, combinedLabels, null);
    }
}

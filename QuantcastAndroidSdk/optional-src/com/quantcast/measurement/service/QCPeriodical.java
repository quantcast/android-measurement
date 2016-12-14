package com.quantcast.measurement.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2016 Quantcast Corp.
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 * compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 */
public class QCPeriodical {

    private static final QCLog.Tag TAG = new QCLog.Tag(QCPeriodical.class);
    static final String QC_PERIODICAL_NAME_KEY = "periodical-name";
    static final String QC_ISSUE_NAME_KEY = "issue-name";
    static final String QC_ISSUE_DATE_KEY = "issue-date";
    static final String QC_ARTICLE_NAME_KEY = "article";
    static final String QC_AUTHOR_NAME_KEY = "authors";
    static final String QC_PAGE_NUM_KEY = "pagenum";

    static final String QC_EVENT_OPEN = "periodical-issue-open";
    static final String QC_EVENT_CLOSE = "periodical-issue-close";
    static final String QC_EVENT_VIEW = "periodical-page-view";
    static final String QC_EVENT_ARTICLE = "periodical-article-view";
    static final String QC_EVENT_DOWNLOAD = "periodical-download";


    static void logAssetDownloadComplete(String periodicalName, String issueName,
                                         Date issueDate, String[] labels) {
        logAssetDownloadComplete(periodicalName, issueName, issueDate, labels, null);
    }


    static void logOpenIssue(String periodicalName, String issueName, Date issueDate, String[] labels){
        logOpenIssue(periodicalName, issueName, issueDate, labels, null);
    }

    static void logCloseIssue(String periodicalName, String issueName, Date issueDate, String[] labels){
        logCloseIssue(periodicalName, issueName, issueDate, labels, null);
    }

    static void logPeriodicalPageView(String periodicalName, String issueName, Date issueDate, int pageNumber, String[] labels){
        logPeriodicalPageView(periodicalName, issueName, issueDate, pageNumber, labels, null);
    }

    static void logPeriodicalArticleView(String periodicalName, String issueName,
                                         Date issueDate, String articleName, String[] authorName, String[] labels){
        logPeriodicalArticleView(periodicalName, issueName, issueDate, articleName, authorName, labels, null);
    }


/********************
 * For use in conjunction with QCNetworkMeasurement only.
 * These methods are exactly the same as above, but they take an optional networkLabels.  These labels are specifically for use with the
 * QCNetworkMeasurement methods.   The networkLabels will be ignored if not used with QCNetworkMeasurement.  Please see method Documentation above
 * for method specific information.
 * ***************/

    static void logAssetDownloadComplete(String periodicalName, String issueName,
                                            Date issueDate, String[] appLabels, String[] networkLabels) {
        if(periodicalName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueDate == null){
            QCLog.e(TAG, "issueDate cannot be null.");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_DOWNLOAD);
        params.put(QC_PERIODICAL_NAME_KEY, periodicalName);
        params.put(QC_ISSUE_NAME_KEY, issueName);
        params.put(QC_ISSUE_DATE_KEY, Long.toString(issueDate.getTime() / 1000));

        QCMeasurement.INSTANCE.logOptionalEvent(params, appLabels, networkLabels);
    }

    static void logOpenIssue(String periodicalName, String issueName, Date issueDate, String[] appLabels, String[] networkLabels){
        if(periodicalName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueDate == null){
            QCLog.e(TAG, "issueDate cannot be null.");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_OPEN);
        params.put(QC_PERIODICAL_NAME_KEY, periodicalName);
        params.put(QC_ISSUE_NAME_KEY, issueName);
        params.put(QC_ISSUE_DATE_KEY, Long.toString(issueDate.getTime() / 1000));

        QCMeasurement.INSTANCE.logOptionalEvent(params, appLabels, networkLabels);
    }

    static void logCloseIssue(String periodicalName, String issueName, Date issueDate, String[] labels, String[] networkLabels){
        if(periodicalName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueDate == null){
            QCLog.e(TAG, "issueDate cannot be null.");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_CLOSE);
        params.put(QC_PERIODICAL_NAME_KEY, periodicalName);
        params.put(QC_ISSUE_NAME_KEY, issueName);
        params.put(QC_ISSUE_DATE_KEY, Long.toString(issueDate.getTime() / 1000));

        QCMeasurement.INSTANCE.logOptionalEvent(params, labels, networkLabels);

    }

    static void logPeriodicalPageView(String periodicalName, String issueName, Date issueDate, int pageNumber,
                                      String[] appLabels, String[] networkLabels){

        if(periodicalName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueDate == null){
            QCLog.e(TAG, "issueDate cannot be null.");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_VIEW);
        params.put(QC_PERIODICAL_NAME_KEY, periodicalName);
        params.put(QC_ISSUE_NAME_KEY, issueName);
        params.put(QC_ISSUE_DATE_KEY, Long.toString(issueDate.getTime() / 1000));
        params.put(QC_PAGE_NUM_KEY, Integer.toString(pageNumber));

        QCMeasurement.INSTANCE.logOptionalEvent(params, appLabels, networkLabels);

    }

    static void logPeriodicalArticleView(String periodicalName, String issueName, Date issueDate,
                                         String articleName, String[] authorName, String[] appLabels, String[] networkLabels){

        if(periodicalName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueName == null){
            QCLog.e(TAG, "issueName cannot be null.");
            return;
        }

        if(issueDate == null){
            QCLog.e(TAG, "issueDate cannot be null.");
            return;
        }

        if(articleName == null){
            QCLog.e(TAG, "articleName cannot be null.");
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put(QCEvent.QC_EVENT_KEY, QC_EVENT_ARTICLE);
        params.put(QC_PERIODICAL_NAME_KEY, periodicalName);
        params.put(QC_ISSUE_NAME_KEY, issueName);
        params.put(QC_ISSUE_DATE_KEY, Long.toString(issueDate.getTime() / 1000));
        params.put(QC_ARTICLE_NAME_KEY, articleName);

        String authorNames = QCUtility.encodeStringArray(authorName);
        if(authorNames != null){
            params.put(QC_AUTHOR_NAME_KEY, authorNames);
        }

        QCMeasurement.INSTANCE.logOptionalEvent(params, appLabels, networkLabels);
    }

}

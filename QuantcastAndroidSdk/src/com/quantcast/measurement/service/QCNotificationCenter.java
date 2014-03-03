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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

enum QCNotificationCenter {
    INSTANCE;

    private final Map<String, List<WeakReference<QCNotificationListener>>> m_bus;

    private QCNotificationCenter() {
        m_bus = new HashMap<String, List<WeakReference<QCNotificationListener>>>();
    }

    /*
       IMPORTANT:  We are using WeakReferences in order to avoid memory loops, any anonymous class
                   listeners will be cleaned up shortly after it is added.
     */
    protected void addListener(String notificationName, QCNotificationListener listener) {
        List<WeakReference<QCNotificationListener>> notif = m_bus.get(notificationName);
        if (notif == null) {
            notif = new ArrayList<WeakReference<QCNotificationListener>>();
        }
        WeakReference<QCNotificationListener> ref = new WeakReference<QCNotificationListener>(listener);
        notif.add(ref);
        m_bus.put(notificationName, notif);
    }

    protected void removeListener(String notificationName, QCNotificationListener listener) {
        List<WeakReference<QCNotificationListener>> notif = m_bus.get(notificationName);
        if (notif != null) {
            for (Iterator<WeakReference<QCNotificationListener>> iterator = notif.iterator();
                 iterator.hasNext(); ) {
                WeakReference<QCNotificationListener> weakRef = iterator.next();
                if (weakRef.get() == null || weakRef.get() == listener) {
                    iterator.remove();
                }
            }
        }
    }

    protected void removeAllListeners(String notificationName) {
        m_bus.remove(notificationName);
    }

    protected void postNotification(String notificationName, Object withObject) {
        List<WeakReference<QCNotificationListener>> notif = m_bus.get(notificationName);
        if (notif != null) {
            for (Iterator<WeakReference<QCNotificationListener>> iterator = notif.iterator();
                 iterator.hasNext(); ) {
                WeakReference<QCNotificationListener> weakRef = iterator.next();
                QCNotificationListener listener = weakRef.get();
                if (listener != null) {
                    listener.notificationCallback(notificationName, withObject);
                } else {
                    iterator.remove();
                }
            }
        }
    }
}

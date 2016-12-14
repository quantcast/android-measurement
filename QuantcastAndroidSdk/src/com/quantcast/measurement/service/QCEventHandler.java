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

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.MessageQueue;
import android.os.PowerManager;
import android.os.Process;

import java.io.PrintWriter;
import java.io.StringWriter;

class QCEventHandler extends HandlerThread {
    private static final QCLog.Tag TAG = new QCLog.Tag(QCEventHandler.class);

    private Handler m_Handler;
    private PowerManager.WakeLock m_wakelock;

    public QCEventHandler() {
        super("com.quantcast.event.handler", Process.THREAD_PRIORITY_BACKGROUND);
    }

    public void setContext(Context context) {
        if (m_wakelock != null) {
            return;
        }

        //wake lock is recommended, but optional
        int res = context.checkCallingOrSelfPermission("android.permission.WAKE_LOCK");
        if (res == PackageManager.PERMISSION_GRANTED) {
            PowerManager pm = (PowerManager) context.getSystemService(
                    Context.POWER_SERVICE);
            m_wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.quantcast.event.wakelock");
            m_wakelock.setReferenceCounted(false);
        }
    }

    public boolean post(Runnable r) {
        if (m_Handler == null) {
            synchronized (this) {
                while (m_Handler == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) { }
                }
            }
        }
        QCLog.i(TAG, "Posting event from queue");
        boolean success = m_Handler.post(new CatchAllRunnable(r));
        if (m_wakelock != null && success) {
            m_wakelock.acquire();
        }
        return success;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        m_Handler = new Handler(this.getLooper());

        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                if (m_wakelock != null) {
                    m_wakelock.release();
                }
                return true;
            }
        });
        synchronized (this) {
            this.notifyAll();
        }
    }

    private static class CatchAllRunnable implements Runnable {
        final Runnable m_delegate;

        public CatchAllRunnable(Runnable delegate) {
            m_delegate = delegate;
        }

        public void run() {
            try {
                m_delegate.run();
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                String stacktrace = sw.toString(); // stack trace as a string
                QCMeasurement.INSTANCE.logSDKError("android-sdk-catchall", t.toString(), stacktrace);
                QCLog.e(TAG, "Catchall exception - " + stacktrace);
            }
        }
    }
}

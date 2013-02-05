/**
* Copyright 2012 Quantcast Corp.
*
* This software is licensed under the Quantcast Mobile App Measurement Terms of Service
* https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
* (the “License”). You may not use this file unless (1) you sign up for an account at
* https://www.quantcast.com and click your agreement to the License and (2) are in
*  compliance with the License. See the License for the specific language governing
* permissions and limitations under the License.
*
*/  
package com.quantcast.measurement.service;

import java.util.List;

import android.content.Context;

import com.quantcast.measurement.event.Event;
import com.quantcast.policy.PolicyEnforcer;

public interface Uploader {

    boolean uploadEvents(Context context, List<? extends Event> events,
            PolicyEnforcer policyEnforcer);

}
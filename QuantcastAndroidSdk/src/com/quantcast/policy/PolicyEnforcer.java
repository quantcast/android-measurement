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
package com.quantcast.policy;

import java.util.List;

import com.quantcast.measurement.event.Event;

public interface PolicyEnforcer {

    /**
     * Attempt to enforce an up-to-date policy on a the provided events. This is done in place.
     * 
     * @param events
     * @return whether or not an up-to-date policy was enforced
     */
    boolean enforePolicy(List<? extends Event> events);

}
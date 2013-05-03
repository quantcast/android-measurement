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
package com.quantcast.measurement.event;

import android.content.Context;

import java.util.Collection;
import java.util.List;

public interface EventDAO {

    List<? extends IdentifiableEvent> getEvents(int maxToRetrieve);

    void removeEvents(Collection<? extends IdentifiableEvent> events);

    void removeAllEvents();

    void writeEvents(Collection<? extends Event> events);

    void deleteDB(Context context);
}
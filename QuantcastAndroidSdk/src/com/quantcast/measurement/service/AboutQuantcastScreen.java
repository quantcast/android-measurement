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

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

import com.quantcast.settings.GlobalControl;
import com.quantcast.settings.GlobalControlListener;
import com.quantcast.settings.GlobalControlProvider;

public class AboutQuantcastScreen extends Activity {
    
    private static final String SAVING_THREAD_NAME = AboutQuantcastScreen.class.getName() + "#saving";
    
    Activity activity = this;
    CheckBox optOutCheckbox;
    
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        QuantcastClient.addActivity(this);
        
        optOutCheckbox = new CheckBox(activity);
        Button proceedButton = new Button(activity);
        View view = ResourceHelper.getDialogView(activity, proceedButton, optOutCheckbox);
        
        proceedButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final boolean blockingEventCollection = optOutCheckbox.isChecked();
                final ProgressDialog progressDialog = ProgressDialog.show(activity, "", "Saving opt out status.");
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        QuantcastGlobalControlProvider.getProvider(activity).saveControl(new GlobalControl(blockingEventCollection));
                        progressDialog.dismiss();
                        activity.finish();
                    }
                    
                }, SAVING_THREAD_NAME).start();
            }

        });
        
        boolean canChangeIcon = requestWindowFeature(Window.FEATURE_LEFT_ICON);
        
        setContentView(view);
        
        if (canChangeIcon) {
            getWindow().setFeatureDrawable(Window.FEATURE_LEFT_ICON, ResourceHelper.getLogo(activity));
        }
        
        setTitle(ResourceHelper.DIALOG_TITLE);
    };
    
    @Override
    protected void onPause() {
        super.onPause();
        
        QuantcastClient.pauseSession();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        QuantcastClient.resumeSession();
        
        GlobalControlProvider globalControlProvider = QuantcastGlobalControlProvider.getProvider(activity);
        globalControlProvider.refresh();
        final ProgressDialog progressDialog = ProgressDialog.show(activity, "", "Retrieving current opt out status.");
        QuantcastGlobalControlProvider.getProvider(activity).getControl(new GlobalControlListener() {
            
            @Override
            public void callback(GlobalControl control) {
                optOutCheckbox.setChecked(control.blockingEventCollection);
                progressDialog.dismiss();
            }
            
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        QuantcastClient.endSession(activity);
    }

}

package com.caterflow.capacitor.netseasy;

import android.app.Activity;

import eu.nets.mia.MiASDK;
import eu.nets.mia.data.MiAPaymentInfo;

public class NetsEasy {

    public void startPayment(Activity activity, String paymentId, String checkoutUrl, String returnUrl, String cancelUrl) {
        MiAPaymentInfo paymentInfo = new MiAPaymentInfo(paymentId, checkoutUrl, returnUrl, cancelUrl);
        MiASDK.getInstance().startSDK(activity, paymentInfo);
    }
}

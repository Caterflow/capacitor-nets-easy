package com.caterflow.capacitor.netseasy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import eu.nets.mia.MiASDK;
import eu.nets.mia.data.MiAResult;
import eu.nets.mia.data.MiAResultCode;

@CapacitorPlugin(
    name = "NetsEasy",
    requestCodes = { NetsEasyPlugin.EASY_SDK_REQUEST_CODE_VALUE }
)
public class NetsEasyPlugin extends Plugin {

    // Must match MiASDK.EASY_SDK_REQUEST_CODE (verified at runtime in load())
    static final int EASY_SDK_REQUEST_CODE_VALUE = 1001;

    private static final String TAG = "NetsEasy";

    private final NetsEasy implementation = new NetsEasy();
    private String savedCallbackId;
    private boolean debug = false;

    @Override
    public void load() {
        debug = getConfig().getBoolean("debug", false);

        // Verify our hardcoded request code matches the SDK's actual value
        if (EASY_SDK_REQUEST_CODE_VALUE != MiASDK.EASY_SDK_REQUEST_CODE) {
            Logger.error(TAG,
                "EASY_SDK_REQUEST_CODE mismatch! Plugin expects " + EASY_SDK_REQUEST_CODE_VALUE +
                " but SDK has " + MiASDK.EASY_SDK_REQUEST_CODE +
                ". Activity results may not be routed correctly.", null);
        }

        if (debug) {
            Logger.debug(TAG, "Debug logging enabled");
        }
    }

    @PluginMethod
    public void startPayment(PluginCall call) {
        String paymentId = call.getString("paymentId");
        if (paymentId == null || paymentId.isEmpty()) {
            call.reject("Missing required parameter: paymentId");
            return;
        }

        String checkoutUrl = call.getString("checkoutUrl");
        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
            call.reject("Missing required parameter: checkoutUrl");
            return;
        }

        String packageName = getContext().getPackageName();
        String returnUrl = call.getString("returnUrl", packageName + "://netseasy/return");
        String cancelUrl = call.getString("cancelUrl", packageName + "://netseasy/cancel");

        if (debug) {
            Logger.debug(TAG, "startPayment called with:" +
                "\n  paymentId = " + paymentId +
                "\n  checkoutUrl = " + checkoutUrl +
                "\n  returnUrl = " + returnUrl +
                "\n  cancelUrl = " + cancelUrl);

            PackageManager pm = getContext().getPackageManager();
            String[] packages = { "dk.danskebank.mobilepay", "no.dnb.vipps" };
            for (String pkg : packages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    Logger.debug(TAG, "Package " + pkg + ": INSTALLED");
                } catch (PackageManager.NameNotFoundException e) {
                    Logger.debug(TAG, "Package " + pkg + ": NOT FOUND");
                }
            }
        }

        // Save the call so we can resolve it when onActivityResult fires
        getBridge().saveCall(call);
        savedCallbackId = call.getCallbackId();

        implementation.startPayment(getActivity(), paymentId, checkoutUrl, returnUrl, cancelUrl);
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        if (debug) {
            Logger.debug(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        }

        if (requestCode != MiASDK.EASY_SDK_REQUEST_CODE) {
            return;
        }

        PluginCall call = getBridge().getSavedCall(savedCallbackId);
        if (call == null) {
            Logger.warn(TAG, "No saved call found for payment result");
            return;
        }

        String paymentId = call.getString("paymentId", "");

        if (resultCode == Activity.RESULT_OK && data != null) {
            MiAResult result = data.getParcelableExtra(MiASDK.BUNDLE_COMPLETE_RESULT);

            if (result != null && result.getMiaResultCode() != null) {
                JSObject ret = new JSObject();
                ret.put("paymentId", paymentId);

                switch (result.getMiaResultCode()) {
                    case RESULT_PAYMENT_COMPLETED:
                        ret.put("status", "completed");
                        if (debug) Logger.debug(TAG, "Payment completed: " + paymentId);
                        break;
                    case RESULT_PAYMENT_CANCELLED:
                        ret.put("status", "cancelled");
                        if (debug) Logger.debug(TAG, "Payment cancelled: " + paymentId);
                        break;
                    case RESULT_PAYMENT_FAILED:
                        ret.put("status", "failed");
                        String errorMessage = "Payment failed";
                        if (result.getMiaError() != null) {
                            errorMessage = result.getMiaError().getErrorMessage();
                        }
                        ret.put("error", errorMessage);
                        if (debug) Logger.debug(TAG, "Payment failed: " + paymentId + ", error: " + errorMessage);
                        break;
                    default:
                        ret.put("status", "failed");
                        ret.put("error", "Unknown result code");
                        if (debug) Logger.debug(TAG, "Payment unknown result code: " + paymentId);
                        break;
                }

                call.resolve(ret);
            } else {
                if (debug) Logger.debug(TAG, "No result from payment SDK: " + paymentId);
                JSObject ret = new JSObject();
                ret.put("status", "failed");
                ret.put("paymentId", paymentId);
                ret.put("error", "No result from payment SDK");
                call.resolve(ret);
            }
        } else {
            // Activity was cancelled without a proper result (e.g. back button before SDK loaded)
            if (debug) Logger.debug(TAG, "Activity cancelled without SDK result: " + paymentId);
            JSObject ret = new JSObject();
            ret.put("status", "cancelled");
            ret.put("paymentId", paymentId);
            call.resolve(ret);
        }

        getBridge().releaseCall(call);
        savedCallbackId = null;
    }
}

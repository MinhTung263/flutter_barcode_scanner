package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.Map;

import io.flutter.embedding.android.FlutterFragmentActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

/**
 * FlutterBarcodeScannerPlugin
 */
public class FlutterBarcodeScannerPlugin implements MethodChannel.MethodCallHandler,
        PluginRegistry.ActivityResultListener,
        EventChannel.StreamHandler,
        FlutterPlugin,
        ActivityAware {

    private static final String CHANNEL = "flutter_barcode_scanner";
    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = FlutterBarcodeScannerPlugin.class.getSimpleName();

    private static FlutterFragmentActivity activity;
    private static MethodChannel.Result pendingResult;
    private Map<String, Object> arguments;

    public static String lineColor = "";
    public static boolean isShowFlashIcon = false;
    public static boolean isContinuousScan = false;

    static EventChannel.EventSink barcodeStream;
    private EventChannel eventChannel;
    private MethodChannel channel;

    private FlutterPlugin.FlutterPluginBinding pluginBinding;
    private ActivityPluginBinding activityBinding;
    private Application applicationContext;
    private Lifecycle lifecycle;
    private LifeCycleObserver observer;

    // Constructor mặc định
    public FlutterBarcodeScannerPlugin() {}

    /**
     * registerWith() cho embedding v1
     */
    public static void registerWith(final PluginRegistry.Registrar registrar) {
        if (registrar.activity() == null) return;

        FlutterBarcodeScannerPlugin instance = new FlutterBarcodeScannerPlugin();
        instance.createPluginSetup(
                registrar.messenger(),
                (Application) registrar.context().getApplicationContext(),
                registrar.activity(),
                registrar,
                null
        );
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        try {
            pendingResult = result;

            if (call.method.equals("scanBarcode")) {
                if (!(call.arguments instanceof Map)) {
                    throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
                }
                arguments = (Map<String, Object>) call.arguments;
                lineColor = (String) arguments.get("lineColor");
                isShowFlashIcon = (boolean) arguments.get("isShowFlashIcon");
                if (lineColor == null || lineColor.isEmpty()) {
                    lineColor = "#DC143C";
                }

                if (arguments.get("scanMode") != null) {
                    int mode = (int) arguments.get("scanMode");
                    if (mode == BarcodeCaptureActivity.SCAN_MODE_ENUM.DEFAULT.ordinal()) {
                        BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
                    } else {
                        BarcodeCaptureActivity.SCAN_MODE = mode;
                    }
                } else {
                    BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
                }

                isContinuousScan = (boolean) arguments.get("isContinuousScan");

                startBarcodeScannerActivity((String) arguments.get("cancelButtonText"), isContinuousScan);
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall: " + e.getLocalizedMessage());
        }
    }

    private void startBarcodeScannerActivity(String buttonText, boolean isContinuousScan) {
        try {
            Intent intent = new Intent(activity, BarcodeCaptureActivity.class)
                    .putExtra("cancelButtonText", buttonText);
            if (isContinuousScan) {
                activity.startActivity(intent);
            } else {
                activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        } catch (Exception e) {
            Log.e(TAG, "startView: " + e.getLocalizedMessage());
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    try {
                        Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        String barcodeResult = barcode.rawValue;
                        pendingResult.success(barcodeResult);
                    } catch (Exception e) {
                        pendingResult.success("-1");
                    }
                } else {
                    pendingResult.success("-1");
                }
                pendingResult = null;
                arguments = null;
                return true;
            } else {
                pendingResult.success("-1");
            }
        }
        return false;
    }

    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        barcodeStream = eventSink;
    }

    @Override
    public void onCancel(Object o) {
        barcodeStream = null;
    }

    public static void onBarcodeScanReceiver(final Barcode barcode) {
        try {
            if (barcode != null && barcode.displayValue != null && !barcode.displayValue.isEmpty()) {
                activity.runOnUiThread(() -> barcodeStream.success(barcode.rawValue));
            }
        } catch (Exception e) {
            Log.e(TAG, "onBarcodeScanReceiver: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        this.pluginBinding = binding;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        this.pluginBinding = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityBinding = binding;
        createPluginSetup(
                pluginBinding.getBinaryMessenger(),
                (Application) pluginBinding.getApplicationContext(),
                binding.getActivity(),
                null, // V2 embedding không cần registrar
                binding
        );
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        clearPluginSetup();
    }

    private void createPluginSetup(BinaryMessenger messenger,
                                   Application applicationContext,
                                   Activity activity,
                                   PluginRegistry.Registrar registrar,
                                   ActivityPluginBinding activityBinding) {

        FlutterBarcodeScannerPlugin.activity = (FlutterFragmentActivity) activity;
        this.applicationContext = applicationContext;

        // Setup EventChannel
        eventChannel = new EventChannel(messenger, "flutter_barcode_scanner_receiver");
        eventChannel.setStreamHandler(this);

        // Setup MethodChannel
        channel = new MethodChannel(messenger, CHANNEL);
        channel.setMethodCallHandler(this);

        if (registrar != null) {
            // V1 embedding
            observer = new LifeCycleObserver(activity);
            applicationContext.registerActivityLifecycleCallbacks(observer);
            registrar.addActivityResultListener(this);
        } else {
            // V2 embedding
            this.activityBinding = activityBinding;
            activityBinding.addActivityResultListener(this);
            lifecycle = io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter.getActivityLifecycle(activityBinding);
            observer = new LifeCycleObserver(activity);
            lifecycle.addObserver(observer);
        }
    }

    private void clearPluginSetup() {
        try {
            if (activityBinding != null) {
                activityBinding.removeActivityResultListener(this);
            }
            if (lifecycle != null && observer != null) {
                lifecycle.removeObserver(observer);
            }
            if (channel != null) {
                channel.setMethodCallHandler(null);
                channel = null;
            }
            if (eventChannel != null) {
                eventChannel.setStreamHandler(null);
                eventChannel = null;
            }
            if (applicationContext != null && observer != null) {
                applicationContext.unregisterActivityLifecycleCallbacks(observer);
            }

        } catch (Exception ignored) {}

        activity = null;
        activityBinding = null;
        lifecycle = null;
        observer = null;
        applicationContext = null;
    }

    private static class LifeCycleObserver implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        private final Activity thisActivity;

        LifeCycleObserver(Activity activity) {
            this.thisActivity = activity;
        }

        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {}

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {}

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {}

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {}

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {}

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {}

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity) {}

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (thisActivity == activity && activity.getApplicationContext() != null) {
                ((Application) activity.getApplicationContext())
                        .unregisterActivityLifecycleCallbacks(this);
            }
        }
    }
}

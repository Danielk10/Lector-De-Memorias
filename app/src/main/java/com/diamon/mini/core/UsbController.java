package com.diamon.mini.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsbController {
    private static final String ACTION_USB_PERMISSION = "com.diamon.mini.USB_PERMISSION";

    public interface Callback {
        void log(String message);
        void onDeviceConnected(String deviceName, int fd, String vidPid, boolean isRecognized);
        void onDeviceConnectionFailed(String deviceName);
        void onDeviceDisconnected();
    }

    // Mapa de VID:PID -> nombre de dispositivo para auto-detección
    public static final Map<String, String> USB_AUTO_MAP = new HashMap<String, String>() {{
        put("04d8:00e0", "TL866II+");
        put("04d8:00de", "TL866A");
        put("04d8:00df", "TL866CS");
        put("1a86:5523", "CH341A");
        put("2e8a:000a", "T48");
        put("2e8a:0005", "T56");
    }};

    private final Activity activity;
    private final UsbManager usbManager;
    private final Callback callback;

    private UsbDeviceConnection currentConnection;
    private int currentFd = -1;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
                    } else {
                        device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connectToDevice(device);
                        }
                    } else {
                        callback.log("Error: Permiso USB denegado.");
                    }
                }
            }
        }
    };

    public UsbController(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            activity.registerReceiver(usbReceiver, filter);
        }
    }

    public void unregisterReceiver() {
        try {
            activity.unregisterReceiver(usbReceiver);
        } catch (Exception ignored) {}
    }

    public int getCurrentFd() {
        return currentFd;
    }

    public boolean isConnected() {
        return currentFd >= 0;
    }

    public void searchAndRequestDevice() {
        Map<String, UsbDevice> devices = usbManager.getDeviceList();
        if (devices == null || devices.isEmpty()) {
            callback.log("Error: No se detectó ningún dispositivo USB.");
            return;
        }

        List<UsbDevice> candidates = new ArrayList<>(devices.values());

        // Auto-selección si hay un dispositivo reconocido
        for (UsbDevice device : candidates) {
            String key = String.format(Locale.US, "%04x:%04x", device.getVendorId(), device.getProductId());
            if (USB_AUTO_MAP.containsKey(key)) {
                String autoName = USB_AUTO_MAP.get(key);
                callback.log("Detección automática: " + key + " reconocido como " + autoName);
                requestUsbPermission(device);
                return;
            }
        }

        Collections.sort(candidates, new Comparator<UsbDevice>() {
            @Override
            public int compare(UsbDevice a, UsbDevice b) {
                int vid = Integer.compare(a.getVendorId(), b.getVendorId());
                return vid != 0 ? vid : Integer.compare(a.getProductId(), b.getProductId());
            }
        });

        callback.log("[AVISO] Dispositivo no reconocido automáticamente como programador minipro.");
        callback.log("Puedes intentar conectarte manualmente.");

        if (candidates.size() == 1) {
            requestUsbPermission(candidates.get(0));
            return;
        }

        CharSequence[] labels = new CharSequence[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            labels[i] = formatUsbDeviceLabel(candidates.get(i));
        }

        new android.app.AlertDialog.Builder(activity)
                .setTitle("Seleccionar dispositivo USB")
                .setItems(labels, (dialog, which) -> requestUsbPermission(candidates.get(which)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void requestUsbPermission(UsbDevice device) {
        String deviceName = device.getProductName() == null ? "Dispositivo USB" : device.getProductName();
        callback.log("Dispositivo detectado: " + deviceName + " | Solicitando enlace...");
        callback.log("VID:PID => "
                + String.format(Locale.US, "%04x:%04x", device.getVendorId(), device.getProductId()));

        if (usbManager.hasPermission(device)) {
            connectToDevice(device);
        } else {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            Intent intent = new Intent(ACTION_USB_PERMISSION);
            intent.setPackage(activity.getPackageName());
            PendingIntent permissionIntent = PendingIntent.getBroadcast(activity, 0, intent, flags);
            usbManager.requestPermission(device, permissionIntent);
        }
    }

    private String formatUsbDeviceLabel(UsbDevice device) {
        String productName = device.getProductName();
        if (productName == null || productName.trim().isEmpty()) {
            productName = "Dispositivo USB";
        }
        String manufacturer = device.getManufacturerName();
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            manufacturer = "Fabricante desconocido";
        }
        return productName + " (" + manufacturer + ")\nVID:PID "
                + String.format(Locale.US, "%04x:%04x", device.getVendorId(), device.getProductId());
    }

    private void connectToDevice(UsbDevice device) {
        currentConnection = usbManager.openDevice(device);
        if (currentConnection == null) {
            callback.onDeviceConnectionFailed(device.getProductName());
            return;
        }

        currentFd = currentConnection.getFileDescriptor();
        String deviceName = device.getProductName() == null ? "Dispositivo USB" : device.getProductName();
        String vidPid = String.format(Locale.US, "%04x:%04x", device.getVendorId(), device.getProductId());

        boolean isRecognized = USB_AUTO_MAP.containsKey(vidPid);
        callback.onDeviceConnected(deviceName, currentFd, vidPid, isRecognized);
    }

    public void disconnectDevice() {
        if (currentConnection != null) {
            try {
                currentConnection.close();
            } catch (Exception ignored) {}
            currentConnection = null;
        }
        currentFd = -1;
        callback.onDeviceDisconnected();
    }
}

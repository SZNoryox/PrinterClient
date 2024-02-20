[中文文档](README-ZH.md)

PrinterClient
==========
### 
The demo for Android Studio has full functionality, such as printing text, printing barcodes, printing qr code, printing pictures, LCD, cash box and scanning. Please import project by Android Studio to get the detailed instructions for use.
###

## Printer SDK integration
Printer SDK is using AIDL integration. About AIDL, please refer to [https://developer.android.com/guide/components/aidl](https://developer.android.com/guide/components/aidl)

### Integration file description
- [net.nyx.printerservice.print.IPrinterService.aidl](app/src/main/aidl/net/nyx/printerservice/print/IPrinterService.aidl) —— the aidl interface for all printer functions
- [net.nyx.printerservice.print.PrintTextFormat.aidl](app/src/main/aidl/net/nyx/printerservice/print/PrintTextFormat.aidl) —— the aidl bean class to set the print text style
- [net.nyx.printerservice.print.PrintTextFormat.java](app/src/main/java/net/nyx/printerservice/print/PrintTextFormat.java) —— the java bean class to set the print text style

### Integration
1. Add the above three files to the project and cannot modify the package and file
2. Add query tag in `AndroidManifest.xml` to adapt `android 11 package visibility` for Android 12 platform
```xml
<queries>
    <package android:name="com.incar.printerservice"/>
</queries>
```
3. Bind printer AIDL service
```
private IPrinterService printerService;
private ServiceConnection connService = new ServiceConnection() {
    @Override
    public void onServiceDisconnected(ComponentName name) {
        showLog("printer service disconnected, try reconnect");
        printerService = null;
        // rebind
        handler.postDelayed(() -> bindService(), 5000);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Timber.d("onServiceConnected: %s", name);
        printerService = IPrinterService.Stub.asInterface(service);
    }
};

private void bindService() {
    Intent intent = new Intent();
    intent.setPackage("com.incar.printerservice");
    intent.setAction("com.incar.printerservice.IPrinterService");
    bindService(intent, connService, Context.BIND_AUTO_CREATE);
}

private void unbindService() {
    unbindService(connService);
}
```

## Printer

### Print text/bitmap/barcode
PrintTextFormat: the bean class to custom text style, like text size, alignment, line spacing, custom font.
```
try {
    PrintTextFormat textFormat = new PrintTextFormat();
    // textFormat.setTextSize(32);
    // textFormat.setUnderline(true);
    int ret = printerService.printText(text, textFormat);
    ret = printerService.printBarcode("123456789", 300, 160, 1, 1, 0);
    ret = printerService.printQrCode("123456789", 300, 300, 1);
    if (ret == 0) {
        printerService.printEndAutoOut();
    }
} catch (RemoteException e) {
    e.printStackTrace();
}
```

For custom print font, **font path needs to be set as a public path**. Font placed in `assets` directory or application private directory will not take effect
```
try {
    PrintTextFormat textFormat = new PrintTextFormat();
    textFormat.setFont(5);
    textFormat.setPath("/sdcard/TLAsc.ttf");
    int ret = printerService.printText(text, textFormat);
} catch (RemoteException e) {
    e.printStackTrace();
}
```

### Print table
```
private void printTable() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret;
                PrintTextFormat formatCenter = new PrintTextFormat();
                formatCenter.setAli(1);
                PrintTextFormat formatLeft = new PrintTextFormat();
                formatLeft.setAli(0);
                PrintTextFormat[] formats = {formatCenter, formatCenter, formatCenter, formatCenter};
                PrintTextFormat[] formats2 = {formatLeft, formatCenter, formatCenter, formatCenter};
                int[] weights = {2, 1, 1, 1};
                String[] row1 = {"ITEM", "QTY", "PRICE", "TOTAL"};
                String[] row2 = {"Apple", "1", "2.00", "2.00"};
                String[] row3 = {"Strawberry", "1", "2.00", "2.00"};
                String[] row4 = {"Watermelon", "1", "2.00", "2.00"};
                String[] row5 = {"Orange", "1", "2.00", "2.00"};
                ret = printerService.printTableText(row1, weights, formats);
                ret = printerService.printTableText(row2, weights, formats2);
                ret = printerService.printTableText(row3, weights, formats2);
                ret = printerService.printTableText(row4, weights, formats2);
                ret = printerService.printTableText(row5, weights, formats2);
                showLog("Print table: " + msg(ret));
                if (ret == 0) {
                    printerService.printEndAutoOut();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
}
```

### Print ESC/POS commands
For details about the common ESC instruction set, please see [ESC/POS Commands](https://download4.epson.biz/sec_pubs/pos/reference_en/escpos/commands.html)

**Note**: This interface is different from other SDK synchronization interfaces. This interface is asynchronous. Calling this interface will only transmit ESC/POS instructions to the instruction queue. The return value indicates that the instruction is successfully enqueued and will not return to the printer status. Do not mix this interface with other interfaces, otherwise the order of printing content will be inconsistent. Here only provides additional options for customers using ESC/POS instructions.
```
private void printEscpos() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                printerService.printEscposData(new byte[]{0x1b, 0x40});
                printerService.printEscposData(new byte[]{0x1b, 0x61, 0x01, 0x1b, 0x21, 48});
                printerService.printEscposData("Receipt\n".getBytes());
                printerService.printEscposData(new byte[]{0x1b, 0x61, 0x00, 0x1b, 0x21, 0x00});
                printerService.printEscposData("\n".getBytes());
                printerService.printEscposData(Utils.printTwoColumn("Order:", System.currentTimeMillis() + ""));
                printerService.printEscposData(Utils.printTwoColumn("Time:", "2024-12-12 12:12:12"));
                printerService.printEscposData("--------------------------------".getBytes());
                printerService.printEscposData(Utils.printTwoColumn("phone", "4999.00"));
                printerService.printEscposData(Utils.printTwoColumn("laptop", "4999.00"));
                printerService.printEscposData("--------------------------------".getBytes());
                printerService.printEscposData(Utils.printTwoColumn("Total:", "9998.00"));
                printerService.printEscposData(Utils.printTwoColumn("Cash:", "10000.00"));
                printerService.printEscposData(Utils.printTwoColumn("Change:", "22.00"));
                printerService.printEscposData(new byte[]{0x1d, 0x56, 0x42, 0x00});
                showLog("Print ESC/POS cmd: " + msg(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
}
```

### Print label
There are two ways to print label

#### 1. Know the exact dimensions of the label (pixels)
The content of the printed label needs to be included between `printerService.labelLocate()` and `printerService.labelPrintEnd()`

```
private void printLabel() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret = printerService.labelLocate(240, 16);
                if (ret == 0) {
                    PrintTextFormat format = new PrintTextFormat();
                    printerService.printText("/nModel:/t/tNB55", format);
                    printerService.printBarcode("1234567890987654321", 320, 90, 2, 0, 0);
                    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    printerService.printText("Time:/t/t" + date, format);
                    ret = printerService.labelPrintEnd();
                }
                showLog("Print label: " + msg(ret));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });
}
```
#### 2. Label learning
Label learning will automatically output label paper for a certain distance to get the params about the label paper. After the interface returns successfully, include the printed content between `printerService.labelLocateAuto()` and `printerService.labelPrintEnd()`
- `printerService.hasLabelLearning()`: whether the system has already performed label learning
- `printerService.clearLabelLearning()`: clear the system storaged the label learning result
```
private void printLabelLearning() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            int ret = 0;
            try {
                if (!printerService.hasLabelLearning()) {
                    // label learning
                    ret = printerService.labelDetectAuto();
                }
                if (ret == 0) {
                    // set label height and gap. using different params by different label type
                    ret = printerService.labelLocateAuto(240, 16);
                    if (ret == 0) {
                        PrintTextFormat format = new PrintTextFormat();
                        printerService.printText("/nModel:/t/tNB55", format);
                        printerService.printBarcode("1234567890987654321", 320, 90, 2, 0, 0);
                        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                        printerService.printText("Time:/t/t" + date, format);
                        printerService.labelPrintEnd();
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            showLog("Label learning print: " + msg(ret));
        }
    });
}
```

### Printer result
All the printer interfaces will return the integer result, please refer to [SdkResult.java](app/src/main/java/net/nyx/printerclient/SdkResult.java)


## LCD customer display
Devices that support the customer display screen can control the LCD. Device without this module will return an error when calling the interface

### LCD control
```
// @param flag 0--init 1--wakeup LCD 2--sleep LCD 3--clear LCD 4--reset LCD display
// int configLcd(int flag);

// wakup
singleThreadExecutor.submit(new Runnable() {
    @Override
    public void run() {
        try {
            // init
            int ret = printerService.configLcd(0);
            if (ret == 0) {
                ret = printerService.configLcd(1);
            }
            showLog("LCD config: " + msg(ret));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
});

// sleep
singleThreadExecutor.submit(new Runnable() {
    @Override
    public void run() {
        try {
            // init
            int ret = printerService.configLcd(0);
            if (ret == 0) {
                ret = printerService.configLcd(2);
            }
            showLog("LCD config: " + msg(ret));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
});

// reset default display
singleThreadExecutor.submit(new Runnable() {
    @Override
    public void run() {
        try {
            // init
            int ret = printerService.configLcd(0);
            if (ret == 0) {
                ret = printerService.configLcd(3);
            }
            showLog("LCD config: " + msg(ret));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
});
```

### LCD display

**The size of the bitmap must be same as the size of the LCD. If the bitmap is smaller than the LCD, it will be shown in the center of LCD**

```
private void showLcdBitmap() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            // 240*320 LCD
            String content = Utils.getRandomStr(100);
            Bitmap bitmap = Utils.createQRCode(content, 220, 220);
            try {
                // init
                int ret = printerService.configLcd(0);
                if (ret == 0) {
                    ret = printerService.showLcdBitmap(bitmap);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

### LCD default logo
```
private void setLcdLogo() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            String content = Utils.getRandomStr(100);
            Bitmap bitmap = Utils.createQRCode(content, 220, 220);
            try {
                // init
                int ret = printerService.configLcd(0);
                if (ret == 0) {
                    ret = printerService.setLcdLogo(bitmap);
                }
                showLog("Set LCD logo: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

## Scanner
### Camera scanner
Just start a system activity to get built-in camera scanner. The capture surface cannot be customized.

```
private void scan() {
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("com.incar.scanner",
            "net.nyx.scanner.ScannerActivity"));
    // set the capture activity actionbar title
    //intent.putExtra("TITLE", "Scan");
    // show album icon, default true
    // intent.putExtra("SHOW_ALBUM", true);
    // play beep sound when get the scan result, default true
    // intent.putExtra("PLAY_SOUND", true);
    // play vibrate when get the scan result, default true
    // intent.putExtra("PLAY_VIBRATE", true);
    startActivityForResult(intent, RC_SCAN);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == RC_SCAN && resultCode == RESULT_OK && data != null) {
        String result = data.getStringExtra("SCAN_RESULT");
        showLog("Scanner result: " + result);
    }
}
```    

### Infrared scan
    
Register the system broadcast to get the infrared scan result
    
```
private final BroadcastReceiver qscReceiver = new BroadcastReceiver() {
                                          
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.NYX_QSC_DATA".equals(intent.getAction())) {
            String qsc = intent.getStringExtra("qsc");
            showLog("qsc scan result: %s", qsc);
            printText("qsc-quick-scan-code\n" + qsc);
        }
}
};

private void registerQscScanReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction("com.android.NYX_QSC_DATA");
    registerReceiver(qscReceiver, filter);
}

private void unregisterQscReceiver() {
    unregisterReceiver(qscReceiver);
}
```

By default, infrared scan will be triggered by the side button. Here is the code for soft trigger

Note: The infrared scan close function has a minimum system version requirement.
```
private void openInfraredScan() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret = printerService.triggerQscScan(0);
                showLog("Open Infrared scan: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}

private void closeInfraredScan() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret = printerService.triggerQscScan(1);
                showLog("Close Infrared scan: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

## Cash box
Devices that support the cash box can open. Device without this module will return an error when calling the interface

```
private void openCashBox() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret = printerService.openCashBox();
                showLog("Open cash box: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

## NFC
NFC uses the Android general NFC module, the specific introduction can refer to [Android NFC](https://developer.android.google.cn/guide/topics/connectivity/nfc)

Card reading can refer to the following projects
- [MifareClassicTool](https://github.com/ikarus23/MifareClassicTool)
- [EMV-NFC-Paycard-Enrollment](https://github.com/devnied/EMV-NFC-Paycard-Enrollment)

## Q&A
1. If using AGP 8.0+ or throw exception like `Unresolved reference: IPrinterService`, `java.lang.NullPointerException: Attempt to invoke interface method 'int net.nyx.printerservice.print.IPrinterService.printText(java.lang.String, net.nyx.printerservice.print.PrintTextFormat)' on a null object reference`

Add config in `build.gradle` as below, [For AIDL Details](https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes)
```
buildFeatures {
    aidl = true
}
```

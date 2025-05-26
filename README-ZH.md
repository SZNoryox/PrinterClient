PrinterClient
==========
###
该Demo详细展示了Pos主要功能，包含：
- 打印：文本，图片，条形码/二维码，表格，标签，ESC/POS指令
- 客显屏
- 扫码：摄像头扫码，红外线扫码
- NFC
###

## 打印SDK集成
Demo使用AIDL进行集成。有关AIDL，请参考 [https://developer.android.com/guide/components/aidl](https://developer.android.com/guide/components/aidl)

### 主要文件说明
- [net.nyx.printerservice.print.IPrinterService.aidl](app/src/main/aidl/net/nyx/printerservice/print/IPrinterService.aidl) —— the aidl interface for all printer functions
- [net.nyx.printerservice.print.PrintTextFormat.aidl](app/src/main/aidl/net/nyx/printerservice/print/PrintTextFormat.aidl) —— the aidl bean class to set the print text style
- [net.nyx.printerservice.print.PrintTextFormat.java](app/src/main/java/net/nyx/printerservice/print/PrintTextFormat.java) —— the java bean class to set the print text style

### 集成
1. 在项⽬中添加上述三个⽂件且保持包路径和文件不变
2. Android12在`AndroidManifest.xml`添加标签以适配 `android 11 package visibility`
```xml
<queries>
    <package android:name="net.nyx.printerservice"/>
</queries>
```
3. 绑定AIDL service
```
private IPrinterService printerService;
private ServiceConnection connService = new ServiceConnection() {
    @Override
    public void onServiceDisconnected(ComponentName name) {
        showLog("printer service disconnected, try reconnect");
        printerService = null;
        // 尝试重新bind
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
    intent.setPackage("net.nyx.printerservice");
    intent.setAction("net.nyx.printerservice.IPrinterService");
    bindService(intent, connService, Context.BIND_AUTO_CREATE);
}

private void unbindService() {
    unbindService(connService);
}
```
4. 使用`printerService`调用 AIDL接口中定义的⽅法进行打印

## Printer

### 打印文本/图片/条码
[PrintTextFormat](app/src/main/java/net/nyx/printerservice/print/PrintTextFormat.java): java bean类，设置打印文本样式，包括文本大小、文本样式、文本对齐方式、文本字库等
```
try {
    PrintTextFormat textFormat = new PrintTextFormat();
    // textFormat.setTextSize(32);
    // textFormat.setUnderline(true);
    int ret = printerService.printText(text, textFormat);
    ret = printerService.printBarcode("123456789", 300, 160, 1, 1);
    ret = printerService.printQrCode("123456789", 300, 300, 1);
    if (ret == 0) {
        printerService.printEndAutoOut();
    }
} catch (RemoteException e) {
    e.printStackTrace();
}
```

自定义打印字库，**路径需要设置为公有路径**，字库放在`assets`目录或应用私有目录将不会生效
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

### 打印表格
按表格排列形式打印，方便进行内容排版
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

### 打印ESC/POS指令
透传ESC/POS指令，ESC指令集可以参考 [ESC/POS](https://download4.epson.biz/sec_pubs/pos/reference_en/escpos/commands.html)

**注意**：该接口与其他sdk同步接口不同，该接口是异步的，调用该接口只会将ESC/POS指令传输至指令队列，返回值表示指令成功入队，并不会返回打印机状态。请勿将该接口与其他接口混合使用，否则会出现打印内容顺序错乱情况。此处仅为使用ESC/POS指令客户提供除蓝牙外的额外选择。
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

### 打印标签

打印标签有两种方式
#### 1. 清楚标签的具体尺寸（像素）
打印标签内容需要被包含在 `printerService.labelLocate()` 和 `printerService.labelPrintEnd()` 中间
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
                    printerService.printBarcode("1234567890987654321", 320, 90, 2, 0);
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
#### 2. 标签自动检测，即标签学习
标签学习会自动走出一段距离标签纸，用于检测标签纸尺寸等相关参数，待接口返回成功后，即可将打印内容包含在`printerService.labelLocateAuto()` 和 `printerService.labelPrintEnd()`中间
- `printerService.hasLabelLearning()`：判断系统是否已经进行过相关学习
- `printerService.clearLabelLearning()`：可以清除系统对标签相关参数的存储
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
                        printerService.printBarcode("1234567890987654321", 320, 90, 2, 0);
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

### 打印结果
所有打印接口都返回int类型结果，参考 [SdkResult.java](app/src/main/java/net/nyx/printerclient/SdkResult.java) 对打印结果进行相关处理

## 客显屏
支持客显屏的设备可控制显示，无该模块的设备调用接口将会返回错误

### 客显控制
```
// @param flag 0--init 1--wakeup LCD 2--sleep LCD 3--clear LCD 4--reset LCD display
// int configLcd(int flag);

// 唤醒
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
// 休眠
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

// 还原默认显示
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

### 客显显示

**图片尺寸要与客显屏尺寸一致，小于客显屏的图片将居中显示**
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
                showLog("Show LCD bitmap: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

### 客显默认图片
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
                showLog("Show LCD default: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

## 扫码

### 摄像头扫描
只需要启动系统活动即可获得内置相机扫描仪。该方式无法自定义扫码界面。

```
private void scan() {
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("net.nyx.scanner",
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

### 红外线扫码

注册系统广播以获得红外扫码结果    
    
```
private final BroadcastReceiver qscReceiver = new BroadcastReceiver() {
                                          
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.NYX_QSC_DATA".equals(intent.getAction())) {
            String qsc = intent.getStringExtra("qsc");
            showLog("qsc scan result: %s", qsc);
            printText("qsc-quick-scan-code/n" + qsc);
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

默认情况下红外扫码由侧边实体按键触发，也提供软触发的接口
```
private void infraredScan() {
    singleThreadExecutor.submit(new Runnable() {
        @Override
        public void run() {
            try {
                int ret = printerService.triggerQscScan();
                showLog("Infrared scan: " + msg(ret));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    });
}
```

## 钱箱
仅支持钱箱的设备操作，无该模块的设备调用接口将会返回错误
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
NFC使用Android通用NFC模块，具体介绍可参考[Android NFC](https://developer.android.google.cn/guide/topics/connectivity/nfc)

读卡相关可以参考以下项目
- [MifareClassicTool](https://github.com/ikarus23/MifareClassicTool)
- [EMV-NFC-Paycard-Enrollment](https://github.com/devnied/EMV-NFC-Paycard-Enrollment)

## Q&A
1. 使用AGP8.0+ 或者报错 `Unresolved reference: IPrinterService`, `java.lang.NullPointerException: Attempt to invoke interface method 'int net.nyx.printerservice.print.IPrinterService.printText(java.lang.String, net.nyx.printerservice.print.PrintTextFormat)' on a null object reference`

`build.gradle`中增加如下配置, [AIDL详情](https://developer.android.com/build/releases/past-releases/agp-8-0-0-release-notes)
```
buildFeatures {
    aidl = true
}
```

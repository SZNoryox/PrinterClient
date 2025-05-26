package net.nyx.printerclient;

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;
import timber.log.Timber;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.nyx.printerclient.Result.msg;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    protected Button btnVer;
    protected Button btnPaper;
    protected Button btnPrint;
    protected Button btn1;
    protected Button btn2;
    protected Button btn3;
    protected Button btn4;
    protected Button btn5;
    protected Button btn6;
    protected Button btnLbl;
    protected Button btnLblLearning;
    protected Button btnGetDensity;
    protected Button btnSetDensity;
    protected Button btnLcdBmp;
    protected Button btnLcdReset;
    protected Button btnLcdWakeup;
    protected Button btnLcdSleep;
    protected Button btnCashBox;
    protected Button btnInfraredScan;
    protected Button btnCameraScan;
    protected TextView tvLog;

    private static final int RC_SCAN = 0x99;
    public static String PRN_TEXT;

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    String[] version = new String[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
        bindService();
        registerQscScanReceiver();
        Timber.plant(new Timber.DebugTree());
        PRN_TEXT = getString(R.string.print_text);
    }


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
            getVersion();
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

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_ver) {
            getVersion();
        } else if (view.getId() == R.id.btn_paper) {
            paperOut();
        } else if (view.getId() == R.id.btn_get_density) {
            getDensity();
        } else if (view.getId() == R.id.btn_set_density) {
            setDensity();
        } else if (view.getId() == R.id.btn_print) {
            printTest();
        } else if (view.getId() == R.id.btn1) {
            printText();
        } else if (view.getId() == R.id.btn2) {
            printBarcode();
        } else if (view.getId() == R.id.btn3) {
            printQrCode();
        } else if (view.getId() == R.id.btn4) {
            printBitmap();
        } else if (view.getId() == R.id.btn5) {
            printTable();
        } else if (view.getId() == R.id.btn6) {
            printEscpos();
        } else if (view.getId() == R.id.btn_infrared_scan) {
            infraredScan();
        } else if (view.getId() == R.id.btn_camera_scan) {
            cameraScan();
        } else if (view.getId() == R.id.btn_lbl) {
            printLabel();
        } else if (view.getId() == R.id.btn_lbl_learning) {
            printLabelLearning();
        } else if (view.getId() == R.id.btn_lcd_bmp) {
            showLcdBitmap();
        } else if (view.getId() == R.id.btn_lcd_reset) {
            configLcd(4);
        } else if (view.getId() == R.id.btn_lcd_wakeup) {
            configLcd(1);
        } else if (view.getId() == R.id.btn_lcd_sleep) {
            configLcd(2);
        } else if (view.getId() == R.id.btn_cash_box) {
            openCashBox();
        }
    }

    private void getVersion() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.getPrinterVersion(version);
                    showLog("Version: " + msg(ret) + "  " + version[0]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void paperOut() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    printerService.paperOut(80);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getDensity() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int[] density = new int[1];
                    int ret = printerService.getPrinterDensity(density);
                    showLog("Printer density: " + msg(ret) + "  " + density[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setDensity() {
        int[] densityList = {100, 110, 120, 130};
        String[] densityTextList = {"100%", "110%", "120%", "130%"};
        new AlertDialog
                .Builder(this, android.R.style.Theme_Material_Light_Dialog)
                .setTitle(R.string.main_set_density)
                .setItems(densityTextList, (dialog, which) -> {
                    setDensity(densityList[which]);
                })
                .show();

    }

    private void setDensity(int density) {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.setPrinterDensity(density);
                    showLog("Set printer density: " + msg(ret));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printTest() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PrintTextFormat textFormat = new PrintTextFormat();
                    int ret = printerService.printText(PRN_TEXT, textFormat);
                    ret = printerService.printBarcode("123456789", 300, 160, 1, 1);
                    ret = printerService.printQrCode("123456789", 300, 300, 1);
                    ret = printerService.printBitmap(BitmapFactory.decodeStream(getAssets().open("bmp.png")), 1, 1);
                    showLog("Print test: " + msg(ret));
                    if (ret == 0) {
                        printerService.printEndAutoOut();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printText() {
        printText(PRN_TEXT);
    }

    private void printText(String text) {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PrintTextFormat textFormat = new PrintTextFormat();
                    // textFormat.setTextSize(32);
                    // textFormat.setUnderline(true);
                    int ret = printerService.printText(text, textFormat);
                    showLog("Print text: " + msg(ret));
                    if (ret == 0) {
                        printerService.printEndAutoOut();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printBarcode() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printBarcode("123456789", 300, 160, 1, 1);
                    showLog("Print text: " + msg(ret));
                    if (ret == 0) {
                        printerService.printEndAutoOut();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printQrCode() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printQrCode("123456789", 300, 300, 1);
                    showLog("Print barcode: " + msg(ret));
                    if (ret == 0) {
                        printerService.printEndAutoOut();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printBitmap() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printBitmap(BitmapFactory.decodeStream(getAssets().open("bmp.png")), 1, 1);
                    showLog("Print bitmap: " + msg(ret));
                    if (ret == 0) {
                        printerService.printEndAutoOut();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

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
                    printerService.printEscposData("--------------------------------\n".getBytes());
                    printerService.printEscposData(Utils.printTwoColumn("phone", "4999.00"));
                    printerService.printEscposData(Utils.printTwoColumn("laptop", "4999.00"));
                    printerService.printEscposData("--------------------------------\n".getBytes());
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

    private void printLabel() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.labelLocate(240, 16);
                    if (ret == 0) {
                        PrintTextFormat format = new PrintTextFormat();
                        printerService.printText("\nModel:\t\tNB55", format);
                        printerService.printBarcode("1234567890987654321", 320, 90, 2, 0);
                        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                        printerService.printText("Time:\t\t" + date, format);
                        ret = printerService.labelPrintEnd();
                    }
                    showLog("Print label: " + msg(ret));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printLabelLearning() {
        if (version[0] != null && Float.parseFloat(version[0]) < 1.10) {
            showLog(getString(R.string.res_not_support));
            return;
        }
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
                            printerService.printText("\nModel:\t\tNB55", format);
                            printerService.printBarcode("1234567890987654321", 320, 90, 2, 0);
                            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                            printerService.printText("Time:\t\t" + date, format);
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

    private void showLcdBitmap() {
        showDialog();
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // init
                    int ret = printerService.configLcd(0);
                    if (ret == 0) {
                        Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open("bmp.png"));
                        ret = printerService.showLcdBitmap(bitmap);
                    }
                    showLog("Show LCD bitmap: " + msg(ret));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hideDialog();
            }
        });
    }

    private void configLcd(int opt) {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // init
                    int ret = printerService.configLcd(0);
                    if (ret == 0) {
                        ret = printerService.configLcd(opt);
                    }
                    showLog("LCD config: " + msg(ret));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

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

    private void cameraScan() {
        // need permission "android.permission.QUERY_ALL_PACKAGES" for android 11 package visible
        if (!existApp("net.nyx.scanner")) {
            showLog("Scanner app is not installed");
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("net.nyx.scanner",
                "net.nyx.scanner.ScannerActivity"));
        // set the capture activity actionbar title
        intent.putExtra("TITLE", "Scan");
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

    boolean existApp(String pkg) {
        try {
            return getPackageManager().getPackageInfo(pkg, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
        unregisterQscReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_clear) {
            clearLog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        btnVer = (Button) findViewById(R.id.btn_ver);
        btnVer.setOnClickListener(MainActivity.this);
        btnPaper = (Button) findViewById(R.id.btn_paper);
        btnPaper.setOnClickListener(MainActivity.this);
        btnPrint = (Button) findViewById(R.id.btn_print);
        btnPrint.setOnClickListener(MainActivity.this);
        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(MainActivity.this);
        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(MainActivity.this);
        btn3 = (Button) findViewById(R.id.btn3);
        btn3.setOnClickListener(MainActivity.this);
        btn4 = (Button) findViewById(R.id.btn4);
        btn4.setOnClickListener(MainActivity.this);
        btn5 = (Button) findViewById(R.id.btn5);
        btn5.setOnClickListener(MainActivity.this);
        btn6 = (Button) findViewById(R.id.btn6);
        btn6.setOnClickListener(MainActivity.this);
        btnInfraredScan = (Button) findViewById(R.id.btn_infrared_scan);
        btnInfraredScan.setOnClickListener(MainActivity.this);
        btnCameraScan = (Button) findViewById(R.id.btn_camera_scan);
        btnCameraScan.setOnClickListener(MainActivity.this);
        tvLog = (TextView) findViewById(R.id.tv_log);
        btnLbl = (Button) findViewById(R.id.btn_lbl);
        btnLbl.setOnClickListener(MainActivity.this);
        btnLblLearning = (Button) findViewById(R.id.btn_lbl_learning);
        btnLblLearning.setOnClickListener(MainActivity.this);
        btnGetDensity = (Button) findViewById(R.id.btn_get_density);
        btnGetDensity.setOnClickListener(MainActivity.this);
        btnSetDensity = (Button) findViewById(R.id.btn_set_density);
        btnSetDensity.setOnClickListener(MainActivity.this);
        btnLcdBmp = (Button) findViewById(R.id.btn_lcd_bmp);
        btnLcdBmp.setOnClickListener(MainActivity.this);
        btnLcdReset = (Button) findViewById(R.id.btn_lcd_reset);
        btnLcdReset.setOnClickListener(MainActivity.this);
        btnLcdWakeup = (Button) findViewById(R.id.btn_lcd_wakeup);
        btnLcdWakeup.setOnClickListener(MainActivity.this);
        btnLcdSleep = (Button) findViewById(R.id.btn_lcd_sleep);
        btnLcdSleep.setOnClickListener(MainActivity.this);
        btnCashBox = (Button) findViewById(R.id.btn_cash_box);
        btnCashBox.setOnClickListener(MainActivity.this);
    }

    void showLog(String log, Object... args) {
        if (args != null && args.length > 0) {
            log = String.format(log, args);
        }
        String res = log;
        Log.e(TAG, res);
        DateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvLog.getLineCount() > 100) {
                    tvLog.setText("");
                }
                tvLog.append((dateFormat.format(new Date()) + ":" + res + "\n"));
                tvLog.post(new Runnable() {
                    @Override
                    public void run() {
                        ((ScrollView) tvLog.getParent()).fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    void clearLog() {
        tvLog.setText("");
    }

}

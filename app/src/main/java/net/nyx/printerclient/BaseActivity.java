package net.nyx.printerclient;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected Handler handler = new Handler(Looper.getMainLooper());
    private ProgressDialog mDialog;

    public void showDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        handler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (mDialog == null) {
                mDialog = new ProgressDialog(this);
                mDialog.setMessage("loading...");
                mDialog.setIndeterminate(false);
                mDialog.setCancelable(false);
            }
            if (!mDialog.isShowing()) {
                mDialog.show();
            }
        });
    }

    public void hideDialog() {
        handler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (mDialog == null || !mDialog.isShowing()) {
                return;
            }
            mDialog.dismiss();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            hideDialog();
        }
        mDialog = null;
    }
}

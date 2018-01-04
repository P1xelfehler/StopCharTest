package com.laudien.p1xelfehler.stopchartest;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private static final long WAIT_TIME = 10000;
    ToggleChargingFile[] files = new ToggleChargingFile[]{
            new ToggleChargingFile("/sys/class/power_supply/battery/battery_charging_enabled", "1", "0"),
            new ToggleChargingFile("/sys/class/power_supply/battery/charging_enabled", "1", "0"),
            new ToggleChargingFile("/sys/class/power_supply/battery/batt_slate_mode", "0", "1"),
            new ToggleChargingFile("/sys/class/hw_power/charger/charge_data/enable_charger", "1", "0"),
            new ToggleChargingFile("/sys/module/pm8921_charger/parameters/disabled", "0", "1"),
            new ToggleChargingFile("/sys/devices/qpnp-charger-f2d04c00/power_supply/battery/charging_enabled", "1", "0"),
            new ToggleChargingFile("/sys/devices/qpnp-charger-14/power_supply/battery/charging_enabled", "1", "0"),
            new ToggleChargingFile("/sys/class/power_supply/battery/input_suspend", "0", "1")
    };
    private boolean isCharging;
    private TextView tv_log;
    private Button btn_copy;
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_log = findViewById(R.id.tv_log);
        Button btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CheckFilesTask().execute();
            }
        });
        btn_copy = findViewById(R.id.btn_copy);
        btn_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence copyText = tv_log.getText();
                if (!copyText.equals("")) {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText(getTitle(), tv_log.getText());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(MainActivity.this, getString(R.string.toast_copy_success), LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.toast_nothing_to_copy), Toast.LENGTH_SHORT).show();
                }
            }
        });
        Intent batteryStatus = registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));
        isCharging = batteryStatus != null && batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
        if (!isCharging) {
            final AlertDialog dialog = getDialog(getString(R.string.dialog_connect_charger));
            dialog.show();
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    dialog.dismiss();
                    unregisterReceiver(this);
                }
            }, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        }
        new CheckForRootTask().execute();
    }

    private AlertDialog getDialog(String message) {
        return new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getTitle())
                .setMessage(message)
                .setIcon(R.mipmap.ic_launcher)
                .create();
    }

    private void log(String message) {
        tv_log.append(message.concat("\n"));
    }

    private void clearLog() {
        tv_log.setText("");
    }

    private class CheckFilesTask extends AsyncTask<Void, String, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            btn_copy.setEnabled(false);
            clearLog();
            log("Brand: " + Build.BRAND);
            log("Manufacturer: " + Build.MANUFACTURER);
            log("Model: " + Build.MODEL);
            log("Product: " + Build.PRODUCT);
            log("---------------------------------------------------------------------------");
            registerReceiver(batteryChangedReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (ToggleChargingFile file : files) {
                publishProgress(String.format("Testing file '%s'...", file.getPath()));
                if (new File(file.getPath()).exists()) {
                    Shell.SU.run(String.format("echo %s > %s", file.getChargeOff(), file.getPath()));
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!isCharging) {
                        publishProgress(String.format("That file disabled charging: '%s'!", file.getPath()));
                        publishProgress("Resetting file...");
                        Shell.SU.run(String.format("echo %s > %s", file.getChargeOn(), file.getPath()));
                        break;
                    } else {
                        publishProgress("That file did not work!");
                        publishProgress("Resetting file...");
                        Shell.SU.run(String.format("echo %s > %s", file.getChargeOn(), file.getPath()));
                    }
                } else {
                    publishProgress("That file does not exist!");
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... logMessages) {
            super.onProgressUpdate(logMessages);
            for (String message : logMessages) {
                log(message);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            btn_copy.setEnabled(true);
            log("Process finished!");
            unregisterReceiver(batteryChangedReceiver);
        }
    }

    private class CheckForRootTask extends AsyncTask<Void, Void, Boolean> {
        private AlertDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = getDialog(getString(R.string.dialog_check_root));
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return Shell.SU.available();
        }

        @Override
        protected void onPostExecute(Boolean rootAvailable) {
            super.onPostExecute(rootAvailable);
            dialog.dismiss();
            if (!rootAvailable) {
                dialog = getDialog(getString(R.string.dialog_not_rooted));
                dialog.show();
            }
        }
    }
}

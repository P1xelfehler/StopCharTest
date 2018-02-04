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
import static android.content.Intent.ACTION_POWER_CONNECTED;
import static android.content.Intent.ACTION_POWER_DISCONNECTED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
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
            new ToggleChargingFile("/sys/class/power_supply/battery/input_suspend", "0", "1"),
            new ToggleChargingFile("/sys/class/power_supply/ac/charging_enabled", "1", "0"),
            new ToggleChargingFile("/sys/class/power_supply/battery/charge_enabled", "1", "0"),
            new ToggleChargingFile("/sys/class/power_supply/battery/device/Charging_Enable", "1", "0"),
            new ToggleChargingFile("/sys/devices/platform/7000c400.i2c/i2c-1/1-006b/charging_state", "enabled", "disabled"),
            new ToggleChargingFile("/sys/class/power_supply/battery/charger_control", "1", "0"), // experimental
            new ToggleChargingFile("/sys/class/power_supply/bq2589x_charger/enable_charging", "1", "0"), // experimental
            new ToggleChargingFile("/sys/class/power_supply/chargalg/disable_charging", "0", "1"), // experimental
            new ToggleChargingFile("/sys/class/power_supply/dollar_cove_charger/present", "1", "0"), // experimental
            new ToggleChargingFile("/sys/class/power_supply/battery/charge_disable", "0", "1"), // experimental
            new ToggleChargingFile("/sys/devices/platform/battery/ChargerEnable", "1", "0"), // experimental
            new ToggleChargingFile("/sys/devices/platform/huawei_charger/enable_charger", "1", "0"), // experimental
            new ToggleChargingFile("/sys/devices/platform/mt-battery/disable_charger", "0", "1"), // experimental
            new ToggleChargingFile("/sys/devices/platform/tegra12-i2c.0/i2c-0/0-006b/charging_state", "enabled", "disabled"), // experimental
            new ToggleChargingFile("/sys/devices/virtual/power_supply/manta-battery/charge_enabled", "1", "0") // experimental
    };
    private boolean changeReceived;
    private TextView tv_log;
    private Button btn_copy;
    private BroadcastReceiver batteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (changeReceived || intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(ACTION_POWER_CONNECTED) || intent.getAction().equals(ACTION_POWER_DISCONNECTED)) {
                Toast.makeText(MainActivity.this, intent.getAction(), Toast.LENGTH_SHORT).show();
                changeReceived = true;
                return;
            }
            if (intent.getAction().equals(ACTION_BATTERY_CHANGED)) {
                int chargingType = intent.getIntExtra(EXTRA_PLUGGED, 0);
                boolean isCharging = chargingType != 0;
                if (!isCharging) {
                    Toast.makeText(MainActivity.this, intent.getAction(), Toast.LENGTH_SHORT).show();
                    changeReceived = true;
                }
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
        boolean isCharging = batteryStatus != null && batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0;
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
            IntentFilter intentFilter = new IntentFilter(ACTION_POWER_CONNECTED);
            intentFilter.addAction(ACTION_POWER_DISCONNECTED);
            intentFilter.addAction(ACTION_BATTERY_CHANGED);
            registerReceiver(batteryChangedReceiver, intentFilter);
        }

        @Override
        protected Void doInBackground(Void... params) {
            changeReceived = false;
            for (ToggleChargingFile file : files) {
                publishProgress(String.format("Testing file '%s'...", file.getPath()));
                if (new File(file.getPath()).exists()) {
                    Shell.SU.run(String.format("echo %s > %s", file.getChargeOff(), file.getPath()));
                    sleep();
                    BatteryManager batteryManager = SDK_INT >= LOLLIPOP ? (BatteryManager) getSystemService(BATTERY_SERVICE) : null;
                    int current = SDK_INT >= LOLLIPOP && batteryManager != null ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) : 0;
                    publishProgress("Resetting file...");
                    Shell.SU.run(String.format("echo %s > %s", file.getChargeOn(), file.getPath()));
                    if (changeReceived || current < -50000) {
                        publishProgress(String.format("-----\nThat file might have disabled charging: '%s'!\n-----", file.getPath()));
                        changeReceived = false;
                    } else {
                        publishProgress("That file did not work!");
                    }
                } else {
                    publishProgress("That file does not exist!");
                }
            }
            return null;
        }

        private void sleep() {
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

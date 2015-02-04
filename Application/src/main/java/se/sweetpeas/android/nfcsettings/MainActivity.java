/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.sweetpeas.android.nfcsettings;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * This sample shows you how to use ActionBarCompat to create a basic Activity which displays
 * action items. It covers inflating items from a menu resource, as well as adding an item in code.
 *
 * This Activity extends from {@link ActionBarActivity}, which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 */
public class MainActivity extends ActionBarActivity {

    private static final int OP_READ = 1;
    private static final int OP_WRITE = 2;

    public static final String TAG = "NfcDemo.MainActivity";

    private int Operation = OP_READ;

    private NfcAdapter nfca;

    private FrameLayout curLayout = null;

    byte[] payload;

    EspUserData eud= new EspUserData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        initScreens();

        Log.d(TAG, "Got to the main activity onCreate !");
        nfca = NfcAdapter.getDefaultAdapter(this);

        if (nfca == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfca.isEnabled()) {
            Toast.makeText(this, "Please note that NFC is disabled.", Toast.LENGTH_LONG).show();
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, nfca);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, nfca);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity
         * instance. Instead of creating a new activity, onNewIntent will be called. For more
         * information have a look at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        Log.d(TAG, "Got to onNewIntent !");
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        switch (Operation) {
            case OP_READ:
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                    Toast.makeText(this, "NDEF Tag Detected, Operation = " + Operation + " !",
                            Toast.LENGTH_LONG).show();

                    String type = intent.getType();
                    Log.d(TAG, "Got mime type: " + type);
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    new NdefReaderTask().execute(tag);
                    setScreen(1);
                } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

                    // In case we would still use the Tech Discovered Intent
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    String[] techList = tag.getTechList();
                    String searchedTech = Ndef.class.getName();

                    for (String tech : techList) {
                        if (searchedTech.equals(tech)) {
                            new NdefReaderTask().execute(tag);
                            break;
                        }
                    }
                }
                break;

            case OP_WRITE:
                // Always fall back to read mode, even if we fail
                Operation = OP_READ;
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                    NdefRecord nr = NdefRecord.createExternal(
                            new String("se.sweetpeas.android.nfcsettings"),
                            new String("externaltype"), payload);
                    NdefMessage msg = new NdefMessage(new NdefRecord[] {nr});

                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    Ndef ndef = Ndef.get(tag);

                    try {
                        ndef.connect();
                    } catch (Exception e) {
                        Log.e(TAG, "Could not connect to tag !");
                        Toast.makeText(this, "Failed to connect to tag !", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "Attempting to write data to tag !");
                    try {
                        ndef.writeNdefMessage(msg);
                        Log.i(TAG, "Succeeded to write to tag !");
                        Toast.makeText(this, "Succeeded to write new data to tag !", Toast.LENGTH_LONG).show();
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error while trying to write an NDEF message: " +
                                e.toString());
                        Toast.makeText(this, "Error while trying to write to tag !", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "Not an NDEF tag");
                }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent =
                PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataScheme("vnd.android.nfc");
            filters[0].addDataPath("/se.sweetpeas.android.nfcsettings:externaltype",
                    PatternMatcher.PATTERN_LITERAL);
        } catch (Exception e) {
            Log.e(TAG, "Error occurred: " + e.toString());
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    // BEGIN_INCLUDE(create_menu)
    /**
     * Use this method to instantiate your menu, and add your items to it. You
     * should return true if you have added items to it and want the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu from the resources by using the menu inflater.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }
    // END_INCLUDE(create_menu)

    private byte[] ipToArray(String ipstring) throws ArithmeticException {
        byte[] ip = new byte[4];
        String[] parts = ipstring.split("\\.");

        if (parts.length != 4) {
            throw new ArithmeticException("Not exactly 4 parameters in ip address");
        }

        for (int i = 0; i < 4; i++) {
            ip[i] = (byte)Integer.parseInt(parts[i]);
        }

        return ip;
    }

    /**
     * Build the payload that should be written to the NFC tag memory
     */
    private void buildPayload() {

        EditText et;
        CheckBox cb;

        byte[] device;
        byte[] dhcpEnabled = new byte[1];
        byte[] ip;
        byte[] netmask;
        byte[] gateway;
        int imPort;     // Intermediate variable
        byte[] port = new byte[2];

        // Read out values from gui, starting with the system name
        et = (EditText)findViewById(R.id.espSystemName);
        device = eud.getDevice(et.getText().toString());

        // DHCP enabled
        cb = (CheckBox)findViewById(R.id.espEnableDhcp);
        if (cb.isChecked() == true) {
            dhcpEnabled[0] = 1;
        } else {
            dhcpEnabled[0] = 0;
        }

        // IP Address
        et = (EditText)findViewById(R.id.espIpAddress);
        try {
            ip = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in IP address, setting it to 0.0.0.0" + e);
            ip = new byte[4];
            ip[0] = ip[1] = ip[2] = ip[3] = 0;
        }

        // Netmask
        et = (EditText)findViewById(R.id.espNetmask);
        try {
            netmask = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in Netmask address, setting it to 0.0.0.0" + e);
            netmask = new byte[4];
            netmask[0] = netmask[1] = netmask[2] = netmask[3] = 0;
        }

        // Default gateway
        et = (EditText)findViewById(R.id.espGateway);
        try {
            gateway = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in Default gateway address, setting it to 0.0.0.0" + e);
            gateway = new byte[4];
            gateway[0] = gateway[1] = gateway[2] = gateway[3] = 0;
        }

        // Webserver port
        et = (EditText)findViewById(R.id.espWebServerport);
        imPort = Integer.parseInt(et.getText().toString());
        port[0] = (byte)(imPort & 255);
        port[1] = (byte)((imPort / 256) & 255);

        // Create a new payload stream
        eud.streamCreate();
        // Write objects to the stream
        eud.streamData(device);
        eud.streamData(dhcpEnabled);
        eud.streamData(ip);
        eud.streamData(netmask);
        eud.streamData(gateway);
        eud.streamData(port);
    }

    // BEGIN_INCLUDE(menu_item_selected)
    /**
     * This method is called when one of the menu items to selected. These items
     * can be on the Action Bar, the overflow menu, or the standard options menu. You
     * should return true if you handle the selection.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag == null) {
                    Log.i(TAG, "Could not find a connected tag !");
                    return true;
                }

                Ndef ndef = Ndef.get(tag);
                if (ndef.isConnected() == true) {
                    // Here we might start a background refresh task
                    new NdefReaderTask().execute(tag);
                }
                return true;

            case R.id.menu_update:
                Log.d(TAG, "Selected menu_update");
                // Set up a new pending intent to take care of writing a tag
                Operation = OP_WRITE;
                setupForegroundDispatch(this, nfca);
                Toast.makeText(this, "Move the tag into range to write the data !", Toast.LENGTH_LONG).show();

                // Create the payload for our ndef message
                buildPayload();
                // Get the payload from the created stream.
                payload = eud.getStreamArray();

                Log.d(TAG, "Payload string length = " + payload.length);
                for (int i=0;i<payload.length;i++) {
                    Log.d(TAG, "payload[" + i + "] = " + getUnsignedByte(payload[i]));
                }
                return true;

            case R.id.menu_settings:
                /* Here we would open up our settings activity */
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initScreens() {
        FrameLayout fl = null;

        fl = (FrameLayout)findViewById(R.id.startLayout);
        fl.setVisibility(View.VISIBLE);
        curLayout = fl;

        fl = (FrameLayout)findViewById(R.id.dataLayout);
        fl.setVisibility(View.GONE);
    }
    /**
     * Used to change between resident screens.
     *
     * @param screen - The screen that shall be displayed
     */
    private void setScreen(int screen) {

        FrameLayout fl = null;

        if (curLayout != null) {
            curLayout.setVisibility(View.GONE);
        }

        switch (screen)
        {
            case 0:
                fl = (FrameLayout)findViewById(R.id.startLayout);
                fl.setVisibility(View.VISIBLE);
                break;

            case 1:
                fl = (FrameLayout)findViewById(R.id.dataLayout);
                fl.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }
        curLayout = fl;
    }

    private int getUnsignedByte(byte data) {
        if (data < 0) {
            return 256 - Math.abs(data);
        }
        return data;
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, byte[]> {

        @Override
        protected byte[] doInBackground(Tag... params) {
            Tag tag = params[0];
            Ndef ndef;

            try {
                ndef = Ndef.get(tag);
            } catch (NullPointerException e) {
                Log.w(TAG, "Ndef.get returned null !");
                return null;
            }
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            Log.d(TAG, "I see " + records.length + " ndef records !");
            for (NdefRecord ndefRecord : records) {
                Log.d(TAG, "Record Inf: " + ndefRecord.getTnf());
                Log.d(TAG, "Record Type: " + ndefRecord.getType().toString());
                if (ndefRecord.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE) {
                    payload = ndefRecord.getPayload();
                    // Build IP address
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EditText et;

                            // system name
                            et = (EditText)findViewById(R.id.espSystemName);
                            et.setText(eud.getDeviceString(payload[0]));

                            // DHCP Switch
                            CheckBox cb = (CheckBox)findViewById(R.id.espEnableDhcp);
                            if (payload[1] != 0) {
                                cb.setChecked(true);
                            } else {
                                cb.setChecked(false);
                            }

                            // Ip Address
                            String ip = new String();
                            ip += Integer.toString(getUnsignedByte(payload[2]));
                            ip += ".";
                            ip += Integer.toString(getUnsignedByte(payload[3]));
                            ip += ".";
                            ip += Integer.toString(getUnsignedByte(payload[4]));
                            ip += ".";
                            ip += Integer.toString(getUnsignedByte(payload[5]));
                            et = (EditText)findViewById(R.id.espIpAddress);
                            et.setText(ip);

                            // Netmask Address
                            String nm = new String();
                            nm += Integer.toString(getUnsignedByte(payload[6]));
                            nm += ".";
                            nm += Integer.toString(getUnsignedByte(payload[7]));
                            nm += ".";
                            nm += Integer.toString(getUnsignedByte(payload[8]));
                            nm += ".";
                            nm += Integer.toString(getUnsignedByte(payload[9]));
                            et = (EditText)findViewById(R.id.espNetmask);
                            et.setText(nm);

                            // Default gateway
                            String gw = new String();
                            gw += Integer.toString(getUnsignedByte(payload[10]));
                            gw += ".";
                            gw += Integer.toString(getUnsignedByte(payload[11]));
                            gw += ".";
                            gw += Integer.toString(getUnsignedByte(payload[12]));
                            gw += ".";
                            gw += Integer.toString(getUnsignedByte(payload[13]));
                            et = (EditText)findViewById(R.id.espGateway);
                            et.setText(gw);

                            //  Webserver port
                            int port = getUnsignedByte(payload[14]) |
                                    getUnsignedByte(payload[15]) * 256;
                            et = (EditText)findViewById(R.id.espWebServerport);
                            et.setText(Integer.toString(port));
                        }
                    });
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(byte[] payload) {
            Log.d(TAG, "Received tag !");
        }
    }
}

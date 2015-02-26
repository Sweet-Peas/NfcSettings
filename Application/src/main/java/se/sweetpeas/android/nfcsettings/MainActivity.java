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
import android.widget.FrameLayout;
import android.widget.Toast;

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

    private boolean actionMenuEnabled = false;

    private NfcAdapter nfca;

    private FrameLayout curLayout = null;

    byte[] payload;

    EspUserData eud = new EspUserData();

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
        Log.d(TAG, "Got onNewIntent !");
        handleIntent(intent);
    }

    /*
     * This method handles the intent passed to it.
     * Supports read and write operations.
     */
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        switch (Operation) {
            case OP_READ:
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                    Toast.makeText(this, "NDEF Tag Detected !",
                            Toast.LENGTH_LONG).show();
                    String type = intent.getType();
                    Log.d(TAG, "Got mime type: " + type);
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    ReaderData rd = new ReaderData(this, tag);
                    new NdefReaderTask().execute(rd);
                    setScreen(1);
                } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

                    // In case we would still use the Tech Discovered Intent
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    String[] techList = tag.getTechList();
                    String searchedTech = Ndef.class.getName();

                    for (String tech : techList) {
                        if (searchedTech.equals(tech)) {
                            ReaderData rd = new ReaderData(this, tag);
                            new NdefReaderTask().execute(rd);
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
                        Toast.makeText(this, "Failed to connect to tag !",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "Attempting to write data to tag !");
                    try {
                        ndef.writeNdefMessage(msg);
                        Log.i(TAG, "Succeeded to write to tag !");
                        Toast.makeText(this, "Succeeded to write new data to tag !",
                                Toast.LENGTH_LONG).show();
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error while trying to write an NDEF message: " +
                                e.toString());
                        Toast.makeText(this, "Error while trying to write to tag !",
                                Toast.LENGTH_LONG).show();
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

    // BEGIN_INCLUDE(menu_item_selected)
    /**
     * This method is called when one of the menu items to selected. These items
     * can be on the Action Bar, the overflow menu, or the standard options menu. You
     * should return true if you handle the selection.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (actionMenuEnabled == true) {
            switch (item.getItemId()) {
                case R.id.menu_refresh:
                    Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    if (tag == null) {
                        Log.i(TAG, "Could not find a connected tag !");
                        return true;
                    }

                    Ndef ndef = Ndef.get(tag);
                    if (ndef.isConnected() == true) {
                        // Here we start a background refresh task
                        ReaderData rd = new ReaderData(this, tag);
                        new NdefReaderTask().execute(rd);
                    }
                    return true;

                case R.id.menu_update:
                    Log.d(TAG, "Selected menu_update");
                    // Set up a new pending intent to take care of writing a tag
                    Operation = OP_WRITE;
                    setupForegroundDispatch(this, nfca);
                    Toast.makeText(this, "Move the tag into range to write the data !",
                            Toast.LENGTH_LONG).show();

                    // Create the payload for our ndef message
                    eud.buildPayload(this);
                    // Get the payload from the created stream.
                    payload = eud.getStreamArray();

                    return true;

                case R.id.menu_settings:
                    /* Here we would open up our settings activity */
                    return true;
            }
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
                actionMenuEnabled = false;
                break;

            case 1:
                fl = (FrameLayout)findViewById(R.id.dataLayout);
                fl.setVisibility(View.VISIBLE);
                actionMenuEnabled = true;
                break;

            default:
                break;
        }
        curLayout = fl;
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     */
    private class NdefReaderTask extends AsyncTask<ReaderData, Void, byte[]> {

        @Override
        protected byte[] doInBackground(ReaderData... params) {
            final Tag tag = params[0].getTag();
            final Activity activity = params[0].getActivity();
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
                            eud.setPayload(activity, payload);
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

    /*
     * Support class for the AsyncTask
     */
    private class ReaderData {
        Tag tag;
        Activity activity;

        public ReaderData(Activity activity) {
            this.activity = activity;
        }

        public ReaderData(Activity activity, Tag tag) {
            this.activity = activity;
            this.tag = tag;
        }

        public void setTag(Tag tag) {
            this.tag = tag;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        public Tag getTag() {
            return this.tag;
        }

        public Activity getActivity() {
            return activity;
        }
    }
}

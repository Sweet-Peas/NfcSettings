package com.example.android.actionbarcompat.basic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by 23061151 on 1/12/15.
 *
 * Handles detection of tags that are not initialized
 */
public class HandleTag extends Activity {

    public static final int DIALOG_ICON_WARNING = 1;
    public static final int DIALOG_ICON_INFO = 2;

    public static final String TAG = "NfcDemo.HandleTag";
    private NfcAdapter nfca;
    private boolean doInitialize = false;

    EspUserData eud = new EspUserData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty_tag);

        Log.d(TAG, "Entered onCreate");

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
        onNewIntent(this.getIntent());
    }

    private void initTag() {
        Toast.makeText(this, "Move you phone close to the NFC antenna to initialize the tag.",
                Toast.LENGTH_LONG).show();
        doInitialize = true;
        ImageView image = (ImageView)findViewById(R.id.nfcImage);
        image.setVisibility(View.VISIBLE);
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "Entered onPause()");
        if (nfca != null) {
            try {
                nfca.disableForegroundDispatch(this);
            } catch (NullPointerException e) {
                Log.w(TAG, "Safely caught a null pointer exception, don't worry about it !");
            }
        }
    }

    public void onResume() {
        super.onResume();
        Log.d(TAG, "Entered onResume()");
        setupMyForegroundDispatch(this, nfca);
    }

    private void disableNfcImage() {
        ImageView image = (ImageView)findViewById(R.id.nfcImage);
        image.setVisibility(View.INVISIBLE);
        doInitialize = false;
    }

    private void OkButtonDialog(Context context, int type, String message) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        switch (type) {
            case 1:
                Log.d(TAG, "Setting a warning icon in the dialog !");
                builder1.setIcon(R.drawable.ic_action_warning);
                break;

            case 2:
                Log.d(TAG, "Setting a information icon in the dialog !");
                builder1.setIcon(R.drawable.ic_action_about);
                break;
        }
        builder1.setTitle(message);
        builder1.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "Entered onNewIntent");

        Log.d(TAG, "doInitialize = " + doInitialize);
        String action = getIntent().getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            Log.d(TAG, "Empty tag detected");
            if (doInitialize == true) {
                // Write a demo record to the tag
                byte[] payload= eud.getInitialData();
                NdefRecord nr = NdefRecord.createExternal(
                        new String("com.example.android.actionbarcompat.basic"),
                        new String("externaltype"), payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {nr});

                Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Ndef ndef = Ndef.get(tag);

                try {
                    ndef.connect();
                } catch (Exception e) {
                    Log.e(TAG, "Could not connect to tag !" + e.toString());
                    disableNfcImage();
                    OkButtonDialog(this, DIALOG_ICON_WARNING, "Failed to connect to tag, please try again !");
                    return;
                }

                Log.d(TAG, "Attempting to write data to tag !");
                try {
                    ndef.writeNdefMessage(msg);
                } catch (Exception e) {
                    Log.e(TAG, "Error while trying to write an NDEF message: " +
                            e.toString());
                    disableNfcImage();
                    OkButtonDialog(this, DIALOG_ICON_WARNING, "Failed to write to tag, please try again !");
                    return;
                }
                Log.d(TAG, "Successfully initialized the tag !");
                OkButtonDialog(this, DIALOG_ICON_INFO, "Tag was successfully initialized !");
                disableNfcImage();
            } else {
                // Show a dialog asking for what the user wants to do
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage("An empty tag was detected, would you like me to initialize it for use with the SweetPeas UnoNet ?");
                builder1.setCancelable(true);
                builder1.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Looks like the user wants to initialize so go do it.
                                initTag();
                                dialog.cancel();
                            }
                        });
                builder1.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User don't want to initialize, do nothing more
                                dialog.cancel();
                                finish();
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    private static void setupMyForegroundDispatch(final Activity activity, NfcAdapter adapter) {

        Log.d(TAG, "Entered setupMyForegroundDispatch");
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent =
                PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{ new String[] {"android.nfc.tech.Ndef"} };

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
}

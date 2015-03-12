package se.sweetpeas.android.nfcsettings.boards;

import android.os.Bundle;
import android.util.Log;

import java.lang.annotation.Target;

/**
 * Created by 23061151 on 3/10/15.
 */
public final class TargetBoardFragmentManager {

    private static final String TAG = "NfcDemo.TargetBoardManager";

    static TargetBoardInterface tbi = null;

    public static TargetBoardInterface newInstance(byte system) {

        switch (system) {
            case TargetDataInterface.SYSTEM_UNONET_PLUS:
                tbi = new UnoNetFragment();
                break;

            // Add more boards as they are developed
            default:
                Log.e(TAG, "Unsupported board detected !");
        }

        return tbi;
    }
}

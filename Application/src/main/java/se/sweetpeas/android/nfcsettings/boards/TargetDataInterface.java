package se.sweetpeas.android.nfcsettings.boards;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Created by 23061151 on 3/10/15.
 */
public interface TargetDataInterface {

    public static final int SYSTEM_UNONET_PLUS = 1;
    public static final int SYSTEM_LEOFI_PLUS = 2;
    public static final int SYSTEM_UNO_PLUS = 3;

    byte[] streamArray = new byte[2048];    // Max 2048 bytes in one stream
    String[] supportedDevices = {
            "Unknown",
            "UnoNet+",
            "LeoFi+",
            "Uno+",
    };

    public byte[] getDevice(String system);
    public String getDeviceString(byte device);
    public byte[] getInitialPayload();
    public void streamCreate();
    public void streamData(byte[] data);
    public byte[] getStreamArray();
    public void buildPayload(final Activity activity);
    public void onClickEvent(Activity activity, View view);
    public void setPayload(Activity activity, byte[] payload);
    public void screenInit(Activity activity);
}

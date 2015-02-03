package com.example.android.actionbarcompat.basic;

import java.util.Arrays;

/**
 * This class handles the target application user data
 */
public class EspUserData {

    private int wptr = 0;
    private byte[] streamArray = new byte[2048];    // Max 2048 bytes in one stream
    private String[] supportedDevices = {
            "Unknown",
            "UnoNet",
            "LeoFi+"
    };

    public byte[] getDevice(String system) {
        byte[] device = new byte[1];

        for(int i=0;i<supportedDevices.length;i++) {
            if (supportedDevices[i].equals(system)) {
                device[0] = (byte)i;
                return device;
            }
        }
        device[0] = 0;
        return device;
    }

    public String getDeviceString(byte device) {
        return new String(supportedDevices[device]);
    }

    public byte[] getInitialData() {
        return new byte[] {
                1,                                  // Sweetpeas product type number
                0,                                  // DHCP enabled or not
                (byte)192,(byte)168,0,10,           // Default IP address
                (byte)255,(byte)255,(byte)255,0,    // Default Netmask
                (byte)192,(byte)168,0,1,            // Default gateway
                80,0                                // Webserver port
        };
    }

    public void streamCreate() {
        wptr = 0;
    }

    public void streamData(byte[] data) {
        if (data.length == 0) {
            return;
        }

        // Copy data to stream buffer
        for (int i=0;i<data.length;i++) {
            streamArray[wptr++] = data[i];
        }
    }

    public int streamLength() {
        return wptr;
    }

    public byte[] getStreamArray() {
        byte[] array = new byte[wptr];
        array = Arrays.copyOfRange(streamArray, 0, wptr);
        return array;
    }
}

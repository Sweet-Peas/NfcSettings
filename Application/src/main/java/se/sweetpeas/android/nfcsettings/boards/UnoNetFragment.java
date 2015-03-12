package se.sweetpeas.android.nfcsettings.boards;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import java.util.Arrays;
import se.sweetpeas.android.nfcsettings.*;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link UnoNetFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link UnoNetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UnoNetFragment extends Fragment implements TargetBoardInterface {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private final String TAG = "NfcDemo.UnoNetFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UnoNetFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UnoNetFragment newInstance(String param1, String param2) {
        UnoNetFragment fragment = new UnoNetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public UnoNetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_uno_net, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    //===========================================================================================
    //==
    //== Implementation of the TargetDataInterface
    //==
    //===========================================================================================

    int wptr;

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

    public byte[] getInitialPayload() {
        return new byte[]{
                1,                                  // Sweetpeas product type number
                0,                                  // DHCP enabled or not
                (byte) 192, (byte) 168, 0, 10,           // Default IP address
                (byte) 255, (byte) 255, (byte) 255, 0,    // Default Netmask
                (byte) 192, (byte) 168, 0, 1,            // Default gateway
                80, 0                                // Webserver port
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
        for (int i = 0; i < data.length; i++) {
            streamArray[wptr++] = data[i];
        }
    }

    public byte[] getStreamArray() {
        byte[] array = new byte[wptr];
        array = Arrays.copyOfRange(streamArray, 0, wptr);
        return array;
    }

    /*
     * Support function to convert an ascii string to IP numbers
     */
    private byte[] ipToArray(String ipstring) throws ArithmeticException {
        byte[] ip = new byte[4];
        String[] parts = ipstring.split("\\.");

        if (parts.length != 4) {
            throw new ArithmeticException("Not exactly 4 parameters in ip address");
        }

        for (int i = 0; i < 4; i++) {
            ip[i] = (byte) Integer.parseInt(parts[i]);
        }

        return ip;
    }

    /*
     * Convert a byte (signed char) to an unsigned int
     * The target specific data is stored in memory as a byte array and these need to be
     * converted to the proper format before being used.
     */
    private int getUnsignedByte(byte data) {
        if (data < 0) {
            return 256 - Math.abs(data);
        }
        return data;
    }

    /**
     * Build the payload that should be written to the NFC tag memory
     */
    public void buildPayload(final Activity activity) {

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
        et = (EditText) activity.findViewById(R.id.espSystemName);
        device = getDevice(et.getText().toString());

        // DHCP enabled
        cb = (CheckBox) activity.findViewById(R.id.espEnableDhcp);
        if (cb.isChecked() == true) {
            dhcpEnabled[0] = 1;
        } else {
            dhcpEnabled[0] = 0;
        }

        // IP Address
        et = (EditText) activity.findViewById(R.id.espIpAddress);
        try {
            ip = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in IP address, setting it to 0.0.0.0" + e);
            ip = new byte[4];
            ip[0] = ip[1] = ip[2] = ip[3] = 0;
        }

        // Netmask
        et = (EditText) activity.findViewById(R.id.espNetmask);
        try {
            netmask = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in Netmask address, setting it to 0.0.0.0" + e);
            netmask = new byte[4];
            netmask[0] = netmask[1] = netmask[2] = netmask[3] = 0;
        }

        // Default gateway
        et = (EditText) activity.findViewById(R.id.espGateway);
        try {
            gateway = ipToArray(et.getText().toString());
        } catch (ArithmeticException e) {
            Log.d(TAG, "Error in Default gateway address, setting it to 0.0.0.0" + e);
            gateway = new byte[4];
            gateway[0] = gateway[1] = gateway[2] = gateway[3] = 0;
        }

        // Webserver port
        et = (EditText) activity.findViewById(R.id.espWebServerport);
        imPort = Integer.parseInt(et.getText().toString());
        port[0] = (byte) (imPort & 255);
        port[1] = (byte) ((imPort / 256) & 255);

        // Create a new payload stream
        streamCreate();
        // Write objects to the stream
        streamData(device);
        streamData(dhcpEnabled);
        streamData(ip);
        streamData(netmask);
        streamData(gateway);
        streamData(port);
    }

    private void enableDisableAddresses(Activity activity, boolean checked) {

        Log.d(TAG, "Setting setEnabled to " + checked);
        // Enable or disable text fields accordingly.
        EditText edAddress = (EditText) activity.findViewById(R.id.espIpAddress);
        edAddress.setEnabled(checked);

        edAddress = (EditText) activity.findViewById(R.id.espNetmask);
        edAddress.setEnabled(checked);

        edAddress = (EditText) activity.findViewById(R.id.espGateway);
        edAddress.setEnabled(checked);
    }

    /*
     * It is always our responsibility to handle what happens on the screen, so all onClick events
     * from the main activity gets routed here.
     */
    public void onClickEvent(Activity activity, View view) {

        switch (view.getId()) {
            case R.id.espEnableDhcp:
                boolean checked = !((CheckBox) view).isChecked();
                enableDisableAddresses(activity, checked);
                break;

            default:
                Log.d(TAG, "Unhandled view element detected !");
        }
    }

    public void setPayload(Activity activity, byte[] payload) {
        EditText et;

        // system name
        et = (EditText) activity.findViewById(R.id.espSystemName);
        et.setText(this.getDeviceString(payload[0]));

        // DHCP Switch
        CheckBox cb = (CheckBox) activity.findViewById(R.id.espEnableDhcp);
        if (payload[1] != 0) {
            cb.setChecked(true);
            enableDisableAddresses(activity, false);
        } else {
            cb.setChecked(false);
            enableDisableAddresses(activity, true);
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
        et = (EditText) activity.findViewById(R.id.espIpAddress);
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
        et = (EditText) activity.findViewById(R.id.espNetmask);
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
        et = (EditText) activity.findViewById(R.id.espGateway);
        et.setText(gw);

        //  Webserver port
        int port = getUnsignedByte(payload[14]) |
                getUnsignedByte(payload[15]) * 256;
        et = (EditText) activity.findViewById(R.id.espWebServerport);
        et.setText(Integer.toString(port));
    }

    public void screenInit(Activity activity) {
        // Connect our new key listener to the address field.
        EditText edAddress = (EditText) activity.findViewById(R.id.espIpAddress);
        edAddress.setKeyListener(IPAddressKeyListener.getInstance());

        edAddress = (EditText) activity.findViewById(R.id.espNetmask);
        edAddress.setKeyListener(IPAddressKeyListener.getInstance());

        edAddress = (EditText) activity.findViewById(R.id.espGateway);
        edAddress.setKeyListener(IPAddressKeyListener.getInstance());

        edAddress = (EditText) activity.findViewById(R.id.espWebServerport);
        edAddress.setKeyListener(IPAddressKeyListener.getInstance());

    }

}

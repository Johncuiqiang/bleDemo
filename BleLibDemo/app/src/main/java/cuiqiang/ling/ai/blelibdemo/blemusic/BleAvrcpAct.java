package cuiqiang.ling.ai.blelibdemo.blemusic;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import cuiqiang.ling.ai.blelibdemo.R;
import cuiqiang.ling.ai.blelibdemo.reflect.FieldUtils;
import cuiqiang.ling.ai.blelibdemo.reflect.MethodUtils;

/**
 * Created by cuiqiang on 17-12-25.
 *
 * @author cuiqiang
 */
public class BleAvrcpAct extends Activity {

    private static final String TAG = "ble";
    private Object mAvrcpController;
    private TextView mStatusView;
    private TextView mAttrsView;
    private int AVRCP_CONTROLLER;
    private int PASSTHROUGH_STATE_PRESS;
    private int PASSTHROUGH_STATE_RELEASE;
    private int PASSTHROUGH_ID_PLAY;
    private int PASSTHROUGH_ID_BACKWARD;
    private int PASSTHROUGH_ID_FORWARD;
    private int PASSTHROUGH_ID_PAUSE;
    private int PASSTHROUGH_ID_STOP;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_avrcp);
        initView();
        initData();
    }

    private void initView() {
        mStatusView = findViewById(R.id.status);
        mAttrsView =  findViewById(R.id.attrs);
    }

    private void initData() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            AVRCP_CONTROLLER = (int) FieldUtils.readStaticField(BluetoothProfile.class, "AVRCP_CONTROLLER");
            Class cls = Class.forName("android.bluetooth.BluetoothAvrcp");
            PASSTHROUGH_STATE_PRESS = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_STATE_PRESS");
            PASSTHROUGH_STATE_RELEASE = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_STATE_RELEASE");
            PASSTHROUGH_ID_PLAY = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_ID_PLAY");
            PASSTHROUGH_ID_STOP = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_ID_STOP");
            PASSTHROUGH_ID_PAUSE = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_ID_PAUSE");
            PASSTHROUGH_ID_FORWARD = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_ID_FORWARD");
            PASSTHROUGH_ID_BACKWARD = (int) FieldUtils.readStaticField(cls, "PASSTHROUGH_ID_BACKWARD");
        } catch (Exception e) {
            e.printStackTrace();
        }
        bluetoothAdapter.getProfileProxy(this, mAvrcpServiceListener, AVRCP_CONTROLLER);
        mStatusView.setText("Connecting to the AVRCP_CONTROLLER service");
    }

    private BluetoothProfile.ServiceListener mAvrcpServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "AvrcpControllerService connected");
            if (profile == AVRCP_CONTROLLER) {
                mStatusView.setText("AVRCP_CONTROLLER connected");
                Log.d(TAG, "AvrcpControllerService connected");

//                mAvrcpController = (BluetoothAvrcpController) proxy;
                mAvrcpController = proxy;
                try {
//                    MethodUtils.invokeMethod(mAvrcpController, "setCallback", new AvrcpControllerCallback());
                    mStatusView.append("\r\nAvrcp devices: \r\n");
                    List<BluetoothDevice> devices = (List<BluetoothDevice>) MethodUtils.invokeMethod(mAvrcpController, "getConnectedDevices");
                    for (BluetoothDevice device : devices) {
                        mStatusView.append(" - " + device.getName() + " " + device.getAddress() + "\r\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == AVRCP_CONTROLLER) {
                mStatusView.setText("AVRCP_CONTROLLER disconnected");
                Log.d(TAG, "AvrcpControllerService disconnected");
                try {
                    MethodUtils.invokeMethod(mAvrcpController, "removeCallback");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mAvrcpController = null;
            }
        }
    };

    private void sendCommand(int keyCode) {
        if (mAvrcpController == null) {
            return;
        }
        List<BluetoothDevice> devices = null;
        try {
            devices = (List<BluetoothDevice>) MethodUtils.invokeMethod(mAvrcpController, "getConnectedDevices");
            for (BluetoothDevice device : devices) {
                Log.d(TAG, "send command to device: " + device.getName() + " " + device.getAddress());
                MethodUtils.invokeMethod(mAvrcpController, "sendPassThroughCmd", device, keyCode, PASSTHROUGH_STATE_PRESS);
                MethodUtils.invokeMethod(mAvrcpController, "sendPassThroughCmd", device, keyCode, PASSTHROUGH_STATE_RELEASE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onPlayButtonClick(View view) {
        sendCommand(PASSTHROUGH_ID_PLAY);
    }

    public void onStopButtonClick(View view) {
        sendCommand(PASSTHROUGH_ID_STOP);
    }

    public void onPauseButtonClick(View view) {
        sendCommand(PASSTHROUGH_ID_PAUSE);
    }

    public void onNextButtonClick(View view) {
        sendCommand(PASSTHROUGH_ID_FORWARD);
    }

    public void onPrevButtonClick(View view) {
        sendCommand(PASSTHROUGH_ID_BACKWARD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            MethodUtils.invokeMethod(mAvrcpController, "removeCallback");
        } catch (Exception e) {
            e.printStackTrace();
        }
        bluetoothAdapter.closeProfileProxy(AVRCP_CONTROLLER, (BluetoothProfile) mAvrcpController);
    }

}

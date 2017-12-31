package cuiqiang.ling.ai.blelibdemo.client;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

/**
 * Created by cuiqiang on 17-12-26.
 *
 * @author cuiqiang
 */

public class BleFoundReceiver extends BroadcastReceiver {

    private static final String TAG = "ble";

    List<String> mList;
    ArrayAdapter<String> mAdapter;
    ListView mListView;
    Button mButton;

    public BleFoundReceiver(List<String> deviceList, ArrayAdapter<String> arrayAdapter, ListView mListView,
                               Button mButton) {
        this.mList = deviceList;
        this.mAdapter = arrayAdapter;
        this.mListView = mListView;
        this.mButton = mButton;
    }

    public BleFoundReceiver(List<String> msgList, ArrayAdapter<String> mAdapter, ListView lv_device) {
        this.mList = msgList;
        this.mAdapter = mAdapter;
        this.mListView = lv_device;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        // 广播活动：发现远程设备
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // 每次通过该类进行广播时，作为Parcelable
            // BluetoothDevice的附加域。它包含了该常量适用的BluetoothDevice类。
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                mList.add(device.getName() + "\n" + device.getAddress());
                mAdapter.notifyDataSetChanged();
            }
        }

        // 广播事件：本地蓝牙适配器已经完成设备的搜寻过程。
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            if (mListView.getCount() == 0) {
                mList.add("未搜素到配对的设备");
                mAdapter.notifyDataSetChanged();
            }
        }

        // 广播活动：指明一个与远程设备建立的低级别（ACL）连接。
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Log.e(TAG, "ACTION_ACL_CONNECTED");
        }

        // 可以识别本机主动断开，但是无法识别远程是否断开
        // 广播活动：指明一个为远程设备提出的低级别（ACL）的断开连接请求，并即将断开连接。
        if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            Log.e(TAG, "ACTION_ACL_DISCONNECT_REQUESTED");
        }

        // 广播活动：指明一个来自于远程设备的低级别（ACL）连接的断开
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            Log.e(TAG, "ACTION_ACL_DISCONNECTED");
        }

        if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    Log.d(TAG, "正在配对......");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.d(TAG, "完成配对");
                    break;
                case BluetoothDevice.BOND_NONE:
                    Log.d(TAG, "取消配对");
                    break;
                default:
                    break;
            }
        }
    }
}

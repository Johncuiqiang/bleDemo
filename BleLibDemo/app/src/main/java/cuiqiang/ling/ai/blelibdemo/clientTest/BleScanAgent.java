package cuiqiang.ling.ai.blelibdemo.clientTest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_FIRST_USER;

/**
 * Created by cuiqiang on 17-12-25.
 *
 * @author cuiqiang
 */

public class BleScanAgent {

    private static final String TAG = "ble";

    private Context mContext;

    // 本机蓝牙可被发现时间
    private int mDiscoverableTime = 300;
    // 是否已开启本机蓝牙可见
    private final int RESULT_DISCOVERABLE = 0x12;
    // 蓝牙开启方式：true 窗口开启蓝牙，false 后台直接打开
    private boolean isOpenWindow = true;
    private boolean isRegister = false;
    private ClientFoundReceiver mDeviceReceiver;
    private BluetoothAdapter mBluetoothAdapter = null;

    public BleScanAgent(Context context) {
        mContext = context;
    }

    /**
     * 配置蓝牙功能
     */
    public void setBleEnable() {
        Log.e(TAG, "setBluetooth()");
        // 获取蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            // 确认开启蓝牙
            if (!mBluetoothAdapter.isEnabled()) {
                openBluetooth();
            } else {
                // 让设备可见
                discoverBluetooth();
            }
        }
    }

    /**
     * 开始扫描
     */
    public void startDiscovery() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startDiscovery();
        }
    }

    /**
     * 是否正在搜索
     */
    public boolean isDiscovering() {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.isDiscovering();
        }
        return false;
    }

    /**
     * 取消搜索
     */
    public void cancelDiscovery() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * 打开蓝牙
     */
    public void openBluetooth() {
        if (isOpenWindow) {
            // 请求用户开启
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mContext).startActivityForResult(intent, RESULT_FIRST_USER);
        } else {
            // 直接开启，不经过提示
            mBluetoothAdapter.enable();
            discoverBluetooth();
        }
    }


    /**
     * 打开蓝牙方式
     *
     * @param openWindow true 窗口开启蓝牙，false 后台直接打开
     */
    public void setOpenWindow(boolean openWindow) {
        isOpenWindow = openWindow;
    }

    /**
     * 注册广播
     */
    public void registerReceiver(List<String> deviceList, ArrayAdapter<String> arrayAdapter,
                                 ListView listView, Button button) {
        // 如未注册，则注册
        if (!isRegister) {
            isRegister = true;
            IntentFilter filter = new IntentFilter();

            // 广播活动：发现远程设备
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            // 广播事件：本地蓝牙适配器已经完成设备的搜寻过程。
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            // 广播活动：指明一个与远程设备建立的低级别（ACL）连接。
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            // 广播活动：指明一个为远程设备提出的低级别（ACL）的断开连接请求，并即将断开连接。
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            // 广播活动：指明一个来自于远程设备的低级别（ACL）连接的断开
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            //广播活动： 监听蓝牙连接的状态
            filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            mDeviceReceiver = new ClientFoundReceiver(deviceList, arrayAdapter, listView, button);
            mContext.registerReceiver(mDeviceReceiver, filter);
        }
    }

    /**
     * 返回蓝牙配对设备
     *
     * @return 蓝牙配对设备
     */
    public Set<BluetoothDevice> getBondedDevices() {
        // 获取可配对蓝牙设备
        if (null != mBluetoothAdapter) {
            return mBluetoothAdapter.getBondedDevices();
        }
        return null;
    }

    /**
     * 解绑广播注册
     */
    public void release() {
        // 如果当前蓝牙适配器正处于设备发现查找进程中，则将返回true
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            // 取消当前的设备发现查找进程
            mBluetoothAdapter.cancelDiscovery();
        }
        // 如已注册广播，解除注册
        if (isRegister) {
            isRegister = false;
            mContext.unregisterReceiver(mDeviceReceiver);
        }
    }

    /**
     * 让设备可见
     */
    private void discoverBluetooth() {
        // 若未被设为可见
        if (!BluetoothMsg.isShowDevice) {
            Log.e(TAG, "discoverBluetooth()");
            // Activity活动：显示一个请求被搜寻模式的系统活动。如果蓝牙模块当前未打开，该活动也将请求用户打开蓝牙模块。
            Intent intentDiscov = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            // 让当前蓝牙设备可见，默认值为120秒，超过300秒的请求将被限制
            intentDiscov.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mDiscoverableTime);
            Log.d(TAG, "当前设备蓝牙已开启被发现，时间为" + mDiscoverableTime + "秒");
            ((Activity) mContext).startActivityForResult(intentDiscov, RESULT_DISCOVERABLE);
            BluetoothMsg.isShowDevice = true;
            timeOut();
        }
    }

    /**
     * 本机蓝牙被发现到超时关闭
     */
    private void timeOut() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                BluetoothMsg.isShowDevice = false;
            }
        }, mDiscoverableTime * 1000);
    }

    private class ClientFoundReceiver extends BroadcastReceiver {

        private static final String TAG = "ble";

        List<String> mList;
        ArrayAdapter<String> mAdapter;
        ListView mListView;
        Button mButton;

        public ClientFoundReceiver(List<String> deviceList, ArrayAdapter<String> arrayAdapter, ListView mListView,
                                   Button mButton) {
            this.mList = deviceList;
            this.mAdapter = arrayAdapter;
            this.mListView = mListView;
            this.mButton = mButton;
        }

        public ClientFoundReceiver(List<String> msgList, ArrayAdapter<String> mAdapter, ListView lv_device) {
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





}

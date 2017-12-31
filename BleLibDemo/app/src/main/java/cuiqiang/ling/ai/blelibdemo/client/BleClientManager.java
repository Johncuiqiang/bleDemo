package cuiqiang.ling.ai.blelibdemo.client;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import cuiqiang.ling.ai.blelibdemo.clientTest.ClientSearchActivity;


/**
 * Created by cuiqiang on 17-11-20.
 *
 * @author cuiqiang
 */
public class BleClientManager {

    private static final String TAG = "ble";
    private static BleClientManager INSTANCE;

    public static final int SERVER_CONNECTING = 0x01;//正在连接服务端
    public static final int SERVER_CONNECTED = 0x02;//连接服务端成功
    public static final int SERVER_CONNECT_ERROR = 0x03;//连接服务端失败
    public static final int MESSAGE = 0x04;//收发消息
    private final int RESULT_DISCOVERABLE = 0x12;

    private UUID mUuid;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BleFoundReceiver mBleFoundReceiver;//蓝牙广播
    private ClientThread mClientThread;//客户端发消息线程
    private ReadThread mReadThread; //客户端读消息线程
    private BluetoothSocket mClientSocket; //蓝牙socket连接
    private BluetoothDevice mDevice;//蓝牙远程设备对象
    private Handler mReceiveHandler;
    private IBleConnectState mIBleConnectState;
    private String mBlueToothAddress;//蓝牙地址
    private String mBlueToothName;//蓝牙设备名称

    private int mDiscoverableTime = 300;// 本机蓝牙可被发现时间
    private int mLastMsgResultWhat = 0x00;
    private boolean isRegister = false;//是否注册蓝牙广播
    private boolean isShowDevice = false; //蓝牙可见, 为防止多次重新打开蓝牙

    public static BleClientManager getInstance() {
        if (null == INSTANCE) {
            synchronized (BleClientManager.class) {
                if (null == INSTANCE) {
                    INSTANCE = new BleClientManager();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    /**
     * 配置蓝牙功能
     */
    public void setBleEnable() {
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
     * 打开蓝牙
     */
    public void openBluetooth() {
        if (null != mBluetoothAdapter) {
            // 直接开启，不经过提示
            mBluetoothAdapter.enable();
            discoverBluetooth();
        }
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
            mBleFoundReceiver = new BleFoundReceiver(deviceList, arrayAdapter, listView, button);
            mContext.registerReceiver(mBleFoundReceiver, filter);
        }
    }

    /**
     * 释放资源
     */
    public void unregisterReceiver() {
        // 如果当前蓝牙适配器正处于设备发现查找进程中，则将返回true
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            // 取消当前的设备发现查找进程
            mBluetoothAdapter.cancelDiscovery();
        }
        // 如已注册广播，解除注册
        if (isRegister) {
            isRegister = false;
            mContext.unregisterReceiver(mBleFoundReceiver);
        }
    }

    /**
     * 得到蓝牙adapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * 获取远端设备名
     *
     * @return 远端设备名
     */
    public String getRmoteDeviceName() {
        return mDevice != null ? mDevice.getName() : "";
    }

    /**
     * 获取本机设备名
     *
     * @return 本机设备名
     */
    public String getDeviceName() {
        return mBluetoothAdapter != null ? mBluetoothAdapter.getName() : "";
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
     * 蓝牙连接以及scoket连接
     */
    public void connect(String blueToothAddress,String blueToothName){
        mBlueToothAddress = blueToothAddress;
        mBlueToothName = blueToothName;
        // 校验是否已连接
        if (mClientSocket != null && mClientSocket.isConnected()) {
            Toast.makeText(mContext, "设备已连接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (null != mIBleConnectState){
            mIBleConnectState.onConnecting();
        }
        Log.e(TAG, "address:" + mBlueToothAddress);
        if (!mBlueToothAddress.equals("null")) {
            mDevice = mBluetoothAdapter.getRemoteDevice(mBlueToothAddress);
            mClientThread = new ClientThread();
            mClientThread.start();
        } else {
            Toast.makeText(mContext, "连接已断开，请重新获取连接", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, ClientSearchActivity.class);
            mContext.startActivity(intent);
        }
    }

    /**
     * 蓝牙连接以及scoket连接
     */
    public void reconnect(){
        // 校验是否已连接
        if (mClientSocket != null && mClientSocket.isConnected()) {
            Toast.makeText(mContext, "设备已连接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (null != mIBleConnectState){
            mIBleConnectState.onConnecting();
        }
        Log.e(TAG, "address:" + mBlueToothAddress);
        if (!mBlueToothAddress.equals("null")) {
            mDevice = mBluetoothAdapter.getRemoteDevice(mBlueToothAddress);
            mClientThread = new ClientThread();
            mClientThread.start();
        } else {
            Toast.makeText(mContext, "连接已断开，请重新获取连接", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, ClientSearchActivity.class);
            mContext.startActivity(intent);
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(String msg) {
        if (mClientSocket == null) {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        OutputStream os;
        try {
            os = mClientSocket.getOutputStream();
            os.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 蓝牙连接回调接口
     * @param iBleConnectState 回调接口
     */
    public void setIBleConnectState(IBleConnectState iBleConnectState) {
        this.mIBleConnectState = iBleConnectState;
    }


    public void setReceiveHandler(Handler receiveHandler) {
        this.mReceiveHandler = receiveHandler;
    }

    /**
     * 释放
     */
    public void release() {
        shutdownClient();
    }

    /**
     * 客户端启动线程
     *
     */
    private class ClientThread extends Thread {

        @Override
        public void run() {
            Log.e(TAG, "ClientThread...running");
            try {
                Log.e(TAG, "mDevice=" + mDevice);
                mClientSocket = mDevice.createRfcommSocketToServiceRecord(mUuid);
                String msg = mBlueToothName + mBlueToothAddress + "\n" + "客户端已启动，正在连接服务器...";
                sendHandlerMsg(SERVER_CONNECTING, msg);
                mClientSocket.connect();
                String success = "已连接上服务器，可发送消息";
                sendHandlerMsg(SERVER_CONNECTED, success);
                if (null != mIBleConnectState) {
                    mIBleConnectState.onComplete();
                }
                // 启动接收数据
                mReadThread = new ReadThread(mClientSocket);
                mReadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                if (null != mIBleConnectState) {
                    mIBleConnectState.onError();
                }
                String msg = "服务端未开启...请在另一台设备上先启动服务端...然后点击重试";
                sendHandlerMsg(SERVER_CONNECT_ERROR, msg);
            }
            super.run();
        }
    }


    /**
     * 读取数据
     */
    private class ReadThread extends Thread {

        private BluetoothSocket socket;

        public ReadThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Log.e(TAG, "ReadThread...running");

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream = null;

            try {
                inputStream = socket.getInputStream();
                while (true) {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        byte[] bufData = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            bufData[i] = buffer[i];
                        }
                        String msg = new String(bufData);
                        sendHandlerMsg(MESSAGE, msg);
                    }
                }
            } catch (Exception e) {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            super.run();
        }
    }

    /**
     * 关闭客户端线程和流
     */
    private void shutdownClient() {
        Log.e(TAG, "shutdownClient()");

        if (mClientThread != null) {
            mClientThread.interrupt();
            mClientThread = null;
        }
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息到handler
     *
     * @param type 不同类别
     * @param data 消息
     */
    private void sendHandlerMsg(int type, Object data) {
        if (null != mReceiveHandler) {
            if (mReceiveHandler.hasMessages(mLastMsgResultWhat)) {
                mReceiveHandler.removeMessages(mLastMsgResultWhat);
            }
            Message msg = mReceiveHandler.obtainMessage();
            msg.what = type;
            msg.obj = data;
            mReceiveHandler.sendMessage(msg);
            //缓存当前result 对象
            this.mLastMsgResultWhat = type;
        }
    }

    /**
     * 让设备可见
     */
    private void discoverBluetooth() {
        // 若未被设为可见
        if (!isShowDevice) {
            Log.e(TAG, "discoverBluetooth()");
            // Activity活动：显示一个请求被搜寻模式的系统活动。如果蓝牙模块当前未打开，该活动也将请求用户打开蓝牙模块。
            Intent intentDiscov = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            // 让当前蓝牙设备可见，默认值为120秒，超过300秒的请求将被限制
            intentDiscov.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mDiscoverableTime);
            Log.d(TAG, "当前设备蓝牙已开启被发现，时间为" + mDiscoverableTime + "秒");
            ((Activity) mContext).startActivityForResult(intentDiscov, RESULT_DISCOVERABLE);
            isShowDevice = true;
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
                isShowDevice = false;
            }
        }, mDiscoverableTime * 1000);
    }

    /**
     * 设置要连接的蓝牙地址
     * @param blueToothAddress 蓝牙地址
     */
    public void setBlueToothAddress(String blueToothAddress) {
        this.mBlueToothAddress = blueToothAddress;
    }

    /**
     * 设置要连接的蓝牙名称
     * @param blueToothName 蓝牙名称
     */
    public void setBlueToothName(String blueToothName) {
        this.mBlueToothName = blueToothName;
    }

    public interface IBleConnectState {

        void onConnecting();

        void onComplete();

        void onError();
    }
}

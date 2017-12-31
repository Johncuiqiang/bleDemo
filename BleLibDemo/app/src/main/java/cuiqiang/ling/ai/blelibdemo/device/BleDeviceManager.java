package cuiqiang.ling.ai.blelibdemo.device;

import android.content.Context;
import android.text.TextUtils;

/**
 * Created by cuiqiang on 17-11-17.
 *
 * @author cuiqiang
 */

public class BleDeviceManager {

    private static final String TAG = "ble";

    private Context mContext;
    private BleDeviceAgent mBleDeviceAgent;

    public BleDeviceManager(Context context){
        mContext = context;
        mBleDeviceAgent = new BleDeviceAgent(mContext);
    }

    /**
     *  打开蓝牙
     */
    public void openBle(){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.openBle();
        }
    }

    /**
     * 使蓝牙可发现,时间300秒,系统允许的最大时间
     */
    public void discoverBluetooth(){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.discoverBluetooth();
        }
    }

    /**
     * 发送信息
     */
    public void sendMessage(String message){
        if (!TextUtils.isEmpty(message) && null != mBleDeviceAgent) {
            mBleDeviceAgent.sendMessage(message);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect(){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.disconnect();
        }
    }

    /**
     * 重新连接
     */
    public void connect(){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.connect();
        }
    }

    /**
     * 释放资源
     */
    public void release(){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.release();
        }
    }

    /**
     * 设置接收数据的监听回调
     * @param iReceiveMsgCallback
     */
    public void setMsgCallback(IReceiveMsgCallback iReceiveMsgCallback){
        if (null != mBleDeviceAgent) {
            mBleDeviceAgent.setMsgCallback(iReceiveMsgCallback);
        }
    }

    public interface IReceiveMsgCallback{

        void onReceive(String message);
    }

}

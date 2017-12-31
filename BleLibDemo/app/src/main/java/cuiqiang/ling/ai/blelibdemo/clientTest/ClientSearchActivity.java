package cuiqiang.ling.ai.blelibdemo.clientTest;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import cuiqiang.ling.ai.blelibdemo.R;

public class ClientSearchActivity extends Activity implements OnItemClickListener {

	private static final String TAG = "ble";

	private ListView mLvList;
	private Button mBtnSearch;
	private Context mContext;
	private ArrayAdapter<String> arrayAdapter;
	private ArrayList<String> deviceList;
	private BleScanAgent mBleScanAgent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_device_search);

		// 初始化控件
		initView();
		// 适配
		initAdapter();
		// 监听
		initListener();
		// 配置蓝牙
		setBluetooth();
		// 开启广播
		regsRecever();
	}

	private void initView() {
		Log.e(TAG, "initView()");
		mContext = this;
		mLvList =findViewById(R.id.lv_list);
		mBtnSearch = findViewById(R.id.btn_search);
	}

	private void initAdapter() {
		Log.e(TAG,"initAdapter()");

		// 适配ListView，用于展示附近的蓝牙设备
		deviceList = new ArrayList<>();
		arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
		mLvList.setAdapter(arrayAdapter);

	}

	/**
	 * listView的点击监听
	 */
	private void initListener() {
		Log.e(TAG,"initListener()");
		mLvList.setOnItemClickListener(this);
		mBtnSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//取消搜索
				if (mBleScanAgent.isDiscovering()) {
					mBleScanAgent.cancelDiscovery();
					mBtnSearch.setText("重新搜索");
				} else {
					//开始搜索
					mBleScanAgent.setBleEnable();
					findAvalibeDevice();
					mBleScanAgent.startDiscovery();
					mBtnSearch.setText("停止搜索");
				}
			}
		});
	}

	/**
	 * 注册蓝牙，接收广播
	 */
	private void regsRecever() {
		Log.e(TAG,"regsRecever()");
		mBleScanAgent.registerReceiver(deviceList, arrayAdapter, mLvList, mBtnSearch);
		super.onStart();
	}

	/**
	 * 界面销毁前，退出蓝牙功能
	 */
	@Override
	protected void onDestroy() {
		Log.e(TAG,"onDestroy()");
		mBleScanAgent.release();
		super.onDestroy();
	}

	/**
	 * 配置蓝牙功能
	 */
	public void setBluetooth() {
		Log.e(TAG, "setBluetooth()");
		mBleScanAgent = new BleScanAgent(mContext);
	}


	/*
	 * 寻找可见的蓝牙设备
	 */
	public void findAvalibeDevice() {
		Log.e(TAG, "findAvalibeDevice()");

		// 获取可配对蓝牙设备
		Set<BluetoothDevice> devices = mBleScanAgent.getBondedDevices();
		if (mBleScanAgent.isDiscovering()) {
			Log.e(TAG, "清空列表");
			deviceList.clear();
			arrayAdapter.notifyDataSetChanged();
		}
		if (devices.size() > 0) {
			deviceList.clear();
			Log.e(TAG, "搜索到已配对设备数量为：" + devices.size());
			Iterator<BluetoothDevice> iterator = devices.iterator();
			while (iterator.hasNext()) {
				BluetoothDevice bthDevice = iterator.next();
				deviceList.add(bthDevice.getName() + "（已配对）" + "\n" + bthDevice.getAddress());
				arrayAdapter.notifyDataSetChanged();
			}
		} else {
			// 没有配对设备
			deviceList.add("没有已配对的设备记录");
			arrayAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.e(TAG, "onItemClick()");

		final String msg = deviceList.get(arg2);
		if (mBleScanAgent.isDiscovering()) {
			mBleScanAgent.cancelDiscovery();
			mBtnSearch.setText("重新搜索");
		}

		Builder dialog = new Builder(this).setTitle("确认连接设备").setMessage(msg);
		dialog.setPositiveButton("连接", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				BluetoothMsg.BlueToothAddress = msg.substring(msg.length() - 17);
				BluetoothMsg.BlueToothName = msg.substring(0, msg.length() - 17);
				if (BluetoothMsg.LastBlueToothAddress != BluetoothMsg.BlueToothAddress) {
					BluetoothMsg.LastBlueToothAddress = BluetoothMsg.BlueToothAddress;
				}
			    toNextPage();
			}
		});
		dialog.setNegativeButton("取消", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				BluetoothMsg.BlueToothAddress = null;
				BluetoothMsg.BlueToothName = null;
			}
		});
		dialog.show();
	}

	// 前往下一页
	public void toNextPage() {
		Intent intent = new Intent(ClientSearchActivity.this, ClientBleActivity.class);
		startActivity(intent);
	}

}

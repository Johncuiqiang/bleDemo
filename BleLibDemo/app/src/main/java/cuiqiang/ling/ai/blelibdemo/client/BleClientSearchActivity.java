package cuiqiang.ling.ai.blelibdemo.client;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.MESSAGE;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTED;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTING;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECT_ERROR;

public class BleClientSearchActivity extends Activity implements OnItemClickListener {

	private static final String TAG = "ble";

	private ListView mLvList;
	private Button mBtnSearch;
	private Context mContext;
	private ArrayAdapter<String> arrayAdapter;
	private ArrayList<String> mDeviceList;
	private Handler mHandler = new Handler();
	private BluetoothAdapter mBluetoothAdapter;

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
		mDeviceList = new ArrayList<>();
		arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);
		mLvList.setAdapter(arrayAdapter);
		BleClientManager.getInstance().init(mContext);
		ReceiveHandler receiveHandler = new ReceiveHandler(mHandler.getLooper());
		BleClientManager.getInstance().setReceiveHandler(receiveHandler);
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
			if (null != mBluetoothAdapter && mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
				mBtnSearch.setText("重新搜索");
			} else {
				//开始搜索
				BleClientManager.getInstance().setBleEnable();
				findAvalibeDevice();
				mBluetoothAdapter.startDiscovery();
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
		BleClientManager.getInstance().registerReceiver(mDeviceList, arrayAdapter, mLvList, mBtnSearch);
		super.onStart();
	}

	/**
	 * 界面销毁前，退出蓝牙功能
	 */
	@Override
	protected void onDestroy() {
		Log.e(TAG,"onDestroy()");
		BleClientManager.getInstance().unregisterReceiver();
		super.onDestroy();
	}

	/**
	 * 配置蓝牙功能
	 */
	public void setBluetooth() {
		Log.e(TAG, "setBluetooth()");
		BleClientManager.getInstance().setBleEnable();
		mBluetoothAdapter = BleClientManager.getInstance().getBluetoothAdapter();
	}


	/*
	 * 寻找可见的蓝牙设备
	 */
	public void findAvalibeDevice() {
		Log.e(TAG, "findAvalibeDevice()");

		// 获取可配对蓝牙设备
		Set<BluetoothDevice> devices = BleClientManager.getInstance().getBondedDevices();
		if (null != mBluetoothAdapter && mBluetoothAdapter.isDiscovering()) {
			Log.e(TAG, "清空列表");
			mDeviceList.clear();
			arrayAdapter.notifyDataSetChanged();
		}
		if (devices.size() > 0) {
			mDeviceList.clear();
			Log.e(TAG, "搜索到已配对设备数量为：" + devices.size());
			Iterator<BluetoothDevice> iterator = devices.iterator();
			while (iterator.hasNext()) {
				BluetoothDevice bthDevice = iterator.next();
				mDeviceList.add(bthDevice.getName() + "（已配对）" + "\n" + bthDevice.getAddress());
				arrayAdapter.notifyDataSetChanged();
			}
		} else {
			// 没有配对设备
			mDeviceList.add("没有已配对的设备记录");
			arrayAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.e(TAG, "onItemClick()");

		final String msg = mDeviceList.get(arg2);
		if (null != mBluetoothAdapter && mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
			mBtnSearch.setText("重新搜索");
		}

		Builder dialog = new Builder(this).setTitle("确认连接设备").setMessage(msg);
		dialog.setPositiveButton("连接", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String blueToothAddress = msg.substring(msg.length() - 17);
				String blueToothName = msg.substring(0, msg.length() - 17);
				BleClientManager.getInstance().setIBleConnectState(new BleClientManager.IBleConnectState() {
					@Override
					public void onConnecting() {
						Log.e(TAG, "onConnecting()");
					}

					@Override
					public void onComplete() {
						Log.e(TAG, "onComplete()");
						toNextPage();
					}

					@Override
					public void onError() {
						Log.e(TAG, "onError()");
						BleClientManager.getInstance().release();
					}
				});
				BleClientManager.getInstance().connect(blueToothAddress,blueToothName);
			}
		});
		dialog.setNegativeButton("取消", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				BleClientManager.getInstance().setBlueToothAddress(null);
				BleClientManager.getInstance().setBlueToothName(null);
			}
		});
		dialog.show();
	}

	// 前往下一页
	public void toNextPage() {
		Intent intent = new Intent(BleClientSearchActivity.this, BleClientActivity.class);
		startActivity(intent);
	}

	public class ReceiveHandler extends Handler {

		ReceiveHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int what = msg.what;
			String message = (String) msg.obj;
			switch (what) {
				//连接中
				case SERVER_CONNECTING:
					mDeviceList.add(message);
					break;
				//已经连接
				case SERVER_CONNECTED:
					mDeviceList.add(message);
					break;
				//连接错误
				case SERVER_CONNECT_ERROR:
					mDeviceList.add(message);
					break;
				// 处理收到的消息
				case MESSAGE:

					break;
				default:
					break;
			}
			arrayAdapter.notifyDataSetChanged();
			mLvList.setSelection(mDeviceList.size());
		}
	}
}

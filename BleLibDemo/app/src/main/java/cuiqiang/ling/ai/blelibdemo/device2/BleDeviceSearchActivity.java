package cuiqiang.ling.ai.blelibdemo.device2;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import cuiqiang.ling.ai.blelibdemo.R;
import cuiqiang.ling.ai.blelibdemo.clientTest.BluetoothMsg;
import cuiqiang.ling.ai.blelibdemo.device.BleDeviceManager;
import cuiqiang.ling.ai.blelibdemo.device.DeviceSearchActivity;

import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.MESSAGE;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTED;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTING;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECT_ERROR;

public class BleDeviceSearchActivity extends Activity implements AdapterView.OnItemClickListener {

	private static final String TAG = "ble";
	private Context mContext;
	private EditText mEtText;
	private View mBtnSend;
	private Button mBtnSearch;
	private ListView mLvList;
	private BleDeviceManager mBleDeviceManager;
	private Handler mHandler = new Handler();
	private BluetoothAdapter mBluetoothAdapter;
	private ArrayAdapter<String> mArrayAdapter;
	private ArrayList<String> mDeviceList;
	private ReceiveHandler mReceiveHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.act_device2);
		initView();
		initData();
		initAdapter();
		initListener();
		setBluetooth();
		regsRecever();
	}

	private void initView() {
		mEtText = findViewById(R.id.et_text);
		mBtnSend = findViewById(R.id.btn_send);
		mLvList = findViewById(R.id.lv_device);
		mBtnSearch = findViewById(R.id.btn_search);
	}

	private void initData() {
		mContext = this;
		//初始化服务端
		mBleDeviceManager = new BleDeviceManager(mContext);
		mBleDeviceManager.setMsgCallback(new BleDeviceManager.IReceiveMsgCallback() {
			@Override
			public void onReceive(String message) {
				Log.d(TAG,"message "+message);
				Message msg = mReceiveHandler.obtainMessage();
				msg.what = MESSAGE;
				msg.obj = message;
				mReceiveHandler.sendMessage(msg);
			}
		});
		mBtnSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = mEtText.getText().toString().trim();
				mBleDeviceManager.sendMessage(message);
				BleDeviceClientManager.getInstance().sendMessage(message);
			}
		});
	}

	private void initAdapter() {
		// 适配ListView，用于展示附近的蓝牙设备
		mDeviceList = new ArrayList<>();
		mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);
		mLvList.setAdapter(mArrayAdapter);
		BleDeviceClientManager.getInstance().init(mContext);
		mReceiveHandler = new ReceiveHandler(mHandler.getLooper());
		BleDeviceClientManager.getInstance().setReceiveHandler(mReceiveHandler);
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
					BleDeviceClientManager.getInstance().setBleEnable();
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
		Log.e(TAG, "regsRecever()");
		BleDeviceClientManager.getInstance().registerReceiver(mDeviceList, mArrayAdapter, mLvList, mBtnSearch);
		super.onStart();
	}

	/**
	 * 配置蓝牙功能
	 */
	public void setBluetooth() {
		Log.e(TAG, "setBluetooth()");
		BleDeviceClientManager.getInstance().setBleEnable();
		mBluetoothAdapter = BleDeviceClientManager.getInstance().getBluetoothAdapter();
	}

	/*
	 * 寻找可见的蓝牙设备
	 */
	public void findAvalibeDevice() {
		// 获取可配对蓝牙设备
		Set<BluetoothDevice> devices = BleDeviceClientManager.getInstance().getBondedDevices();
		if (null != mBluetoothAdapter && mBluetoothAdapter.isDiscovering()) {
			Log.e(TAG, "清空列表");
			mDeviceList.clear();
			mArrayAdapter.notifyDataSetChanged();
		}
		if (devices.size() > 0) {
			mDeviceList.clear();
			Log.e(TAG, "搜索到已配对设备数量为：" + devices.size());
			Iterator<BluetoothDevice> iterator = devices.iterator();
			while (iterator.hasNext()) {
				BluetoothDevice bthDevice = iterator.next();
				mDeviceList.add(bthDevice.getName() + "（已配对）" + "\n" + bthDevice.getAddress());
				mArrayAdapter.notifyDataSetChanged();
			}
		} else {
			// 没有配对设备
			mDeviceList.add("没有已配对的设备记录");
			mArrayAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onResume() {
		mBleDeviceManager.connect();
		super.onResume();
	}

	/**
	 * 界面销毁前，退出蓝牙功能
	 */
	@Override
	protected void onDestroy() {
		Log.e(TAG,"onDestroy()");
		mBleDeviceManager.release();
		mBleDeviceManager = null;
		BleDeviceClientManager.getInstance().unregisterReceiver();
		BleDeviceClientManager.getInstance().release();
		super.onDestroy();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Log.e(TAG, "onItemClick()");

		final String msg = mDeviceList.get(arg2);
		if (null != mBluetoothAdapter && mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
			mBtnSearch.setText("重新搜索");
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("确认连接设备").setMessage(msg);
		dialog.setPositiveButton("连接", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				final String blueToothAddress = msg.substring(msg.length() - 17);
				final String blueToothName = msg.substring(0, msg.length() - 17);
				BleDeviceClientManager.getInstance().setIBleConnectState(new BleDeviceClientManager.IBleConnectState() {
					@Override
					public void onConnecting() {
						Log.e(TAG, "onConnecting()");
					}

					@Override
					public void onComplete() {
						Log.e(TAG, "onComplete()");
					}

					@Override
					public void onError() {
						Log.e(TAG, "onError()");
						BleDeviceClientManager.getInstance().release();
					}
				});
				// 选择本机作为客户端还是服务端
				AlertDialog.Builder dialogChoose = new AlertDialog.Builder(BleDeviceSearchActivity.this)
						.setTitle("你要连接第一个服务端,还是第二服务端");
				dialogChoose.setNegativeButton("第一个", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BleDeviceClientManager.getInstance().setWhichConnect(true);
						BleDeviceClientManager.getInstance().connect(blueToothAddress,blueToothName);
					}
				});
				dialogChoose.setPositiveButton("第二个", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BleDeviceClientManager.getInstance().setWhichConnect(false);
						BleDeviceClientManager.getInstance().connect(blueToothAddress,blueToothName);
					}
				});
				dialogChoose.show();

			}
		});
		dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				BleDeviceClientManager.getInstance().setBlueToothAddress(null);
				BleDeviceClientManager.getInstance().setBlueToothName(null);
			}
		});
		dialog.show();
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
					BleDeviceClientManager.getInstance().getRmoteDeviceName();
					mDeviceList.add("：" + message);
					break;
				default:
					break;
			}
			mArrayAdapter.notifyDataSetChanged();
			mLvList.setSelection(mDeviceList.size());
		}
	}
}

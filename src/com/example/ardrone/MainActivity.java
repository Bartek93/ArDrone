package com.example.ardrone;

import android.util.Log;

import com.example.ardrone.ARDroneAPI;
import com.example.ardrone.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.renderscript.Sampler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import ioioservice.ARDroneIOIOService;

public class MainActivity extends Activity implements LocationListener, SensorEventListener
{
	private Button connectBtn;
	private Button disconnectBtn;
	private Button ftrimBtn;
	private Button takeoffBtn;
	private Button landingBtn;
	private Button hoverBtn;
	private Button upBtn;
	private Button downBtn;
	private Button forwardBtn;
	private Button backwardBtn;
	private Button leftBtn;
	private Button rotateLeftBtn;
	private Button rightBtn;
	private Button rotateRightBtn;
	private Button autonomyBtn;
	private Button stopBtn;

	private CheckBox accBox;
	private ToggleButton tg;

	private TextView accTxt;
	private TextView txtVFrontSensor;
	private TextView txtVRightSensor;
	private TextView txtVLeftSensor;

	private static final int SAFE_DISTANCE = 50; // in cm

	private ARDroneAPI drone;
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private Location mCurrentLocation;
	private Location mTargetLocation;

	private double mOrientation;
	protected ARDroneIOIOService mService;

	protected boolean mBound = false;
	private boolean mStarted = false;
	
	private static final boolean PERM_TO_SEND_COMMAND = false;

	private boolean permToAutonomy = true;

	private boolean permToHover = true;
	private boolean permToGoForward = true;
	private boolean permToControllByAcc = false;

	private int sensorDistanceFront;
	private int sensorDistanceRight;
	private int sensorDistanceLeft;

	private float mAkcel[];

	private enum State
	{
		Default, Hover, Forward, Backward, Right, Left
	};

	private State state = State.Default;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.i("MainActivity", "onCreate");

		// dummy date, Auto Pilot is not completed.
		mTargetLocation = new Location("target");

		mTargetLocation.setLatitude(1.0);
		mTargetLocation.setLongitude(1.0);

		mCurrentLocation = new Location("cr");

		mCurrentLocation.setLatitude(1.0);
		mCurrentLocation.setLongitude(1.0);

		Intent intent = new Intent(this, ARDroneIOIOService.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		connectBtn = (Button) findViewById(R.id.connectBtn);
		disconnectBtn = (Button) findViewById(R.id.disconnectBtn);
		ftrimBtn = (Button) findViewById(R.id.ftrimBtn);
		takeoffBtn = (Button) findViewById(R.id.takeOffBtn);
		landingBtn = (Button) findViewById(R.id.landingBtn);
		hoverBtn = (Button) findViewById(R.id.hoverBtn);
		upBtn = (Button) findViewById(R.id.upBtn);
		downBtn = (Button) findViewById(R.id.downBtn);
		forwardBtn = (Button) findViewById(R.id.forwardBtn);
		backwardBtn = (Button) findViewById(R.id.backwardBtn);
		leftBtn = (Button) findViewById(R.id.leftBtn);
		rotateLeftBtn = (Button) findViewById(R.id.rotateLeftBtn);
		rightBtn = (Button) findViewById(R.id.rightBtn);
		rotateRightBtn = (Button) findViewById(R.id.rotateRightBtn);
		autonomyBtn = (Button) findViewById(R.id.autonomyBtn);
		stopBtn = (Button) findViewById(R.id.stopBtn);

		accBox = (CheckBox) findViewById(R.id.accBox);

		txtVFrontSensor = (TextView) findViewById(R.id.txtVFrontSensor);
		txtVRightSensor = (TextView) findViewById(R.id.txtVRightSensor);
		txtVLeftSensor = (TextView) findViewById(R.id.txtVLeftSensor);

		accTxt = (TextView) findViewById(R.id.accTxt);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		initListener();
	}

	private ServiceConnection mConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{

			ARDroneIOIOService.LocalBinder binder = (ARDroneIOIOService.LocalBinder) service;
			mService = binder.getService();
			mBound = true;

			sensorDistanceFront = mService.getSensorDistanceFront();

//			if (mService.isPermToGetDistance1And2())
//			{
//				sensorDistanceRight = mService.getSensorDistanceRight();
//				sensorDistanceLeft = mService.getSensorDistanceLeft();
//			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mBound = false;
		}
	};

	private void initListener()
	{
		connectBtn.setOnClickListener(mClickListener);
		disconnectBtn.setOnClickListener(mClickListener);
		ftrimBtn.setOnClickListener(mClickListener);
		takeoffBtn.setOnClickListener(mClickListener);
		landingBtn.setOnClickListener(mClickListener);
		hoverBtn.setOnClickListener(mClickListener);
		upBtn.setOnClickListener(mClickListener);
		downBtn.setOnClickListener(mClickListener);
		forwardBtn.setOnClickListener(mClickListener);
		backwardBtn.setOnClickListener(mClickListener);
		leftBtn.setOnClickListener(mClickListener);
		rotateLeftBtn.setOnClickListener(mClickListener);
		rightBtn.setOnClickListener(mClickListener);
		rotateRightBtn.setOnClickListener(mClickListener);
		autonomyBtn.setOnClickListener(mClickListener);
		stopBtn.setOnClickListener(mClickListener);

		accBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					Log.i("onCheckedChanged", "checked !!!");
					state = State.Hover;
					//drone.hovering();
					permToControllByAcc = true;
				}
				else
				{
					Log.i("onCheckedChanged", "UNchecked !!!");
					permToControllByAcc = false;
					Log.i("onCheckedChanged", "hover !!!");
					// drone.hovering();
				}
			}
		});
	}

	public OnClickListener mClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
				case R.id.connectBtn:
				{
					new Thread()
					{
						@Override
						public void run()
						{
							Log.i("MainActivity", "Connect");
							try
							{
								drone = new ARDroneAPI();
							}
							catch (Exception e)
							{
								Log.i("OnClickListener", "Exception in Connect !!!");
								e.printStackTrace();
							}
						}
					}.start();
					break;
				}
				case R.id.disconnectBtn:
				{
					Log.i("OnClickListener", "disconnect !!!");
					drone.disbleEmergency();
					break;
				}
				case R.id.ftrimBtn:
				{
					Log.i("OnClickListener", "ftrim !!!");
					drone.trim();
					break;
				}
				case R.id.takeOffBtn:
				{
					Log.i("OnClickListener", "takeoff !!!");
					drone.takeoff();
					break;
				}
				case R.id.landingBtn:
				{
					Log.i("OnClickListener", "landing !!!");
					drone.landing();
					break;
				}
				case R.id.hoverBtn:
				{
					Log.i("OnClickListener", "hover !!!");
					drone.hovering();
					break;
				}
				case R.id.upBtn:
				{
					Log.i("OnClickListener", "up !!!");
					drone.up();
					break;
				}
				case R.id.downBtn:
				{
					Log.i("OnClickListener", "down !!!");
					drone.down();
					break;
				}
				case R.id.forwardBtn:
				{
					Log.i("OnClickListener", "forward !!!");
					drone.goForward();
					break;
				}
				case R.id.backwardBtn:
				{
					Log.i("OnClickListener", "backward !!!");
					drone.goBackward();
					break;
				}
				case R.id.leftBtn:
				{
					Log.i("OnClickListener", "left !!!");
					drone.goLeft();
					break;
				}
				case R.id.rotateLeftBtn:
				{
					Log.i("OnClickListener", "rotate left !!!");
					drone.rotatel();
					break;
				}
				case R.id.rightBtn:
				{
					Log.i("OnClickListener", "right !!!");
					drone.goRight();
					break;
				}
				case R.id.rotateRightBtn:
				{
					Log.i("OnClickListener", "rotate right !!!");
					drone.rotater();
					break;
				}
				case R.id.autonomyBtn:
				{
					Log.i("OnClickListener", "autonomy !!!");

					new Thread()
					{
						@Override
						public void run()
						{
							Log.i("MainActivity", "Autonomy");

							while (true)
							{

								sensorDistanceFront = mService.getSensorDistanceFront();
								Log.d("sensorDistanceFront=", " " + sensorDistanceFront);

								// if (mService.isPermToGetDistance1And2())
								// {
								// sensorDistanceRight =
								// mService.getSensorDistanceRight();
								// sensorDistanceLeft =
								// mService.getSensorDistanceLeft();
								// }

								if (permToAutonomy)
								{
									// w³¹czyæ jedn¹ autonomie! albo autonomy()
									// albo holdSafePositionAutonomy();

									// autonomy();

									// if (mService.isPermToGetDistance1And2())
									// {
									// // Log.i("isPermToGetDistance1And2",
									// // "wszedlem !!!");
									// holdSafePositionAutonomy();
									// }

									sensorDistanceRight = mService.getSensorDistanceRight();
									sensorDistanceLeft = mService.getSensorDistanceLeft();

									holdSafePositionAutonomy();
									updateViews();
								}
							}
						}
					}.start();
					break;
				}
				case R.id.stopBtn:
				{
					Log.i("OnClickListener", "stop !!!");
					setPermToAutonomy(false);
					drone.hovering();
					break;
				}

			}
		}
	};

	private void autonomy()
	{

		if (permToGoForward)
		{
			if (sensorDistanceFront > SAFE_DISTANCE)
			{
				permToHover = true;

				if (state != State.Forward)
				{
					Log.d("MainActivity", "Autonomy goForward");
					// drone.goForward();
					state = State.Forward;
				}
			}
		}

		if (permToHover)
		{
			if (sensorDistanceFront < SAFE_DISTANCE)
			{
				if (state != State.Hover)
				{
					Log.d("MainActivity", "Autonomy hovering");
					// drone.hovering();
					state = State.Hover;
				}
			}
		}

		if (sensorDistanceFront < 20)
		{
			if (state != State.Backward)
			{
				Log.d("MainActivity", "Autonomy goBackward");
				permToHover = false;
				// drone.goBackward();
				state = State.Backward;
			}
		}
	}

	private void holdSafePositionAutonomy()
	{
		holdSafeFrontPosition();

		//holdSafeRightPosition();
		//holdSafeLeftPosition();

		checkIfIsSafe();
	}

	private void holdSafeFrontPosition()
	{
		if (sensorDistanceFront < SAFE_DISTANCE)
		{
			if (state != State.Backward)
			{
				Log.d("MainActivity", "Autonomy goBackward");
				//drone.goBackward();
				state = State.Backward;
			}
		}
	}

	private void holdSafeRightPosition()
	{
		if (sensorDistanceRight < SAFE_DISTANCE)
		{
			if (state != State.Left)
			{
				Log.d("MainActivity", "Autonomy goLeft");
				//drone.goLeft();
				state = State.Left;
			}
		}
	}

	private void holdSafeLeftPosition()
	{
		if (sensorDistanceLeft < SAFE_DISTANCE)
		{
			if (state != State.Right)
			{
				Log.d("MainActivity", "Autonomy goRight");
				///drone.goRight();
				state = State.Right;
			}
		}
	}

	private void checkIfIsSafe()
	{
		if (state == State.Backward)
		{
			if (sensorDistanceFront > SAFE_DISTANCE)
			{
				Log.d("MainActivity", "Autonomy Hover");
				//drone.hovering();
				state = State.Hover;
			}
		}
		else if (state == State.Left)
		{
			if (sensorDistanceRight > SAFE_DISTANCE)
			{
				Log.d("MainActivity", "Autonomy Hover");
				//drone.hovering();
				state = State.Hover;
			}
		}
		else if (state == State.Right)
		{
			Log.d("MainActivity", "Autonomy Hover");
			if (sensorDistanceLeft > SAFE_DISTANCE)
			{
				//drone.hovering();
				state = State.Hover;
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		//mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		// startSensors();
	}

	@Override
	protected void onPause()
	{
		stopSensors();
		//drone.landing();
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		if (mBound)
		{
			unbindService(mConnection);
			mService.stopSelf();
			mBound = false;
		}
		super.onDestroy();

	}

	public void stopSensors()
	{
		if (mLocationManager != null)
		{
			mLocationManager.removeUpdates(this);
			mLocationManager = null;
		}

		if (mSensorManager != null)
		{
			mSensorManager.unregisterListener(this);
			mSensorManager = null;
		}
	}

	public void onLocationChanged(Location location)
	{
		// TODO Auto-generated method stub
		mCurrentLocation = location;

		// Log.d("Drone", "Height=" + mCurrentLocation.getAltitude());
	}

	public void onProviderDisabled(String provider)
	{
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider)
	{
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{

	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event)
	{
		mOrientation = event.values[0];

		switch (event.sensor.getType())
		{
			case Sensor.TYPE_ACCELEROMETER:
			{
				mAkcel = event.values.clone();
				break;
			}
		}

		if (mAkcel != null)
		{
			accTxt.setText("Akcelerometru: " + "\nx: " + mAkcel[0] + "\ny: " + mAkcel[1] + "\nz: " + mAkcel[2]);
		}

		controlByAccelerometer();
	}

	private void controlByAccelerometer()
	{
		if (permToControllByAcc)
		{
			if (state != State.Forward)
			{
				if (mAkcel[0] < -3)
				{
					Log.i("onCheckedChanged", "forward !!!");
					state = State.Forward;
					//drone.goForward();
				}
			}

			if (state != State.Backward)
			{
				if (mAkcel[0] > 3)
				{
					Log.i("onCheckedChanged", "backward !!!");
					state = State.Backward;
					//drone.goBackward();
				}
			}

			if (state != State.Left)
			{
				if (mAkcel[1] < -3)
				{
					Log.i("onCheckedChanged", "left !!!");
					state = State.Left;
					//drone.goLeft();
				}
			}

			if (state != State.Right)
			{
				if (mAkcel[1] > 3)
				{
					Log.i("onCheckedChanged", "right !!!");
					state = State.Right;
					//drone.goRight();
				}
			}

			if (state != State.Hover)
			{
				if (mAkcel[0] < 3 && mAkcel[0] > -3 && mAkcel[1] < 3 && mAkcel[1] > -3)
				{
					Log.i("onCheckedChanged", "hover !!!");
					state = State.Hover;
					drone.hovering();
				}
			}

		}
	}

	public boolean isPermToAutonomy()
	{
		return permToAutonomy;
	}

	public void setPermToAutonomy(boolean permToAutonomy)
	{
		this.permToAutonomy = permToAutonomy;
	}

	public void toast(final String message)
	{
		final Context context = this;
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void updateViews()
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				txtVFrontSensor.setText("Front: " + String.valueOf(sensorDistanceFront));

				//txtVRightSensor.setText("Right: " + String.valueOf(sensorDistanceRight));
				//txtVLeftSensor.setText("Left: " + String.valueOf(sensorDistanceLeft));

			}
		});
	}

	// public void startSensors()
	// {
	// if (mLocationManager == null)
	// {
	// mLocationManager = (LocationManager)
	// getSystemService(Context.LOCATION_SERVICE);
	// if (mLocationManager != null)
	// {
	// mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
	// 1000 /* minTime ms */, 1 /* minDistance in meters */, this);
	// }
	// }
	//
	// if (mSensorManager == null)
	// {
	// mSensorManager = (SensorManager)
	// getSystemService(Context.SENSOR_SERVICE);
	// if (mSensorManager != null)
	// {
	// mSensorManager.registerListener(this,
	// mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
	// SensorManager.SENSOR_DELAY_NORMAL);
	// }
	// }
	// }

	// private void showVersions2(String title)
	// {
	// toast(String.format("%s\n", title));
	// }

}

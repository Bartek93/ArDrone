package com.example.ardrone;

import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
import android.nfc.Tag;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
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
	private Button simpleAutBtn;

	private CheckBox accBox;
	private ToggleButton tg;

	private TextView accTxt;
	private TextView txtVFrontSensor;
	private TextView txtVRightSensor;
	private TextView txtVLeftSensor;
	private TextView txtAutonomyLog;

	private static final int SAFE_DISTANCE = 100; // in cm
	private static final int SAFE_DISTANCE_2 = SAFE_DISTANCE - 70; // in cm
	private static final int WRONG_RESULTS_1 = 0;
	private static final int WRONG_RESULTS_2 = 500;
	
	private static final String TAG = "MA"; // Main Activity
	// wysy³anie polecen za pomoc¹ manulanego sterowania jest mo¿liwe, to blokuje tylko wysylanie polecen przy akcelerometrze i autonomi	
	private static final boolean PERM_TO_SEND_COMMAND = false;	
	private static final boolean PERM_TO_GET_DISTANCE_L_AND_R = false; // prawego i lewego czujnika

	private ARDroneAPI drone;
	protected ARDroneIOIOService mService;
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	
	private Location mCurrentLocation;
	private Location mTargetLocation;
	private double mOrientation;	

	protected boolean mBound = false;
	private boolean mStarted = false;
	private boolean permToHover = true;
	private boolean permToControllByAcc = false;
	
	private boolean permToAutonomy = true;

	private int sensorDistanceFront;
	private int sensorDistanceRight;
	private int sensorDistanceLeft;

	private String autonomyLog = "";
	
	private float mAkcel[];
	private enum State
	{
		Default, Hover, Forward, Backward, Right, Left
	};
	private State state = State.Default;
	
	private final long startTime = 1000 * 5; // 5 sekund
	private final long interval = 500 * 1;	// 0.5 sekundy
	private MyCountDownTimer myCDTimer;
	
	private FileAccess fileAccess;
	
	private MyThread myThread;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.i(TAG, "onCreate");

		// to do wywalenia
		//==================================================
		mTargetLocation = new Location("target");
		mTargetLocation.setLatitude(1.0);
		mTargetLocation.setLongitude(1.0);
		mCurrentLocation = new Location("cr");
		mCurrentLocation.setLatitude(1.0);
		mCurrentLocation.setLongitude(1.0);
		//==================================================

		Intent intent = new Intent(this, ARDroneIOIOService.class);
		startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		initView();
		initListener();
		
		//myCDTimer = new MyCountDownTimer(startTime, interval);
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

			if (PERM_TO_GET_DISTANCE_L_AND_R)
			{
				sensorDistanceRight = mService.getSensorDistanceRight();
				sensorDistanceLeft = mService.getSensorDistanceLeft();
			}
			
			Log.i(TAG, "Service connected");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{			
			mBound = false;
			Log.i(TAG, "Service disconnected");
		}
	};
	
	private void initView()
	{
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
		txtAutonomyLog = (TextView) findViewById(R.id.txtAutonomyLog);
		accTxt = (TextView) findViewById(R.id.accTxt);
		simpleAutBtn = (Button) findViewById(R.id.simpleAutBtn);
	}

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
		
		simpleAutBtn.setOnClickListener(mClickListener);
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
							Log.i(TAG, "OnClickListener: Connect");
							try
							{
								drone = new ARDroneAPI();
							}
							catch (Exception e)
							{
								Log.i(TAG, "OnClickListener: Exception in Connect !!!");
								e.printStackTrace();
							}
						}
					}.start();
					break;
				}
				case R.id.disconnectBtn:
				{
					Log.i(TAG, "OnClickListener: disconnect !!!");
					drone.disbleEmergency();
					break;
				}
				case R.id.ftrimBtn:
				{
					Log.i(TAG, "OnClickListener: ftrim !!!");
					drone.trim();
					break;
				}
				case R.id.takeOffBtn:
				{
					Log.i(TAG, "OnClickListener: takeoff !!!");
					drone.takeoff();
					break;
				}
				case R.id.landingBtn:
				{
					Log.i(TAG, "OnClickListener: landing !!!");
					drone.landing();
					break;
				}
				case R.id.hoverBtn:
				{
					Log.i(TAG, "OnClickListener: hover !!!");
					drone.hovering();
					break;
				}
				case R.id.upBtn:
				{
					Log.i(TAG, "OnClickListener: up !!!");
					drone.up();
					break;
				}
				case R.id.downBtn:
				{
					Log.i(TAG, "OnClickListener: down !!!");
					drone.down();
					break;
				}
				case R.id.forwardBtn:
				{
					Log.i(TAG, "OnClickListener: forward !!!");
					drone.goForward();
					break;
				}
				case R.id.backwardBtn:
				{
					Log.i(TAG, "OnClickListener: backward !!!");
					drone.goBackward();
					break;
				}
				case R.id.leftBtn:
				{
					Log.i(TAG, "OnClickListener: left !!!");
					drone.goLeft();
					break;
				}
				case R.id.rotateLeftBtn:
				{
					Log.i(TAG, "OnClickListener: rotate left !!!");
					drone.rotatel();
					break;
				}
				case R.id.rightBtn:
				{
					Log.i(TAG, "OnClickListener: right !!!");
					drone.goRight();
					break;
				}
				case R.id.rotateRightBtn:
				{
					Log.i(TAG, "OnClickListener: rotate right !!!");
					drone.rotater();
					break;
				}
				case R.id.autonomyBtn:
				{
					Log.i(TAG, "OnClickListener: autonomy !!!");
					state = State.Default;
					setPermToAutonomy(true);
					
//					myThread = new MyThread();
//					myThread.start();

					new Thread()
					{
						@Override
						public void run()
						{
							while (true)
							{
								if (permToAutonomy)
								{
									sensorDistanceFront = mService.getSensorDistanceFront();
									
									// w³¹czyæ jedn¹ autonomie! albo autonomy() albo holdSafePositionAutonomy();
										
									autonomy();

									if (PERM_TO_GET_DISTANCE_L_AND_R)
									{
										sensorDistanceRight = mService.getSensorDistanceRight();
										sensorDistanceLeft = mService.getSensorDistanceLeft();
										//holdSafePositionAutonomy();		
									}																
								}								
								updateViews();
							}
						}
					}.start();
					
					break;
				}
				case R.id.stopBtn:
				{
					Log.i(TAG, "OnClickListener: stop !!!");
					setPermToAutonomy(false);
					//drone.hovering();
					hover();					
					state = State.Default;
					break;
				}
				case R.id.simpleAutBtn:
				{
					Log.i(TAG, "OnClickListener: simpleAutBtn !!!");

					//simpleAutonomy();
					
					if(fileAccess == null)
					{
						createNewLogFile();
					}
					
					break;
				}

			}
		}
	};
	
	//===============================================================================================================================
	// Pe³na autonomia
	// Dzia³anie: £¹czenie siê, trymowanie, wznoszenie, ok 8 sekund utrzymania siê w powietrzu, l¹dowanie, roz³¹czenie
	private void simpleAutonomy()
	{
		// connect
		new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					//drone = new ARDroneAPI();
					Log.i(TAG, "simpleAutonomy(): connect !!!");
					
//					while(true)
//					{
//						Log.i("aaa", "bbb");
//						Thread.sleep(1000);
//					}
				}
				catch (Exception e)
				{
					Log.i(TAG, "OnClickListener: Exception in Connect !!!");
					e.printStackTrace();
				}
			}
		}.start();
		
		
		
		Log.i(TAG, "simpleAutonomy(): przed new timerTask !!!");
		TimerTask timerTask = new MyTimerTask();
		Log.i(TAG, "simpleAutonomy(): przed timerTask.run() !!!");
		timerTask.run();
		Log.i(TAG, "simpleAutonomy(): po timerTask.run() !!!");
		
//		//trim
//		drone.trim();
//		
//		
//		//takeoff
//		drone.takeoff();
//		
//		
//		//5s
//		
//		
//		
//		//land
//		drone.landing();
//		
//		
//		//disconnect
//		drone.disbleEmergency();
		
	}
	//===============================================================================================================================
	
	
	
	//===============================================================================================================================
	// Niepe³na autonomia tzn trzeba najpierw po³¹czyæ siê, trymowaæ, wznieœæ i dopiero autonomia przy pomocy czujników
	// Dzia³anie: 1 czujnik. Dron leci do przodu dopóki nie napotka przeszkody w odleglosci SAFE_DISTANCE, wtedy siê zatrzyma.
	// 			  Jeœli przeszkoda przybli¿y siê dron odsunie siê na odleg³oœæ SAFE_DISTANCE

	private void autonomy()
	{
		if(sensorDistanceFront == WRONG_RESULTS_1 || sensorDistanceFront > WRONG_RESULTS_2)
		{
			Log.i(TAG, "autonomy(): wrong results");
			autonomyLog = "wrong results";
			hover();
			state = State.Hover;
		}
		else
		{
			if (sensorDistanceFront > SAFE_DISTANCE)
			{
				permToHover = true;

				if (state != State.Forward)
				{
					if(state == State.Backward)
					{
						Log.d(TAG, "autonomy(): hovering");
						hover();
						autonomyLog = "hover";
						state = State.Hover;
					}
					else
					{
						Log.d(TAG, "autonomy(): goForward");	
						goForward();
						autonomyLog = "goForward";
						state = State.Forward;
					}
					
//					Log.d(TAG, "autonomy(): goForward");
//					// drone.goForward();					
//					goForward();
//					autonomyLog = "goForward";
//					state = State.Forward;
				}
			}

			if (permToHover)
			{
				if (sensorDistanceFront < SAFE_DISTANCE)
				{
					if (state != State.Hover)
					{
						Log.d(TAG, "autonomy(): hovering");
						// drone.hovering();
						hover();
						autonomyLog = "hover";
						state = State.Hover;
					}
				}
			}

			if (sensorDistanceFront < SAFE_DISTANCE_2)
			{
				if (state != State.Backward)
				{
					Log.d(TAG, "autonomy(): goBackward");
					permToHover = false;
					// drone.goBackward();
					goBackward();
					autonomyLog = "goBackward";
					state = State.Backward;
				}
			}
		}
	}
	//===============================================================================================================================
	
	
	
	//===============================================================================================================================
	// Niepe³na autonomia tzn trzeba najpierw po³¹czyæ siê, trymowaæ, wznieœæ i dopiero autonomia przy pomocy czujników
	// Dzia³anie: 3 czujniki. Dron utrzymuje siê w odleg³oœci co najmniej SAFE_DISTANCE od przodu, prawej i lewej strony

	private void holdSafePositionAutonomy()
	{
		if(sensorDistanceFront == WRONG_RESULTS_1 || sensorDistanceFront > WRONG_RESULTS_2)
		{
			Log.i(TAG, "autonomy(): wrong results");
			hover();
			state = State.Hover;
			Log.i(TAG, "holdSafePositionAutonomy: hover()");
		}
		else
		{
			holdSafeFrontPosition();
			holdSafeRightPosition();
			holdSafeLeftPosition();

			checkIfIsSafe();
		}
	}

	private void holdSafeFrontPosition()
	{
		if (sensorDistanceFront < SAFE_DISTANCE)
		{
			if (state != State.Backward)
			{
				Log.d(TAG, "holdSafeFrontPosition(): goBackward");
				//drone.goBackward();
				goBackward();
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
				Log.d(TAG, "holdSafeRightPosition(): goLeft");
				//drone.goLeft();
				goLeft();
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
				Log.d(TAG, "holdSafeLeftPosition(): goRight");
				///drone.goRight();
				goRight();
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
				Log.d(TAG, "checkIfIsSafe(): Hover");
				//drone.hovering();
				hover();
				state = State.Hover;
			}
		}
		else if (state == State.Left)
		{
			if (sensorDistanceRight > SAFE_DISTANCE)
			{
				Log.d(TAG, "checkIfIsSafe(): Hover");
				//drone.hovering();
				hover();
				state = State.Hover;
			}
		}
		else if (state == State.Right)
		{
			Log.d(TAG, "checkIfIsSafe(): Hover");
			if (sensorDistanceLeft > SAFE_DISTANCE)
			{
				//drone.hovering();
				hover();
				state = State.Hover;
			}
		}
	}

	//===============================================================================================================================
	

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
				txtAutonomyLog.setText("Autonomy Log: " + autonomyLog);

				if (PERM_TO_GET_DISTANCE_L_AND_R)
				{
					txtVRightSensor.setText("Right: " + String.valueOf(sensorDistanceRight));
					txtVLeftSensor.setText("Left: " + String.valueOf(sensorDistanceLeft));	
				}
			}
		});
	}
	
	
	private void hover()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{				
				drone.hovering();
				Log.i(TAG, "hovering()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt niestworzony");
			}
		}
	}
	
	private void goForward()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.goForward();
				Log.i(TAG, "goForward()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta³ stworzony");
				autonomyLog = "obiekt drone=NULL";
			}
		}
	}
	
	private void goBackward()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.goBackward();
				Log.i(TAG, "goBackward()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta³ stworzony");
			}
		}
	}
	
	private void goRight()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.goRight();
				Log.i(TAG, "goRight()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta³ stworzony");
			}
		}
	}
	
	private void goLeft()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.goLeft();
				Log.i(TAG, "goLeft()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta³ stworzony");
			}
		}
	}
	
	private void land()
	{
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.landing();
				Log.i(TAG, "land()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta³ stworzony");
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
		land();
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
	
	public void setPermToAutonomy(boolean permToAutonomy)
	{
		this.permToAutonomy = permToAutonomy;
	}
	
	
	// Do wywalenia
	//===================================================================================
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
	//===================================================================================
	
	
	
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
	
	private class MyCountDownTimer extends CountDownTimer
	{

		public MyCountDownTimer(long millisInFuture, long countDownInterval)
		{
			super(millisInFuture, countDownInterval);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onFinish()
		{
			// TODO Auto-generated method stub
			Log.i("MyCountDownTimer", "Czas dobieg³ koñca");
		}

		@Override
		public void onTick(long millisUntilFinished)
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private class MyTimerTask extends TimerTask
	{

		@Override
		public void run()
		{
			wait3Seconds();
			
			Log.i("MyTimerTask", "trim");	
			wait3Seconds();
			
			Log.i("MyTimerTask", "takeoff");
			wait3Seconds();
			
			Log.i("MyTimerTask", "flying");
			wait8Seconds();
			
			Log.i("MyTimerTask", "land");
			wait3Seconds();
			
			Log.i("MyTimerTask", "disconnect");
			wait3Seconds();
			
		}
		
		private void wait3Seconds()
		{
			try
			{
				Thread.sleep(3000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		private void wait8Seconds()
		{
			try
			{
				Thread.sleep(8000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}		
	}
	
	public class MyThread extends Thread
	{
		@Override
		public void run()
		{
				Log.i(TAG, "MyThread: run()");
			
//			try
//			{
//				while (!Thread.currentThread().isInterrupted())
//				{
//					// ...
//				}
//			}
//			catch (InterruptedException consumed)
//			{
//				/* Allow thread to exit */
//				consumed.printStackTrace();
//			}
			
//			while (true)
//			{
//				if (permToAutonomy)
//				{
//					sensorDistanceFront = mService.getSensorDistanceFront();
//					
//					// w³¹czyæ jedn¹ autonomie! albo autonomy() albo holdSafePositionAutonomy();
//						
//					autonomy();
//
//					if (PERM_TO_GET_DISTANCE_L_AND_R)
//					{
//						sensorDistanceRight = mService.getSensorDistanceRight();
//						sensorDistanceLeft = mService.getSensorDistanceLeft();
//						//holdSafePositionAutonomy();		
//					}																
//				}								
//				updateViews();
//			}			
		}

	}
	
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss", Locale.getDefault());
    
    private void createNewLogFile()
    {
        //File folder = new File(Environment.getExternalStorageDirectory() + "/ArDroneLogs");

        String fileName = "/ArDroneLogs/ArDroneLogs_" + formatter.format(new Date()) + ".txt";
        fileAccess = new FileAccess(fileName);
        writeFirstLine();
    }
    
    private void writeFirstLine()
    {
    	String linia = "Czesc, to moj trzeci zapis do pliku";
    	fileAccess.append(linia);
    	
    	fileAccess.closeFile();
    }
    

}

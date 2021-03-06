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
	private Button calibBtn;
	private Button readBtn;
	private Button magLeftBtn;
	private Button magRightBtn;

	private CheckBox accBox;
	private ToggleButton tg;

	private TextView accTxt;
	private TextView txtVFrontSensor;
	private TextView txtVRightSensor;
	private TextView txtVLeftSensor;
	private TextView txtAutonomyLog;
	private TextView txtYawAngle;
	private TextView txtOldAngle;
	private TextView txtNewAngle;

//	private static final int SAFE_DISTANCE = 64; // in cm
	private static final int SAFE_DISTANCE = 70; // in cm
	private static final int SAFE_DISTANCE_2 = 30; // in cm
	private static final int WRONG_RESULTS_1 = 0;
	private static final int WRONG_RESULTS_2 = 500;
	
	private static final String TAG = "MA"; // Main Activity
	// wysy�anie polecen za pomoc� manulanego sterowania jest mo�liwe, to blokuje tylko wysylanie polecen przy akcelerometrze i autonomi	
	private static final boolean PERM_TO_SEND_COMMAND = true;	
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

	private int sensorDistanceFront = 0;
	private int sensorDistanceRight = 0;
	private int sensorDistanceLeft = 0;
	
	private float yawAngle = 0;
	private float oldYawAngle = 0;
	private float newYawAngle = 0;
	
	private float dopelnienie;

	private String autonomyLog = "";
	
	private float mAkcel[];
	private enum State
	{
		Default, Hover, Forward, Backward, Right, Left, Magneto_Rotate_Right, Magneto_Rotate_Left
	};
	private State state = State.Default;
	
	private enum Rotate
	{
		Default, Rotate_Right, Rotate_Left
	};
	private Rotate rotate = Rotate.Default;
	
	
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
		calibBtn = (Button) findViewById(R.id.calibBtn);
		readBtn = (Button) findViewById(R.id.readBtn);
		magLeftBtn = (Button) findViewById(R.id.magLeftBtn);
		magRightBtn = (Button) findViewById(R.id.magRightBtn);
		txtYawAngle = (TextView) findViewById(R.id.txtYawAngle);
		txtOldAngle= (TextView) findViewById(R.id.txtOldAngle);
		txtNewAngle = (TextView) findViewById(R.id.txtNewAngle);
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
		calibBtn.setOnClickListener(mClickListener);
		readBtn.setOnClickListener(mClickListener);
		magLeftBtn.setOnClickListener(mClickListener);
		magRightBtn.setOnClickListener(mClickListener);
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
				case R.id.readBtn:
				{
					new Thread()
					{
						@Override
						public void run()
						{
							Log.i(TAG, "OnClickListener: readBtn");

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
					
					//if(PERM_TO_SEND_COMMAND)
					oldYawAngle = drone.getArdrone().getYawAngle();
					updateViews();
					
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
					
//					new Thread()
//					{
//						@Override
//						public void run()
//						{
//							while (true)
//							{
//								if (permToAutonomy)
//								{
//									sensorDistanceFront = mService.getSensorDistanceFront();														
//								}	
//							}
//						}
//					}.start();

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
									
									//if(PERM_TO_SEND_COMMAND)
										yawAngle = drone.getArdrone().getYawAngle();
									
									// w��czy� jedn� autonomie! albo autonomy() albo holdSafePositionAutonomy(); albo simpleAutonomy()										
									autonomy();
									//holdSafePositionAutonomy();
									
									updateViews();		
								}								
								
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
					
					//drone.magnetoRotateLeft();
					
					break;
				}
				case R.id.simpleAutBtn:
				{
					Log.i(TAG, "OnClickListener: simpleAutBtn !!!");

					//simpleAutonomy();
					
//					if(fileAccess == null)
//					{
//						createNewLogFile();
//					}
					//drone.magnetoSetNorth();
							
					break;
				}
				case R.id.calibBtn:
				{
					drone.calibrateMagnetometer();
					break;
				}
				case R.id.magLeftBtn:
				{
					magnetoRotateLeft();	
					break;
				}
				case R.id.magRightBtn:
				{
					magnetoRotateRight();
					break;
				}
			}
		}
	};
	
	//===============================================================================================================================
	// Pe�na autonomia
	// Dzia�anie: ��czenie si�, trymowanie, wznoszenie, ok 8 sekund utrzymania si� w powietrzu, l�dowanie, roz��czenie
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
	// Niepe�na autonomia tzn trzeba najpierw po��czy� si�, trymowa�, wznie�� i dopiero autonomia przy pomocy czujnika.
	// Dzia�anie: 1 czujnik. Random Walk: Dron leci do przodu dop�ki nie napotka przeszkody w odleglosci SAFE_DISTANCE, wtedy si� zatrzyma,
	//			  po czym szuka miejsca w kt�re mo�e lecie� (skr�caj�c w prawo).
	// 			  Je�li przeszkoda przybli�y si� ponizej odleglosci SAFE_DISTANCE_2 dron odsunie si� na odleg�o�� SAFE_DISTANCE

	private void autonomy()
	{
		if (sensorDistanceFront == WRONG_RESULTS_1)
		{
			autonomyLog = "wrong results";
			//hover();
		}
		else
		{
			//if (mService.getSensorDistanceFront() > SAFE_DISTANCE)
			if (sensorDistanceFront > SAFE_DISTANCE)
			{
				permToHover = true;

				if (state != State.Forward)
				{
					goForward();
//					if (state == State.Backward)
//					{
//						hover();
//					}
//					else
//					{						
//						goForward();
//					}
				}
				
				//sensorDistanceFront = mService.getSensorDistanceFront();
			}

			// Obr�t w prawo, wy��czaj�c tego if-a, dron b�dzie lata� tylko do przodu i do ty�u
			if (state == State.Hover)
			{	
//				TimerTask timerTask = new MyTimerTask();
//				timerTask.run();
//				timerTask.cancel();
				
				
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				// je�li rozpoznanie terenu tylko z frontu
				//checkAndSetHorizon();		
				// je�li obrot tylko w prawo
				magnetoRotateRight(); 
				
				updateViews();
				
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				hover();	
			}
			
			// musi by� permToHover bo wtedy przy cofaniu pomi�dzy SAFE_DISTANCE a SAFE_DISTANCE_2 wchodzi�oby zar�wno do SAFE_DISTANCE i zatrzyma�by si� jeszcze przed doleglosci� SAFE_DISTANCE 
			if (permToHover)
			{
				if (mService.getSensorDistanceFront() < SAFE_DISTANCE)
				//if (sensorDistanceFront < SAFE_DISTANCE)					
				{
					if (state != State.Hover)
					{						
						hover();
						
						try
						{
							Thread.sleep(500);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			}

			if (mService.getSensorDistanceFront() < SAFE_DISTANCE_2)
			//if (sensorDistanceFront < SAFE_DISTANCE_2)
			{
				if (state != State.Backward)
				{					
					permToHover = false;
					goBackward();
				}
			}
		}
	}
	//===============================================================================================================================
	
	
	
	//===============================================================================================================================
	// Niepe�na autonomia tzn trzeba najpierw po��czy� si�, trymowa�, wznie�� i dopiero autonomia przy pomocy czujnik�w
	// Dzia�anie: 3 czujniki. Dron utrzymuje si� w odleg�o�ci co najmniej SAFE_DISTANCE od przodu, prawej i lewej strony

	private void holdSafePositionAutonomy()
	{

		//if (sensorDistanceFront == WRONG_RESULTS_1 || sensorDistanceRight == WRONG_RESULTS_1)
		if (mService.getSensorDistanceFront() == WRONG_RESULTS_1)
		{
			autonomyLog = "wrong results";
			//hover();
		}
		else
		{
			holdSafeFrontPosition();
			holdSafeRightPosition();
			holdSafeLeftPosition();
			
//			sensorDistanceFront = mService.getSensorDistanceFront();
//			sensorDistanceRight = mService.getSensorDistanceRight();
//			sensorDistanceLeft = mService.getSensorDistanceLeft();
//			
//			if (sensorDistanceFront < SAFE_DISTANCE)
//			{
//				if (state != State.Backward)
//				{
//					Log.d(TAG, "holdSafeFrontPosition(): goBackward");
//					//drone.goBackward();
//					goBackward();
//				}
//			}
//			else if (sensorDistanceRight < SAFE_DISTANCE)
//			{
//				if (state != State.Left)
//				{
//					Log.d(TAG, "holdSafeRightPosition(): goLeft");
//					goLeft();
//				}
//			}
//			else if (sensorDistanceLeft < SAFE_DISTANCE)
//			{
//				if (state != State.Right)
//				{
//					Log.d(TAG, "holdSafeLeftPosition(): goRight");
//					goRight();
//				}
//			}
			

			checkIfIsSafe();
			try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void holdSafeFrontPosition()
	{
		sensorDistanceFront = mService.getSensorDistanceFront();
		if (sensorDistanceFront < SAFE_DISTANCE)
		{
			if (state != State.Backward)
			{
				Log.d(TAG, "holdSafeFrontPosition(): goBackward");
				//drone.goBackward();
				goBackward();
			}
		}
	}

	private void holdSafeRightPosition()
	{
		
//		if (sensorDistanceRight < SAFE_DISTANCE)
		sensorDistanceRight = mService.getSensorDistanceRight();
		if (sensorDistanceRight < SAFE_DISTANCE)
		{
			if (state != State.Left)
			{
				Log.d(TAG, "holdSafeRightPosition(): goLeft");
				goLeft();
			}
		}
	}

	private void holdSafeLeftPosition()
	{
		sensorDistanceLeft = mService.getSensorDistanceLeft();
//		if (sensorDistanceLeft < SAFE_DISTANCE)
		if (sensorDistanceLeft < SAFE_DISTANCE)
		{
			if (state != State.Right)
			{
				Log.d(TAG, "holdSafeLeftPosition(): goRight");
				///drone.goRight();
				goRight();
			}
		}
	}

	private void checkIfIsSafe()
	{
//		sensorDistanceFront = mService.getSensorDistanceFront();
//		sensorDistanceRight = mService.getSensorDistanceRight();
//		sensorDistanceLeft = mService.getSensorDistanceLeft();
//		
//		if (sensorDistanceFront > SAFE_DISTANCE)
//		{				
//			hover();
//		}
//		else if (sensorDistanceRight > SAFE_DISTANCE)
//		{
//			hover();
//		}
//		else if (sensorDistanceLeft > SAFE_DISTANCE)
//		{
//			hover();
//		}
		
		if (state == State.Backward)
		{
			sensorDistanceFront = mService.getSensorDistanceFront();
			if (sensorDistanceFront > SAFE_DISTANCE)
			{				
				Log.d(TAG, "checkIfIsSafe(): Hover");
				hover();
			}
		}
		else if (state == State.Left)
		{
			sensorDistanceRight = mService.getSensorDistanceRight();
			if (sensorDistanceRight > SAFE_DISTANCE)
			{
				Log.d(TAG, "checkIfIsSafe(): Hover");
				hover();
			}
		}
		else if (state == State.Right)
		{
			sensorDistanceLeft = mService.getSensorDistanceLeft();
			Log.d(TAG, "checkIfIsSafe(): Hover");
			if (sensorDistanceLeft > SAFE_DISTANCE)
			{
				hover();
			}
		}
	}

	//===============================================================================================================================
	

	// Do akcelerometru
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
			accTxt.setText("Akcelerometr: " + "\nx: " + mAkcel[0] + "\ny: " + mAkcel[1] + "\nz: " + mAkcel[2]);
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
				//txtVFrontSensor.setText("Front: " + String.valueOf(mService.getSensorDistanceFront()));
				txtVFrontSensor.setText("Front: " + String.valueOf(sensorDistanceFront));
				txtAutonomyLog.setText("Autonomy Log: " + autonomyLog);
				txtYawAngle.setText("Angle: " + yawAngle);
				txtOldAngle.setText("Old Angle: " + oldYawAngle);
				txtNewAngle.setText("New Angle: " + newYawAngle);

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
		Log.d(TAG, "autonomy(): hover");
		
		autonomyLog = "hover";
		state = State.Hover;
		
		//if(PERM_TO_SEND_COMMAND)
			newYawAngle = drone.getArdrone().getYawAngle();
		
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
		Log.d(TAG, "autonomy(): goForward");
		
		autonomyLog = "goForward";
		state = State.Forward;
		
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
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
				autonomyLog = "obiekt drone=NULL";
			}
		}
	}
	
	private void goBackward()
	{
		Log.d(TAG, "autonomy(): goBackward");
		
		autonomyLog = "goBackward";
		state = State.Backward;
		
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
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void goRight()
	{
		autonomyLog = "goRight";
		state = State.Right;
		
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
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void goLeft()
	{
		autonomyLog = "goLeft";
		state = State.Left;
		
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
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
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
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void magnetoRotateRight()
	{
		Log.d(TAG, "autonomy(): magnetoRotateRight");
		
		autonomyLog = "RotateRight";
		state = State.Magneto_Rotate_Right;
		
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
//				if(drone.getMagneto_psi() == (float) 0)
//				{
//					drone.setMagneto_psi(drone.getMagneto_psi() + (float) 0.1);
//					drone.magnetoRotateRight();					
//				}
				
				drone.magnetoRotateRight();	
//				drone.getArdrone().setMagneto_psi(drone.getArdrone().getMagneto_psi() + (float) 0.1);			
//				if(drone.getArdrone().getMagneto_psi() == (float) 1.0)
//				{
//					autonomyLog = "osiagnieto 1.0";
//					drone.getArdrone().setMagneto_psi(0);
//				}
				Log.i(TAG, "magnetoRotateRight()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void magnetoRotateLeft()
	{
		Log.d(TAG, "autonomy(): magnetoRotateLeft");
		
		autonomyLog = "RotateLeft";
		state = State.Magneto_Rotate_Left;
		
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				//drone.rotatel();
				drone.magnetoRotateLeft();
				//drone.getArdrone().setMagneto_psi(drone.getArdrone().getMagneto_psi() - (float) 0.1);
								
				Log.i(TAG, "magnetoRotateLeft()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void rotateRight()
	{
		Log.d(TAG, "autonomy(): RotateRight");
		
		autonomyLog = "RotateRight";
		state = State.Magneto_Rotate_Right;
		
		if(PERM_TO_SEND_COMMAND)
		{
			Log.i(TAG, "PERM_TO_SEND_COMMAND = true");
			if(drone != null)
			{
				drone.rotater();
				
				Log.i(TAG, "RotateRight()");
			}
			else
			{
				Log.i(TAG, "drone=NULL, obiekt nie zosta� stworzony");
			}
		}
	}
	
	private void checkAndSetHorizon()
	{
		if(oldYawAngle > 0 && oldYawAngle < 90)
		{
			if(rotate == Rotate.Default)
			{
				rotate = Rotate.Rotate_Right;
				magnetoRotateRight();
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				if(newYawAngle > 0 && newYawAngle - oldYawAngle < 90)
				{					
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
				else
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(oldYawAngle  - newYawAngle < 90)
				{					
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else
				{
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
			}	
		}
		else if (oldYawAngle > 90 && oldYawAngle < 180)
		{
			if(rotate == Rotate.Default)
			{
				magnetoRotateRight();
				rotate = Rotate.Rotate_Right;
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				dopelnienie = 180 - oldYawAngle;
				
				if(newYawAngle > 0)
				{ 					
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
				else if(newYawAngle < 0 && ((newYawAngle + 180) + dopelnienie) < 90)
				{
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
				else
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(newYawAngle < 0)
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else if(newYawAngle > 0 && oldYawAngle - newYawAngle < 90)
				{					
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else
				{
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
			}
		}
		// tu jest jakis blad
		else if (oldYawAngle > -90 && oldYawAngle < 0)
		{
			dopelnienie = Math.abs(oldYawAngle);
			
			if(rotate == Rotate.Default)
			{
				rotate = Rotate.Rotate_Right;
				magnetoRotateRight();
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				if(dopelnienie + newYawAngle < 90)
				{ 					
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
				else
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(newYawAngle < 0 && oldYawAngle  - newYawAngle < 90)
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else if(newYawAngle > 0 )
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else
				{
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
			}
		}
		else if (oldYawAngle < -90 && oldYawAngle > -180)
		{
			if(rotate == Rotate.Default)
			{
				rotate = Rotate.Rotate_Right;
				magnetoRotateRight();
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				dopelnienie = Math.abs(oldYawAngle);
				
				if(dopelnienie + newYawAngle < 90)
				{ 					
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
				else
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				dopelnienie = 180 + oldYawAngle;
				
				if(newYawAngle < 0 && oldYawAngle  - newYawAngle < 90)
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else if(newYawAngle > 0 && (180 - newYawAngle) + dopelnienie < 90)
				{
					rotate = Rotate.Rotate_Left;
					magnetoRotateLeft();
				}
				else
				{
					rotate = Rotate.Rotate_Right;
					magnetoRotateRight();
				}
			}
		}		
	}
	
	private void checkHorizont()
	{
		float oldAngle;
		float newAngle;
		float dopelnienie;
		
		if(oldYawAngle < 0)
		{
			oldAngle = 360f + oldYawAngle;
			newAngle = 360f + newYawAngle;
		}
		else
		{
			oldAngle = oldYawAngle;
			newAngle = newYawAngle;
		}		

		if(oldAngle > 0 && oldAngle < 90)
		{
			if(rotate == Rotate.Default)
			{
				magnetoRotateRight();
				rotate = Rotate.Rotate_Right;
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				if(newAngle - oldAngle < 90)
				{
					magnetoRotateRight();
					rotate = Rotate.Rotate_Right;
				}
				else
				{
					rotate = Rotate.Rotate_Left;
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(oldAngle  - newAngle < 90)
				{
					magnetoRotateLeft();
					rotate = Rotate.Rotate_Left;
				}
				else
				{
					rotate = Rotate.Rotate_Right;
				}
			}	
		}
		else if (oldAngle > 90 && oldAngle < 180)
		{
			if(rotate == Rotate.Default)
			{
				magnetoRotateRight();
				rotate = Rotate.Rotate_Right;
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				dopelnienie = 180 - oldYawAngle;
				
				if(newYawAngle < 0 && (180 - Math.abs(newYawAngle) + dopelnienie < 90 ))
				{ 
					magnetoRotateRight();
					rotate = Rotate.Rotate_Right;
				}
				else
				{
					rotate = Rotate.Rotate_Left;
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(oldYawAngle  - newYawAngle < 90)
				{
					magnetoRotateLeft();
					rotate = Rotate.Rotate_Left;
				}
				else
				{
					rotate = Rotate.Rotate_Right;
				}
			}
		}
		else if (oldAngle > -90 && oldAngle < 0)
		{
			dopelnienie = Math.abs(oldYawAngle);
			
			if(rotate == Rotate.Default)
			{
				magnetoRotateRight();
				rotate = Rotate.Rotate_Right;
			}
			else if(rotate == Rotate.Rotate_Right)
			{
				if(dopelnienie + newYawAngle < 90)
				{ 
					magnetoRotateRight();
					rotate = Rotate.Rotate_Right;
				}
				else
				{
					rotate = Rotate.Rotate_Left;
				}
			}
			else if(rotate == Rotate.Rotate_Left)
			{
				if(oldYawAngle  - newYawAngle < 90)
				{
					magnetoRotateLeft();
					rotate = Rotate.Rotate_Left;
				}
				else
				{
					rotate = Rotate.Rotate_Right;
				}
			}
		}
		else if (oldAngle > -180 && oldAngle < -90)
		{
			
		}		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		// Do akcelerometru
		//mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		// startSensors();
	}

	@Override
	protected void onPause()
	{
		stopSensors();
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
			Log.i("MyCountDownTimer", "Czas dobieg� ko�ca");
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
			wait2Seconds();
			magnetoRotateRight();	
			wait2Seconds();
			hover();
//			Log.i("MyTimerTask", "trim");	
//			wait3Seconds();
//			
//			Log.i("MyTimerTask", "takeoff");
//			wait3Seconds();
//			
//			Log.i("MyTimerTask", "flying");
//			wait8Seconds();
//			
//			Log.i("MyTimerTask", "land");
//			wait3Seconds();
//			
//			Log.i("MyTimerTask", "disconnect");
//			wait3Seconds();
			
		}
		
		private void wait1Second()
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		private void wait2Seconds()
		{
			try
			{
				Thread.sleep(2000);
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
//					// w��czy� jedn� autonomie! albo autonomy() albo holdSafePositionAutonomy();
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

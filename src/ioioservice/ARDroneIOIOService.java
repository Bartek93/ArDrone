package ioioservice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class ARDroneIOIOService extends IOIOService
{
	private final IBinder mBinder = new LocalBinder();
	
	private int sensorDistanceFront;
	private int sensorDistanceRight;
	private int sensorDistanceLeft;
	
	private boolean isPermToGetDistance1And2 = false;

	public class LocalBinder extends Binder
	{
		public ARDroneIOIOService getService()
		{
			return ARDroneIOIOService.this;
		}
	}

	public IOIOLooper getIOIOLooper()
	{
		return this.createIOIOLooper();
	}

	public ARDroneIOIOService()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	protected IOIOLooper createIOIOLooper()
	{
		return new BaseIOIOLooper()
		{
			private DigitalOutput led_;

			private DigitalOutput triggerPin_1;
			private PulseInput echoPin_1;
			private int echoSecondsSensorFront;
			private int echoDistanceCmSensorFront;
			
			private DigitalOutput triggerPin_2;
			private PulseInput echoPin_2;
			private int echoSecondsSensorRight;
			private int echoDistanceCmSensorRight;
			
			private DigitalOutput triggerPin_3;
			private PulseInput echoPin_3;
			private int echoSecondsSensorLeft;
			private int echoDistanceCmSensorLeft;
			

			@Override
			protected void setup() throws ConnectionLostException, InterruptedException
			{
				//showVersions2("IOIO connected!");
				
				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);

				echoPin_1 = ioio_.openPulseInput(new DigitalInput.Spec(9), PulseInput.ClockRate.RATE_250KHz, PulseInput.PulseMode.POSITIVE, false);
				triggerPin_1 = ioio_.openDigitalOutput(10);				
				
				if(isPermToGetDistance1And2)
				{					
					echoPin_2 = ioio_.openPulseInput(new DigitalInput.Spec(12), PulseInput.ClockRate.RATE_250KHz, PulseInput.PulseMode.POSITIVE, false);
					triggerPin_2 = ioio_.openDigitalOutput(13);
					
					
					echoPin_3 = ioio_.openPulseInput(new DigitalInput.Spec(15), PulseInput.ClockRate.RATE_250KHz, PulseInput.PulseMode.POSITIVE, false);
					triggerPin_3 = ioio_.openDigitalOutput(16);
				}								
			}

			@Override
			public void loop() throws ConnectionLostException, InterruptedException
			{
				led_.write(true);
				distances();
			}

			public void distances() throws ConnectionLostException, InterruptedException
			{
				triggerPin_1.write(false);
				Thread.sleep(5);
				triggerPin_1.write(true);
				Thread.sleep(1);
				triggerPin_1.write(false);

				echoSecondsSensorFront = (int) (echoPin_1.getDuration() * 1000 * 1000);
				echoDistanceCmSensorFront = echoSecondsSensorFront / 58;
				sensorDistanceFront = echoDistanceCmSensorFront;
//				Log.d("IOIOSensor", "Odleglosc sensor1:" + echoDistanceCmSensor1);
				
				if(isPermToGetDistance1And2)
				{
					triggerPin_2.write(false);
					Thread.sleep(5);
					triggerPin_2.write(true);
					Thread.sleep(1);
					triggerPin_2.write(false);

					echoSecondsSensorRight = (int) (echoPin_2.getDuration() * 1000 * 1000);
					echoDistanceCmSensorRight = echoSecondsSensorRight / 58;
					sensorDistanceRight = echoDistanceCmSensorRight;
					
					triggerPin_3.write(false);
					Thread.sleep(5);
					triggerPin_3.write(true);
					Thread.sleep(1);
					triggerPin_3.write(false);

					echoSecondsSensorLeft = (int) (echoPin_3.getDuration() * 1000 * 1000);
					echoDistanceCmSensorLeft = echoSecondsSensorLeft / 58;
					sensorDistanceLeft = echoDistanceCmSensorLeft;
				}

				Thread.sleep(20);
			}
		};
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		
		sensorDistanceFront = 0;
		sensorDistanceRight = 0;
		sensorDistanceLeft = 0;
		
		Log.d("Service", "Start service");
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (intent != null && intent.getAction() != null && intent.getAction().equals("stop"))
		{
			Log.d("Notification", "Notification Clicked");
			// User clicked the notification. Need to stop the service.
			nm.cancel(0);
			stopSelf();
		}
		else
		{
			// Service starting. Create a notification.
			Log.d("Notification", "Notification 1");
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
					// .setSmallIcon(R.drawable)
					.setContentTitle("IOIOService").setContentText("Click to stop");
			mBuilder.build();
			mBuilder.setContentIntent(PendingIntent.getService(this, 0, new Intent("stop", null, this, this.getClass()), 0));
			nm.notify();
			Log.d("Notification", "Notification 2");
			/*
			 * @SuppressWarnings("deprecation") Notification notification = new
			 * Notification(
			 * 
			 * R.drawable.ic_launcher, "IOIO service running",
			 * System.currentTimeMillis());
			 * 
			 * notification .setLatestEventInfo(this, "IOIO Service",
			 * "Click to stop", PendingIntent.getService(this, 0, new Intent(
			 * "stop", null, this, this.getClass()), 0)); notification.flags |=
			 * Notification.FLAG_ONGOING_EVENT; nm.notify(0, notification);
			 */
		}
	}
    
	public int getSensorDistanceFront()
	{
		return sensorDistanceFront;
	}

	public int getSensorDistanceRight()
	{
		return sensorDistanceRight;
	}

	public int getSensorDistanceLeft()
	{
		return sensorDistanceLeft;
	}





	public boolean isPermToGetDistance1And2()
	{
		return isPermToGetDistance1And2;
	}
}

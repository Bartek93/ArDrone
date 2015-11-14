package ioioservice;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
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

	private Handler mHandler = new Handler();
	private final IBinder mBinder = new LocalBinder();
	private int sensorDistance1;

	public int getsensorDistance1()
	{
		return sensorDistance1;
	}

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

			private DigitalOutput triggerPin_;
			private PulseInput echoPin_;
			private int echoSecondsSensor1;
			private int echoDistanceCmSensor1;

			@Override
			protected void setup() throws ConnectionLostException, InterruptedException
			{
				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);

				triggerPin_ = ioio_.openDigitalOutput(36);
				echoPin_ = ioio_.openPulseInput(new DigitalInput.Spec(35), PulseInput.ClockRate.RATE_250KHz, PulseInput.PulseMode.POSITIVE, false);
			}

			@Override
			public void loop() throws ConnectionLostException, InterruptedException
			{
				led_.write(true);
				distances();

			}

			public void distances() throws ConnectionLostException, InterruptedException
			{
				triggerPin_.write(false);

				Thread.sleep(5);

				triggerPin_.write(true);

				Thread.sleep(1);

				triggerPin_.write(false);

				echoSecondsSensor1 = (int) (echoPin_.getDuration() * 1000 * 1000);
				echoDistanceCmSensor1 = echoSecondsSensor1 / 58;
				sensorDistance1 = echoDistanceCmSensor1;
				Log.d("IOIOSensor", "Odleglosc sensor1:" + echoDistanceCmSensor1);

				Thread.sleep(20);
			}
		};
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		sensorDistance1 = 0;
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

}

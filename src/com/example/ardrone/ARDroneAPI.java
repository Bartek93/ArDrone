package com.example.ardrone;

import android.util.Log;

public class ARDroneAPI
{

	private ARDrone ardrone;

	public static String DRONE_IP = "192.168.1.1";

	public ARDroneAPI() throws Exception
	{
		ardrone = new ARDrone(DRONE_IP);
	}

	public String getStatus()
	{
		return "";
	}

	public void landing()
	{
		try
		{
			ardrone.send_at_cmd("AT*REF=" + ardrone.get_seq() + ",290717696");
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception landing !!!");
			e.printStackTrace();
		}
	}

	public void hovering()
	{
		try
		{
			ardrone.send_pcmd(0, 0, 0, 0, 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception hovering !!!");
			e.printStackTrace();
		}
	}

	public void takeoff()
	{
		try
		{
			ardrone.send_at_cmd("AT*REF=" + ardrone.get_seq() + ",290718208");
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception takeoff !!!");
			e.printStackTrace();
		}
	}

	public void disbleEmergency()
	{
		try
		{
			ardrone.send_at_cmd("AT*REF=" + ardrone.get_seq() + ",290717952");

		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception disbleEmergency !!!");
			e.printStackTrace();
		}
	}

	// kalibracja akcelerometru
	public void trim()
	{
		try
		{
			ardrone.send_at_cmd("AT*FTRIM=" + ardrone.get_seq()); // flat trim
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception trim !!!");
			e.printStackTrace();
		}
	}

	public void up()
	{
		try
		{
			ardrone.send_pcmd(1, 0, 0, ardrone.getSpeed(), 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception up !!!");
			e.printStackTrace();
		}
	}

	public void down()
	{
		try
		{
			ardrone.send_pcmd(1, 0, 0, -ardrone.getSpeed(), 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception down !!!");
			e.printStackTrace();
		}
	}

	// yaw left
	public void rotatel()
	{
		try
		{
			ardrone.send_pcmd(1, 0, 0, 0, -ardrone.getSpeed());
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception rotatel !!!");
			e.printStackTrace();
		}
	}

	// yaw right
	public void rotater()
	{
		try
		{
			ardrone.send_pcmd(1, 0, 0, 0, ardrone.getSpeed());
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception rotater !!!");
			e.printStackTrace();
		}
	}

	public void goForward()
	{
		try
		{
			ardrone.send_pcmd(1, 0, -ardrone.getSpeed(), 0, 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception goForward !!!");
			e.printStackTrace();
		}
	}

	public void goBackward()
	{
		try
		{
			ardrone.send_pcmd(1, 0, ardrone.getSpeed(), 0, 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception goBackward !!!");
			e.printStackTrace();
		}
	}

	public void goRight()
	{
		try
		{
			ardrone.send_pcmd(1, ardrone.getSpeed(), 0, 0, 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception goRight !!!");
			e.printStackTrace();
		}
	}

	public void goLeft()
	{
		try
		{
			ardrone.send_pcmd(1, -ardrone.getSpeed(), 0, 0, 0);
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception goLeft !!!");
			e.printStackTrace();
		}
	}
	
	public void magnetoRotateRight()
	{
		try
		{
			ardrone.send_pcmd_mag(1, 0, 0, 0, ardrone.getSpeed(), ardrone.getMagneto_psi(), ardrone.getMagneto_psi_accuracy());
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception magnetoRotate !!!");
			e.printStackTrace();
		}
	}
	
	public void magnetoRotateLeft()
	{
		try
		{
			ardrone.send_pcmd_mag(1, 0, 0, 0, ardrone.getSpeed(), ardrone.getMagneto_psi(), ardrone.getMagneto_psi_accuracy());
		}
		catch (Exception e)
		{
			Log.i("DroneAPI", "Exception magnetoRotate !!!");
			e.printStackTrace();
		}
	}
}

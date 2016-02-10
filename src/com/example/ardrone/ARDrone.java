/*

Author: MAPGPS on
	https://projects.ardrone.org/projects/ardrone-api/boards
	http://bbs.5imx.com/bbs/forumdisplay.php?fid=453
Initial: 2010.09.20
Updated: 2010.10.09

UI_BIT:
00010001010101000000000000000000
   |   | | | |        || | ||||+--0: Button turn to left
   |   | | | |        || | |||+---1: Button altitude down (ah - ab)
   |   | | | |        || | ||+----2: Button turn to right
   |   | | | |        || | |+-----3: Button altitude up (ah - ab)
   |   | | | |        || | +------4: Button - z-axis (r1 - l1)
   |   | | | |        || +--------6: Button + z-axis (r1 - l1)
   |   | | | |        |+----------8: Button emergency reset all
   |   | | | |        +-----------9: Button Takeoff / Landing
   |   | | | +-------------------18: y-axis trim +1 (Trim increase at +/- 1??/s)
   |   | | +---------------------20: x-axis trim +1 (Trim increase at +/- 1??/s)
   |   | +-----------------------22: z-axis trim +1 (Trim increase at +/- 1??/s)
   |   +-------------------------24: x-axis +1
   +-----------------------------28: y-axis +1

AT*REF=<sequence>,<UI>
AT*PCMD=<sequence>,<enable>,<pitch>,<roll>,<gaz>,<yaw>
	(float)0.05 = (int)1028443341		(float)-0.05 = (int)-1119040307
	(float)0.1  = (int)1036831949		(float)-0.1  = (int)-1110651699
	(float)0.2  = (int)1045220557		(float)-0.2  = (int)-1102263091
	(float)0.5  = (int)1056964608		(float)-0.5  = (int)-1090519040
AT*ANIM=<sequence>,<animation>,<duration>
AT*CONFIG=<sequence>,\"<name>\",\"<value>\"

########## Commandline mode ############
Usage: java ARDrone <IP> <AT command>

altitude max2m:	java ARDrone 192.168.1.1 AT*CONFIG=1,\"control:altitude_max\",\"2000\"
Takeoff:	java ARDrone 192.168.1.1 AT*REF=1,290718208
Landing:	java ARDrone 192.168.1.1 AT*REF=1,290717696
Hovering:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,0,0,0
gaz 0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,0,1036831949,0
gaz -0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,0,-1110651699,0
pitch 0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,1036831949,0,0,0
pitch -0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,-1110651699,0,0,0
yaw 0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,0,0,1036831949
yaw -0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,0,0,-1110651699
roll 0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,1036831949,0,0
roll -0.1:	java ARDrone 192.168.1.1 AT*PCMD=1,1,0,-1110651699,0,0
pitch -30 deg:	java ARDrone 192.168.1.1 AT*ANIM=1,0,1000
pitch 30 deg:	java ARDrone 192.168.1.1 AT*ANIM=1,1,1000
Emergency	java ARDrone 192.168.1.1 AT*REF=1,290717952

########## Keyboad mode ############
Usage: java ARDrone [IP]

Takeoff/Landing: Space bar (toggle)
Hovering: Pause key

Arrow keys:
        Go Forward
            ^
            |
Go Left <---+---> Go Right
            |
            v
       Go Backward

Arrow keys with Shift key pressed:
              Go Up
                ^
                |
Rotate Left <---+---> Rotate Right
                |
                v
             Go Down
             
Digital keys 1~9: Change speed (rudder rate 5%~99%), 1 is min and 9 is max.
 */
package com.example.ardrone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

import android.util.Log;

import java.nio.*;

public class ARDrone
{
	static final int NAVDATA_PORT = 5554;
	static final int VIDEO_PORT = 5555;
	static final int AT_PORT = 5556;	
	static final int CONTROL_PORT = 5559;

	// NavData offset
	static final int NAVDATA_HEADER = 0;
	static final int NAVDATA_STATE = 4;
	static final int NAVDATA_SEQUENCE_NUMBER = 8;
	static final int NAVDATA_VISION_TAG = 12;
	static final int NAVDATA_BATTERY = 24;
	static final int NAVDATA_PITCH = 28;
	static final int NAVDATA_ROLL = 32;
	static final int NAVDATA_YAW = 36;
	static final int NAVDATA_ALTITUDE = 40; 			// pokazywane jak dron lata	
	static final int NAVDATA_VX = 44;					// pokazywane jak dron lata
	static final int NAVDATA_VY = 48;
	static final int NAVDATA_VZ = 52;	
	
	InetAddress inet_addr;
	DatagramSocket socket_at;
	int seq = 1; // Send AT command with sequence number 1 will reset the counter
	int seq_last = seq;
	String at_cmd_last = "";	
	boolean shift = false;
	FloatBuffer fb;
	IntBuffer ib;
	final static int INTERVAL = 100;
	
	private float altitude;
	private float yawAngle;
	
	// Parametry do zmian
	private float speed = (float) 0.05; // 0.05 to raczej minimum
	private float yawSpeed = (float) 0.2;	// 0.2 to raczej minimum
	private float magneto_psi = (float) 0.1;	// 0.1 to 18 stopni
	private float magneto_psi_accuracy = (float) 0;
	
	public ARDrone(String ip) throws Exception
	{
		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//
		Log.i("ARDrone", "Wejscie w kontruktor");
		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//

		StringTokenizer st = new StringTokenizer(ip, ".");

		byte[] ip_bytes = new byte[4];
		if (st.countTokens() == 4)
		{
			for (int i = 0; i < 4; i++)
			{
				ip_bytes[i] = (byte) Integer.parseInt(st.nextToken());
			}
		}
		else
		{
			Log.i("ARDrone", "IP:" + ip);
			// System.out.println("Incorrect IP address format: " + ip);
			System.exit(-1);
		}
		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//
		Log.i("ARDrone", "IP:" + ip);
		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//

		// System.out.println("IP: " + ip);
		inet_addr = InetAddress.getByAddress(ip_bytes);

		ByteBuffer bb = ByteBuffer.allocate(4);
		fb = bb.asFloatBuffer();
		ib = bb.asIntBuffer();

		socket_at = new DatagramSocket(ARDrone.AT_PORT);
		socket_at.setSoTimeout(3000);

		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//
		//Log.i("ARDrone", "Speed:" + speed);
		// ----------------------------------------------------------------------//
		// ----------------------------------------------------------------------//

		send_at_cmd("AT*COMWDG=" + get_seq());
		Thread.sleep(INTERVAL);

		NavData nData = new NavData(this, inet_addr);
		nData.start();
		
//		Control control = new Control(this, inet_addr);
//		control.start();
		
		//readDroneConfiguration(this);

		send_at_cmd("AT*PMODE=" + get_seq() + ",2");
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*MISC=" + get_seq() + ",2,20,2000,3000");
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*REF=" + get_seq() + ",290717696");
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"control:altitude_max\",\"2000\""); // altitude
																						// max
																						// 2m
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"control:altitude_min\",\"50\""); // altitude
																					// min
																					// 5cm
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"control:control_level\",\"0\""); // 0:BEGINNER,
																					// 1:ACE,
																					// 2:MAX
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"general:navdata_demo\",\"TRUE\"");

		Thread.sleep(INTERVAL);
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"general:video_enable\",\"TRUE\"");

		Thread.sleep(INTERVAL);
		// send_at_cmd("AT*CONFIG=" + get_seq() +
		// ",\"network:owner_mac\",\"00:18:DE:9D:E9:5D\""); //my PC
		// send_at_cmd("AT*CONFIG=" + get_seq() +
		// ",\"network:owner_mac\",\"00:23:CD:5D:92:37\""); //AP
		// Thread.sleep(INTERVAL);
		send_at_cmd("AT*CONFIG=" + get_seq() + ",\"pic:ultrasound_freq\",\"8\"");
		Thread.sleep(INTERVAL);
		// send_at_cmd("AT*FTRIM=" + get_seq()); //flat trim
		// Thread.sleep(INTERVAL);

		send_at_cmd("AT*REF=" + get_seq() + ",290717696");
		Thread.sleep(INTERVAL);
		send_pcmd(0, 0, 0, 0, 0);
		Thread.sleep(INTERVAL);
		send_at_cmd("AT*REF=" + get_seq() + ",290717696");
		Thread.sleep(INTERVAL);
		// send_at_cmd("AT*REF=" + get_seq() + ",290717952"); //toggle Emergency
		// Thread.sleep(INTERVAL);
		send_at_cmd("AT*REF=" + get_seq() + ",290717696");

	}

	public int intOfFloat(float f)
	{
		fb.put(0, f);
		return ib.get(0);
	}

	public static String byte2hex(byte[] data, int offset, int len)
	{
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++)
		{
			String tmp = Integer.toHexString(((int) data[offset + i]) & 0xFF);
			for (int t = tmp.length(); t < 2; t++)
			{
				sb.append("0");
			}
			sb.append(tmp);
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public static float byteArrayToFloat(byte[] b, int offset) 
	{
		return Float.intBitsToFloat(get_int(b, offset));
	}

	public static int get_int(byte[] data, int offset)
	{
		int tmp = 0, n = 0;

		//Log.i("ARDrone", "get_int(): data = " + byte2hex(data, offset, 4));
		// System.out.println("get_int(): data = " + byte2hex(data, offset, 4));
		for (int i = 3; i >= 0; i--)
		{
			n <<= 8;
			//Log.i("NavData", "data[offset + i]=" + data[offset + i] + "\n");
			tmp = data[offset + i] & 0xFF;
			n |= tmp;
		}
		return n;
	}
	
	// wziête z api
	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0;
		for (int i = 3; i >= 0; i--) {
			int shift = i * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}
	
	//wziête z api
	public static int byteArrayToShort(byte[] b, int offset) {
		return ((b[offset + 1] & 0x000000FF) << 8) + (b[offset] & 0x000000FF);
	}

	public synchronized int get_seq()
	{
		return seq++;
	}

	public void send_pcmd(int enable, float pitch, float roll, float gaz, float yaw) throws Exception
	{
		send_at_cmd("AT*PCMD=" + get_seq() + "," + enable + "," + intOfFloat(pitch) + "," + intOfFloat(roll) + "," + intOfFloat(gaz) + ","
				+ intOfFloat(yaw));
	}
	
	public void send_pcmd_mag(int enable, float pitch, float roll, float gaz, float yaw, float magneto_psi, float magneto_psi_accuracy) throws Exception
	{
		send_at_cmd("AT*PCMD_MAG=" + get_seq() + "," + enable + "," + intOfFloat(pitch) + "," + intOfFloat(roll) + "," + intOfFloat(gaz) + ","
				+ intOfFloat(yaw) + "," + intOfFloat(magneto_psi) + "," + intOfFloat(magneto_psi_accuracy));
	}
	
	// 0 to id magnetometru
	public void send_calib_mag() throws Exception
	{
		send_at_cmd("AT*CALIB=" + get_seq() + "," + 0);
	}

	/*
	 * public void send_pcmd(int enable, float pitch, float roll, float gaz,
	 * float yaw) throws Exception { System.out.println("Speed: " + speed);
	 * send_at_cmd("AT*SEQ=" + get_seq()); send_at_cmd("AT*RADGP=" +
	 * (short)(pitch*25000) + ", " + (short)(roll*25000) + ", " +
	 * (short)(gaz*25000) + ", " + (short)(yaw*25000)); }
	 */
	public synchronized void send_at_cmd(String at_cmd) throws Exception
	{
		//Log.i("ARDrone", "send_at_cmd:" + at_cmd);
		at_cmd_last = at_cmd;
		byte[] buf_snd = (at_cmd + "\r").getBytes();
		final DatagramPacket packet_snd = new DatagramPacket(buf_snd, buf_snd.length, inet_addr, ARDrone.AT_PORT);

		new Thread()
		{
			public void run()
			{
				try
				{
					socket_at.send(packet_snd);
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();

		/*
		 * byte[] buf_rcv = new byte[64]; DatagramPacket packet_rcv = new DatagramPacket(buf_rcv, buf_rcv.length); socket_at.receive(packet_rcv);
		 * System.out.println(new String(packet_rcv.getData(),0,packet_rcv.getLength()));
		 */
	}
	
	class NavData extends Thread
	{
		DatagramSocket socket_nav;
		InetAddress inet_addr;
		ARDrone ardrone;

		public NavData(ARDrone ardrone, InetAddress inet_addr) throws Exception
		{
			this.ardrone = ardrone;
			this.inet_addr = inet_addr;

			socket_nav = new DatagramSocket(ARDrone.NAVDATA_PORT);
			socket_nav.setSoTimeout(3000);
		}

		public void run()
		{
			int cnt = 0;

			try
			{
				byte[] buf_snd = { 0x01, 0x00, 0x00, 0x00 };
				DatagramPacket packet_snd = new DatagramPacket(buf_snd, buf_snd.length, inet_addr, ARDrone.NAVDATA_PORT);
				socket_nav.send(packet_snd);
				//Log.i("NavData", "Wys³ano trigger do portu UDP= " + ARDrone.NAVDATA_PORT);

				ardrone.send_at_cmd("AT*CONFIG=" + ardrone.get_seq() + ",\"general:navdata_demo\",\"TRUE\"");

				byte[] buf_rcv = new byte[10240];
				DatagramPacket packet_rcv = new DatagramPacket(buf_rcv, buf_rcv.length);

				while (true)
				{
					try
					{
						ardrone.send_at_cmd("AT*COMWDG=" + ardrone.get_seq());

						socket_nav.receive(packet_rcv);

						// po cholere ten counter?
						cnt++;
						if (cnt >= 5)
						{
							cnt = 0;

//							int option_tag = byteArrayToShort(buf_rcv, 16);
//							
//							if (option_tag == NavDataTag.NAVDATA_VISION_DETECT_TAG.getValue())
//							{
//								List<VisionTag> vtags = parseVisionTags(buf_rcv, 20);
//								if (vtags != null)
//									setVisionTags(vtags);
//							}
							
							
							//Log.i("NavData", "Otrzymano pakiet o d³ugoœci = " + packet_rcv.getLength() + " bajtów");

//							Log.i("NavData", "Bateria=" + ARDrone.get_int(buf_rcv, NAVDATA_BATTERY) + "%, "
//									+ "Wysokoœæ=" + ((float) ARDrone.get_int(buf_rcv, NAVDATA_ALTITUDE) / 1000) + "m");
//							
//							setAltitude(ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_ALTITUDE)/1000);
//							
//							Log.i("NavData", "Pitch=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_PITCH)/1000 + 
//									", Roll=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_ROLL)/1000 +
//									", Yaw=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_YAW)/1000);
							
							Log.i("NavData", "Yaw=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_YAW)/1000);
//							
							setYawAngle(ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_YAW)/1000);
//							
//							Log.i("NavData", "VX=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_VX) + 
//									", VY=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_VY) +
//									", VZ=" + ARDrone.byteArrayToFloat(buf_rcv, NAVDATA_VZ));
							
						}
					}
					catch (SocketTimeoutException ex3)
					{
						System.out.println("NavData: socket_nav.receive(): Timeout");
					}
					catch (Exception ex1)
					{
						ex1.printStackTrace();
					}
				}
			}
			catch (Exception ex2)
			{
				ex2.printStackTrace();
			}
		}
	}
	
	class Video extends Thread
	{
		DatagramSocket socket_video;
		InetAddress inet_addr;
		ARDrone ardrone;

		public Video(ARDrone ardrone, InetAddress inet_addr) throws Exception
		{
			this.ardrone = ardrone;
			this.inet_addr = inet_addr;

			socket_video = new DatagramSocket(ARDrone.VIDEO_PORT);
			socket_video.setSoTimeout(3000);
		}

		public void run()
		{
			try
			{
				byte[] buf_snd =
				{ 0x01, 0x00, 0x00, 0x00 };
				DatagramPacket packet_snd = new DatagramPacket(buf_snd, buf_snd.length, inet_addr, ARDrone.VIDEO_PORT);
				socket_video.send(packet_snd);
				System.out.println(" Video: Sent trigger flag to UDP port " + ARDrone.VIDEO_PORT);

				ardrone.send_at_cmd("AT*CONFIG=" + ardrone.get_seq() + ",\"general:video_enable\",\"TRUE\"");

				byte[] buf_rcv = new byte[64000];
				DatagramPacket packet_rcv = new DatagramPacket(buf_rcv, buf_rcv.length);

				while (true)
				{
					try
					{

						socket_video.receive(packet_rcv);
						System.out.println("Video Received: " + packet_rcv.getLength() + " bytes");
						// System.out.println(ARDrone.byte2hex(buf_rcv, 0, packet_rcv.getLength()));
					}
					catch (SocketTimeoutException ex3)
					{
						System.out.println("Video: socket_video.receive(): Timeout");
						socket_video.send(packet_snd);
					}
					catch (Exception ex1)
					{
						ex1.printStackTrace();
					}
				}
			}
			catch (Exception ex2)
			{
				ex2.printStackTrace();
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	class Control extends Thread
	{
		DatagramSocket socket_control;
		InetAddress inet_addr;
		ARDrone ardrone;
		
		public Control(ARDrone ardrone, InetAddress inet_addr) throws Exception
		{
			this.ardrone = ardrone;
			this.inet_addr = inet_addr;

			socket_control = new DatagramSocket(ARDrone.CONTROL_PORT);
			socket_control.setSoTimeout(3000);
			Log.i("Control", "konstruktor");
		}
		
		public void run()
		{
			try
			{
				Log.i("Control", "run poczatek");
				byte[] buf_snd = { 0x01, 0x00, 0x00, 0x00 };
				DatagramPacket packet_snd = new DatagramPacket(buf_snd, buf_snd.length, inet_addr, ARDrone.CONTROL_PORT);
				Log.i("Control", "run poczatek2");
				
				socket_control.send(packet_snd);
				Log.i("Control", "run poczatek3");
				
				ardrone.send_at_cmd("AT*CTRL=" + ardrone.get_seq());

				byte[] buf_rcv = new byte[10240];
				DatagramPacket packet_rcv = new DatagramPacket(buf_rcv, buf_rcv.length);	
				Log.i("Control", "run poczatek4");

				socket_control.receive(packet_rcv);
				
				Log.i("Control", "run koniec");
			}
			catch (Exception ex2)
			{
				ex2.printStackTrace();
			}
		}
	}
	
	public synchronized String readDroneConfiguration(ARDrone ardrone)
	{

		String ret = null;
		synchronized (this)
		{
			Socket socket = null;
			try
			{
				Log.i("Control", "run poczatek");
				socket = new Socket(inet_addr.getHostAddress(), CONTROL_PORT);

				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int readCount;
				InputStream in = socket.getInputStream();
				
				Log.i("Control", "run poczatek 2");
				try
				{
					ardrone.send_at_cmd("AT*CTRL=" + ardrone.get_seq() + ", \"4\" ");
				}
				catch (Exception e1)
				{
					Log.i("Control", "Exception");
					e1.printStackTrace();
				}
				//cmd_queue.add(new ControlCommand(4, 0));
				boolean continueReading = true;
				Log.i("Control", "run poczatek 3");
				while (continueReading && ((readCount = in.read(buffer)) > 0))
				{
					bos.write(buffer, 0, readCount);
					try
					{
						Thread.sleep(100); // TODO: figure out something more
											// complex. This code is required in
											// order to give drone time to send
											// content
					}
					catch (InterruptedException e)
					{
						Log.i("Control", "InterruptedException:");
					}
					continueReading = in.available() > 0;
				}
				bos.close();

				ret = new String(bos.toByteArray(), "ASCII");
				Log.i("Control", "ret="+ret);
			}
			catch (IOException ex)
			{
				Log.i("Control", "Error. Fialed to read drone configuration");
			}
			finally
			{
				try
				{
					socket.close();
				}
				catch (IOException e)
				{
					Log.i("Control", "IOException:");
				}
			}
		}

		Log.i("Control", "run poczatek 4");
		return ret;
	}
	
	public float getAltitude()
	{
		return altitude;
	}

	public void setAltitude(float altitude)
	{
		this.altitude = altitude;
	}
	
	public float getYawAngle()
	{
		return yawAngle;
	}

	public void setYawAngle(float yaw)
	{
		this.yawAngle = yaw;
	}

	public float getMagneto_psi()
	{
		return magneto_psi;
	}
	
	public void setMagneto_psi(float magneto_psi)
	{
		this.magneto_psi = magneto_psi;
	}

	public float getMagneto_psi_accuracy()
	{
		return magneto_psi_accuracy;
	}

	public float getSpeed()
	{
		return speed;
	}

	public void setSpeed(float s)
	{
		this.speed = s;
	}
	
	public float getYawSpeed()
	{
		return yawSpeed;
	}
}
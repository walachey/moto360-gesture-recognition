package de.fu_berlin.inf.moto360.util;

import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/*
	Singleton that cares about sending UDP packages.
 */
public class UDPInterface {

	private static UDPInterface singleton = null;

	private String targetAdress;
	private int port;

	public static UDPInterface getInstance( ) {
		if (singleton == null)
			singleton = new UDPInterface( );
		return singleton;
	}

	private UDPInterface() {
		// async call for udp packet instead?
		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setTarget(String target) {
		this.targetAdress = target;
	}

	public void send(String message){
		byte[] buffer = message.getBytes(Charset.forName("UTF-8"));

		try {
			InetAddress address = InetAddress.getByName(this.targetAdress);

			DatagramPacket packet = new DatagramPacket(
					buffer, buffer.length, address, this.port
			);
			DatagramSocket datagramSocket = new DatagramSocket();
			datagramSocket.send(packet);
			datagramSocket.close();
			Log.d("packet send", message + "send to " + this.targetAdress);
		}
		catch (SocketException e) {
			System.out.println("something went wrong with the socket :( \n");
			e.printStackTrace(System.out);
		} catch (UnknownHostException e) {
			System.out.println("something went wrong with the host :( \n");
			e.printStackTrace(System.out);
		} catch (IOException e) {
			System.out.println("something went wrong with the IO :( \n");
			e.printStackTrace(System.out);
		}
	}
}

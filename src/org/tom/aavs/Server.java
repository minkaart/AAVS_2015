package org.tom.aavs;

import processing.*;
import processing.core.*;
import processing.video.*;
import gab.opencv.*;

import java.awt.*;
import java.util.ArrayList;

import org.openkinect.freenect.*;
import org.openkinect.processing.*;

import oscP5.*;
import netP5.*;

import java.net.*;
import java.awt.image.*;

import javax.imageio.*;

import java.io.*;


public class Server extends PApplet {

	float viewX = 520;
	float viewY = 0;
	float viewHeight = 320;
	float viewWidth = 426;
	float viewSeparation = 10;

	int totalClients = 4;
	int activeClient = 0;

	private String serverIP = "127.0.0.1"; // "192.168.0.11"; // this should be taken from a config file
	private int serverPort = 11300;
	private String clientIP = "127.0.0.1"; // "192.168.0.1";
	private int clientPort = 11200;
	private int clientDatagramPort = 11100;

	OscP5 oscP5;
	String[] clients;
	Frame[] trackedFrames;

	NetAddress[] clientAddresses;
	OscMessage activeMessage;

	boolean[] receivedTrackingClient;

	DatagramSocket ds;  // to stream video


	public void setup() {
		size (1400, 800, P3D);


		trackedFrames = new Frame[totalClients];
		receivedTrackingClient = new boolean[totalClients];
		clients = new String[totalClients];
		clientAddresses = new NetAddress[totalClients];
		
		// todo get this info from txt file
		for (int i = 0; i < totalClients; i++) {
			clientAddresses[i] = new NetAddress("127.0.0" + (i+1), clientPort);   // 192.168.0."  FIXME
			clients[i] = "127.0.0." + (i+1); // clients we are writing to

			//trackedFrames[i] = new Frame (-100, -100, -100, -100, -100, -100, -100, -100);
			trackedFrames[i] = new Frame (
					this,
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480),
					(int)random (640), (int)random (480)
					);
			//200, 100, 100, 200, 200, 200);

			receivedTrackingClient[i] = false;
		}
		oscP5 = new OscP5(this, serverPort); // port we are listening to

		try {
			ds = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		activeMessage = new OscMessage("/active");

	}

	public void sendImage (PImage img, String ip, int port) {
		// We need a buffered image to do the JPG encoding
		BufferedImage bimg = new BufferedImage(img.width,img.height, BufferedImage.TYPE_INT_RGB);

		// Transfer pixels from localFrame to the BufferedImage
		img.loadPixels();
		bimg.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);

		// Need these output streams to get image as bytes for UDP communication
		ByteArrayOutputStream baStream = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(baStream);

		// compress the BufferedImage into a JPG and put it in the BufferedOutputStream
		try {
			ImageIO.write(bimg, "jpg", bos);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		// Get the byte array, which we will send out via UDP!
		byte[] packet = baStream.toByteArray();

		// Send JPEG data as a datagram
		// println("Sending datagram with " + packet.length + " bytes");

		// we send the frame to the active client

		//		System.out.println(packet.length);
		try {			
			ds.send(new DatagramPacket(packet, packet.length, InetAddress.getByName(ip), port));
		} 
		catch (Exception e) {
			e.printStackTrace();
		}	
	}


	public PImage getVideoFrame (float x, float y, float rot) {
		PImage img = loadImage("bridge.jpg"); // we should get the image from the video file instead
		return img;
	}


	public void draw() {
		background(0);
		stroke(255);
		strokeWeight(2);
		fill(0);

		// 4 "views"
		rect(viewX + viewSeparation, viewY + viewSeparation, viewWidth, viewHeight);
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewSeparation, viewWidth, viewHeight);
		rect(viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);
		rect(viewX + viewWidth + viewSeparation * 2, viewY + viewHeight + viewSeparation * 2, viewWidth, viewHeight);

		rect(viewSeparation, viewSeparation, 510, 510);

		PImage img = getVideoFrame (0,0,0);


		if (receivedMessagesFromAllClients()) {

			/* 	
			 * need to do the following:

			  		locate frame in 3D space
					decide which module is the active module
					getFrameToProject (currentState, location3D)
					broadcast modules off
					send JPEG frame to active module
					send coordinates to active module

			 */

			//1 we need to locate the frame in 3D space our of the information we have from the four "eyes"

		}				

		sendImage(img, clients[activeClient], clientDatagramPort);
		activateClient(activeClient);

		drawStatus();

		trackedFrames[0].draw((PApplet)this, viewX + viewSeparation, viewY + viewSeparation, 0.66f, img);
		trackedFrames[1].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewSeparation, 0.66f, img);
		trackedFrames[2].draw((PApplet)this, viewX + viewSeparation, viewY + viewHeight + viewSeparation * 2, 0.66f, img);
		trackedFrames[3].draw((PApplet)this, viewX + viewSeparation * 2 + viewWidth, viewY + viewHeight + viewSeparation * 2, 0.66f, img);

	}

	private void activateClient(int which) {
		
		// fixme! (we only have one client in testing)
		/*
		for (int i = 0; i < totalClients; i++) {
			activeMessage.clearArguments();
			activeMessage.add(i == which);
			oscP5.send(activeMessage, clientAddresses[i]);	
		}
		*/
		
		activeMessage.clearArguments();
		if (0 == which) {
			activeMessage.add(1);
		} else {
			activeMessage.add(0);
		}
		
		
		//activeMessage.add((boolean)(0 == which));
		oscP5.send(activeMessage, clientAddresses[0]);
	}

	private boolean receivedMessagesFromAllClients() {

		boolean all = true; 

		for (int i = 0; i < totalClients && !all; i++) {
			all = all && receivedTrackingClient[i];
		}

		return all;
	}

	private void drawStatus() {
		textSize(14);

		fill(255);
	}

	void oscEvent(OscMessage msg) {

		// println("server: received " + msg.addrPattern() + ", typetad: " + msg.typetag() + ", from: " + msg.address());


		String adr = msg.address();
		String[] adrBytes = split (adr, '.');
		int clientNumber = new Integer(adrBytes [3]).intValue() - 1; // 192.168.0.1 -> client 0

		// println("server: client number (last ip byte): " + clientNumber);

		receivedTrackingClient[clientNumber] = true;

		int totalVertices =  msg.typetag ().length() / 2; // 2 coordiantes per vertex, 
		// and we are only sending a list of integers with the coordinates

		trackedFrames[clientNumber].v = new ArrayList<PVector>();

		for (int i = 0; i < totalVertices; i++) {
			PVector vertex = new PVector(((Integer) msg.arguments()[i*2]).intValue(), 
					((Integer) msg.arguments()[i*2+1]).intValue());

			trackedFrames[clientNumber].v.add(vertex);

		}
	}

	private void reset() {
		for (int i = 0; i < totalClients; i++) {
			receivedTrackingClient[i] = false;
		}
	}

	public void keyPressed() {
		switch (key) {
		case 'p':
			for (int i = 0; i < totalClients; i++) {
				trackedFrames[i] = new Frame (
						this,
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480),
						(int)random (640), (int)random (480)
						);
			}
			break;
			
		case '0': case '1': case '2': case '3':			
			activeClient = keyCode-48;			
			break;
			

		}
	}

}







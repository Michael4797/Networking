package me.michael4797.network.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.HashMap;

import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;
import me.michael4797.util.BinaryReader;

public class UDPReceiverHandle implements ReceiverHandle{
	
	protected final DatagramSocket socket;
	protected final PacketReceiver<?> receiver;
	protected final DatagramPacket toReceive;
	protected final HashMap<InetSocketAddress, UDPSessionHandle> sessions = new HashMap<>();
	
	
	public UDPReceiverHandle(int port, PacketReceiver<?> receiver) {
		
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			throw new RuntimeException("Error initializing UDP socket", e);
		}
		
		int packetSize = Math.min(65507, receiver.getMaxPacketSize());
		toReceive = new DatagramPacket(new byte[packetSize], 0, packetSize);
		this.receiver = receiver;
	}

	
	@Override
	public int getPort() {

		return socket.getLocalPort();
	}

	
	@Override
	public UDPSessionHandle openSession(InetSocketAddress to) {

		synchronized(this) {
			
			UDPSessionHandle handle = sessions.get(to);
			if(handle != null)
				return handle;
			
			handle = new UDPSessionHandle(to, this);
			sessions.put(to, handle);
			return handle;
		}
	}


	@Override
	public UDPSessionHandle getSession(InetSocketAddress to) {

		return sessions.get(to);
	}

	
	@Override
	public void closeSession(SessionHandle handle) {
		
		synchronized(this) {
			
			sessions.remove(handle.getAddress());
			handle.close();
		}
	}

	
	@Override
	public void receive() {
		
		while(!socket.isClosed()) {
			
			try {
			
				socket.receive(toReceive);
			} catch(PortUnreachableException e){
				
				throw new RuntimeException("Error establishing connection", e);
			} catch (IOException e) {
					
				if(!socket.isClosed())
					throw new RuntimeException("Error reading from socket", e);
				else
					return;
			}
			
			UDPSessionHandle handle;
			synchronized(this) {
				
				InetSocketAddress address = (InetSocketAddress) toReceive.getSocketAddress();
				handle = sessions.get(address);
				if(handle == null) {
					
					handle = openSession(address);
					receiver.onConnect(handle);
				}		
			}

			BinaryReader reader = new BinaryReader(toReceive.getData(), toReceive.getOffset(), toReceive.getLength());
			readPackets(handle, reader);
		}
	}
	
	
	public void readPackets(UDPSessionHandle handle, BinaryReader reader) {
		
		try {
			if(!handle.readHeader(reader)) {
				
				System.out.println("Error reading packet from client " + toReceive.getAddress() + ": Unkown header in packet data");
				return;
			}
			
			while(reader.hasMoreData()) {
			
				int id = receiver.readPacketID(reader);
				Packet packet = receiver.readPacket(id, reader);
				receiver.onReceive(handle, packet);
			}
		}catch(Throwable e) {
			
			System.err.println("Error reading packet from client " + toReceive.getAddress() + ": ");
			e.printStackTrace();
		}
	}
	

	@Override
	public void close() {
		
		socket.close();
	}
}

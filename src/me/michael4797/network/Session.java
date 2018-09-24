package me.michael4797.network;

import java.io.IOException;

import me.michael4797.network.packet.Packet;
import me.michael4797.network.protocol.SessionHandle;

/**
 * Represents a single connection between this machine and a remote client.
 * This class is responsible for sending data to the remote client and maintaining
 * any relevant state data for the connection.
 */
public class Session {
	
	protected final SessionHandle handle;
	protected final SessionProtocol protocol;
	
	protected boolean connected = false;
	private final Object connectionLock = new Object();
	
	/**
	 * Creates a new session, backed by the specified {@link SessionHandle} and using the specified
	 * {@link SessionProtocol}.
	 * @param handle The SessionHandle created by the underlying protocol.
	 * @param protocol The SessionProtocol used by this Session.
	 */
	public Session(SessionHandle handle, SessionProtocol protocol){

		this.handle = handle;
		this.protocol = protocol;
		protocol.onInit(this);
	}
		
	/**
	 * Called when this Session is connected to a remote client.
	 * This method is called when a remote client opens the connection to
	 * this machine, as opposed to this machine opening the connection to
	 * the remote client.
	 * @see #connect()
	 */
	protected void connected() {

		synchronized(connectionLock) {
		
			connected = true;
		}
	}
	
	/**
	 * Called when this Session connects to a remote client.
	 * This method is called when this machine opens the connection to the
	 * remote client, as opposed to the remote client opening the connection
	 * to this machine.
	 * @see #connected()
	 */
	protected void connect(){
		
		synchronized(connectionLock) {
			
			if(connected)
				return;
			
			connected = true;			
			onConnect();
		}
	}
	
	/**
	 * Disconnects this client from the remote client, updates its connection state,
	 * forwards the event to the {@link SessionProtocol}, then removes this Session
	 * from being managed by the {@link PacketReceiver}. Once a connection is
	 * disconnected, it cannot be reestablished. A new connection must be reopened
	 * by a call to {@link PacketReceiver#openConnection(java.net.InetSocketAddress)
	 * openConnection(InetSocketAddress)}.
	 */
	public void disconnect(){
			
		synchronized(connectionLock) {
			
			if(!connected)
				return;
			
			onDisconnect();
			
			connected = false;
			handle.getReceiver().disconnectSession(this);
		}
	}

	/**
	 * Forwards the onConnect event to this Session's {@link SessionProtocol}.
	 * Unlike the {@link #connect()} and {@link #connected()} methods, this
	 * method is guaranteed to only be called once, and is called regardless of
	 * which end opens the connection.
	 */
	protected void onConnect() {
		
		protocol.onConnect();
	}

	/**
	 * Forwards the onConnect event to this Session's {@link SessionProtocol}.
	 * Unlike the {@link #disconnect()} method, this method is guaranteed to
	 * only be called once.
	 */
	protected void onDisconnect() {
		
		protocol.onDisconnect();
	}

	/**
	 * Signals to the {@link SessionProtocol} that this Session's connection
	 * has timed out. This is typically called by a {@link HeartBeat}.
	 */
	protected void onTimeout() {
		
		protocol.onTimeout();
	}
	
	/**
	 * Signals to the {@link SessionProtocol} that this connection has been "poked" either by a {@link PacketPoke} or
	 * by receiving some other response.
	 */
	public void onPoke(){
		
		protocol.onPoke();
	}
	
	/**
	 * Signals that a specific awaited response has been received by this Session.
	 */
	public void onMessage() {
		
		protocol.onMessage();
	}
	
	/**
	 * Retrieves the connected status of this Session.
	 * @return The connected status.
	 */
	public boolean isConnected() {
		
		return connected;
	}
	
	/**
	 * Sends the specified packet and forces the underlying protocol to ensure the Packet
	 * is reliably received by the remote client. If the underlying protocol does not
	 * enforce reliability by default, this method will carry additional overhead as compared
	 * to {@link #sendPacket(Packet)}.
	 * @see {@link #sendPacket(Packet)}
	 * @param packet The packet to send.
	 */
	public synchronized void sendPacketReliably(Packet packet){
		
		if(!connected)
			return;

		try {
			handle.forceReliability(true);
			handle.sendPacket(packet);
		} catch (IOException e) {
			System.err.println("Error sending packet data to client " + handle.getAddress());
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends the specified packet to the remote client. If the underlying protocol enforces
	 * reliability, this method will behave identically to {@link #sendPacketReliably(Packet)}.
	 * The Packet may not be sent immediately. By default, the underlying protocols will batch
	 * sent packets until either their packet buffers fill or {@link #launchPacket()} is called.
	 * @see #launchPacket()
	 * @param packet The packet to send.
	 */
	public synchronized void sendPacket(Packet packet){
		
		if(!connected)
			return;

		try {
			handle.forceReliability(false);
			handle.sendPacket(packet);
		} catch (IOException e) {
			System.err.println("Error sending packet data to client " + handle.getAddress());
			e.printStackTrace();
		}
	}
	
	/**
	 * When packets are sent, they are typically batched instead of being sent immediately.
	 * This function will flush the packet buffer, sending all batched Packets to the remote
	 * client.
	 */
	public synchronized void launchPacket(){

		if(!connected)
			return;
		
		try {
			handle.launchPacket();
		} catch (IOException e) {
			System.err.println("Error sending packet data to client " + handle.getAddress());
			e.printStackTrace();
		}
	}
	
	
	@Override
	public String toString(){
		
		return handle.getAddress().toString();
	}
}

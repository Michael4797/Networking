package me.michael4797.network;

import me.michael4797.network.packet.PacketConnect;
import me.michael4797.network.packet.PacketKick;

/**
 * A base implementation of {@link SessionProtocol} that sends a {@link PacketConnect} to 
 * establish the connection and uses a HeartBeat to ensure the connection hasn't been broken.
 */
public class BaseSessionProtocol implements SessionProtocol{
	
	public static final byte protocol = 0;
	protected Session session;
	protected HeartBeat heartBeat;
	
	
	@Override
	public void onInit(Session session) {

		this.session = session;
		heartBeat = new BaseHeartBeat(session);
		heartBeat.start();
	}
	
	
	@Override
	public void onConnect() {

		session.sendPacketReliably(new PacketConnect(protocol));
		session.launchPacket();
	}

	
	@Override
	public void onDisconnect() {

		heartBeat.halt();
	}

	
	@Override
	public void onPoke() {

		heartBeat.poke();
	}

	
	@Override
	public void onMessage() {

		heartBeat.response();
	}

	
	@Override
	public void onTimeout() {
		
		session.sendPacketReliably(new PacketKick("Response timed out"));
		session.launchPacket();
		session.disconnect();
	}

	
	@Override
	public byte getVersion() {
		
		return protocol;
	}
	
	
	public HeartBeat getHeartBeat() {
		
		return heartBeat;
	}
}

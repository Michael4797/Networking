package me.michael4797.network;

import me.michael4797.network.packet.PacketPoke;

/**
 * A simple implementation of the {@link HeartBeat} class used by {@link BaseSessionProtocol}.
 * Pulses consist of a single PacketPoke being sent reliably every 5 seconds. Timeouts are
 * generated after 15 seconds.
 */
public class BaseHeartBeat extends HeartBeat{

	
	public BaseHeartBeat(Session session) {
		
		super(session, 5000L, 15000L);
	}

	
	@Override
	protected void pulse() {
		
		session.sendPacketReliably(new PacketPoke());
		session.launchPacket();
	}
}

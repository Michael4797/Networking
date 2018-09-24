package me.michael4797.network.protocol;

import me.michael4797.network.packet.Packet;

/**
 * A convenience class used by {@link TCPSyncReceiverHandle}
 */
public class ReceivedPacket {

	public final SessionHandle handle;
	public final Packet packet;
	
	
	public ReceivedPacket(SessionHandle handle, Packet packet) {
		
		this.handle = handle;
		this.packet = packet;
	}
}

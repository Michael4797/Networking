package me.michael4797.network.packet;

import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * A Packet used to inform the client that the connection
 * is being terminated.
 */
public class PacketDisconnect extends Packet{
	
	
	public static PacketDisconnect read(BinaryInput reader) {

		return new PacketDisconnect();
	}

	
	@Override
	public void send(BinaryWriter writer) {}
}

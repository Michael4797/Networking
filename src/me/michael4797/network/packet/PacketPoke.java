package me.michael4797.network.packet;

import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * A Packet used to "poke" a connection to ensure that the client is
 * still connected and has received all of the sent packets.
 */
public class PacketPoke extends Packet{
	
	
	public static PacketPoke read(BinaryInput reader) {

		return new PacketPoke();
	}

	
	@Override
	public void send(BinaryWriter writer) {}
}

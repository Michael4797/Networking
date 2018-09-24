package me.michael4797.network.packet;

import java.io.IOException;

import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * A Packet used to inform the client that a new connection
 * has been established with a desired protocol.
 */
public class PacketConnect extends Packet{

	public final byte protocol;
	
	/**
	 * A Packet used to inform the client that a new connection
	 * has been established with a specified protocol.
	 * @param protocol The protocol version being used.
	 */
	public PacketConnect(byte protocol){
		
		this.protocol = protocol;
	}
	
	
	public static PacketConnect read(BinaryInput reader) throws IOException {

		return new PacketConnect(reader.readByte());
	}

	
	@Override
	public void send(BinaryWriter writer) {

		writer.writeByte(protocol);
	}
}

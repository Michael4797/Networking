package me.michael4797.network.packet;

import java.io.IOException;

import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * A Packet used to kick a client for not implementing a
 * compatible {@link me.michael4797.network.SessionProtocol SessionProtocol}.
 */
public class PacketInvalidProtocol extends Packet{

	public final byte protocol;
	
	/**
	 * A Packet used to kick a client for not implementing a
	 * compatible {@link me.michael4797.network.SessionProtocol SessionProtocol}.
	 * @param protocol The required protocol version.
	 */
	public PacketInvalidProtocol(byte protocol){
		
		this.protocol = protocol;
	}
	
	
	public static PacketInvalidProtocol read(BinaryInput reader) throws IOException {

		return new PacketInvalidProtocol(reader.readByte());
	}

	
	@Override
	public void send(BinaryWriter writer) {

		writer.writeByte(protocol);
	}
}

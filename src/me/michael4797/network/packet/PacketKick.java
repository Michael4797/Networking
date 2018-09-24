package me.michael4797.network.packet;

import java.io.IOException;

import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * A Packet used to inform the client that they have been
 * forcibly disconnected.
 */
public class PacketKick extends Packet{

	public final String reason;
	
	/**
	 * A Packet that informs the client that they have been
	 * forcibly disconnected for the specified reason.
	 * @param reason The reason for the disconnection.
	 */
	public PacketKick(String reason){
		
		this.reason = reason;
	}
	
	
	public static PacketKick read(BinaryInput reader) throws IOException {

		return new PacketKick(reader.readString());
	}

	
	@Override
	public void send(BinaryWriter writer) {
		
		writer.writeString(reason);
	}
}

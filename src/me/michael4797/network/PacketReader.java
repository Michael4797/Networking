package me.michael4797.network;

import java.io.IOException;

import me.michael4797.network.packet.Packet;
import me.michael4797.util.BinaryInput;

/**
 * Reads a single packet from a ByteInput. Responsible for deserializing packets sent over
 * a network connection.
 * @param <T> The type of Packet read.
 */
@FunctionalInterface
public interface PacketReader<T extends Packet> {

	/**
	 * Reads a single packet of type <T> from the specified ByteInput.
	 * @param reader The ByteInput to read.
	 * @return The deserialized Packet.
	 * @throws IOException If there is an error reading from the specified ByteInput.
	 */
	public T readPacket(BinaryInput reader) throws IOException;
}

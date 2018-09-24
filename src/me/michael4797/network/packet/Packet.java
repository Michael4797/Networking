package me.michael4797.network.packet;

import me.michael4797.util.BinaryWriter;

/**
 * A small amount of data to be sent and received over a network connection.
 */
public abstract class Packet {

	/**
	 * Serializes this class, writing its data to the specified ByteWriter in a way that can
	 * be used to reconstruct this Object later.
	 * @param writer The ByteWriter that this class's data should be written to.
	 */
	public abstract void send(BinaryWriter writer);
}
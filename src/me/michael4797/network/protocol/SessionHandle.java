package me.michael4797.network.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;

import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;

/**
 * The base functionality for a {@link me.michael4797.network.Session Session},
 * implemented by a {@link TransportProtocol}.
 */
public interface SessionHandle {

	/**
	 * Gets the {@link PacketReceiver} tied to the {@link ReceiverHandle}
	 * that created this SessionHandle.
	 * @return The PacketReceiver responsible for this SessionHandle.
	 */
	PacketReceiver<?> getReceiver();
	
	/**
	 * Gets the remote address of the client that this SessionHandle is connected to.
	 * @return The remote address.
	 */
	InetSocketAddress getAddress();
	
	/**
	 * Forces subsequent calls to {@link #sendPacket(Packet)} and {@link #launchPacket()}
	 * to enforce reliability, even if the underlying {@link TransportProtocol} does not
	 * support reliability by default. Calls to this method may force currently buffered
	 * Packets to be launched.
	 * @param reliable True if reliability should be enforced.
	 * @throws IOException If the call to this method results in buffered packets
	 * being launched and an error is encountered sending the Packet data
	 * via the underlying protocol.
	 */
	void forceReliability(boolean reliable) throws IOException;
	
	/**
	 * Sends a single packet of data using the underlying protocol. This method
	 * won't typically send the packet directly, but rather write the packet to
	 * a buffer that will be sent on a subsequent call to {@link #launchPacket()}.
	 * However, if necessary or beneficial, this method may immediately launch the
	 * packet.
	 * @param packet The next packet to be sent.
	 * @throws IOException If the call to this method results in Packet data being
	 * launched, and an error is encountered sending the Packet data via the
	 * underlying protocol.
	 */
	void sendPacket(Packet packet) throws IOException;
	
	/**
	 * Immediately sends all currently buffered packets to the remote client.
	 * @throws IOException If an error is encountered sending the Packet data via
	 * the underlying protocol.
	 */
	void launchPacket() throws IOException;
	
	/**
	 * Closes this connection and all resources tied to this SessionHandle.
	 */
	void close();
}

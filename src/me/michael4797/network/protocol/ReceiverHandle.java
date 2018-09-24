package me.michael4797.network.protocol;

import java.net.InetSocketAddress;

/**
 * The base functionality for a {@link me.michael4797.network.PacketReceiver PacketReceiver},
 * implemented by a {@link TransportProtocol}.
 */
public interface ReceiverHandle {

	/**
	 * Gets the actual local port to which this ReceiverHandle is bound.
	 * If this ReceiverHandle was created with a wildcard port, the return
	 * value of this function should be the actual port to which this ReceiverHandle
	 * is bound, as opposed to the port passed in the call to {@link TransportProtocol#createInstance(int, me.michael4797.network.PacketReceiver)
	 * TransportProtocol.createInstance(port, receiver)}.
	 * @return The port to which this ReceiverHandle is bound.
	 */
	int getPort();
	
	/**
	 * Opens a connection to the specified address and returns the {@link SessionHandle}
	 * representing this new connection. If a SessionHandle for the specified address already
	 * exists, the existing SessionHandle will be returned.
	 * @param to The address to connect to.
	 * @return A SessionHandle connected to the specified address.
	 */
	SessionHandle openSession(InetSocketAddress to);
	
	/**
	 * Gets the {@link SessionHandle} connected to the specified address. If no such
	 * SessionHandle exists, this method will return null.
	 * @param to The address to which the SessionHandle should be connected.
	 * @return The SessionHandle connected to the specified address, or null.
	 */
	SessionHandle getSession(InetSocketAddress to);
	
	/**
	 * Disconnects the specified {@link SessionHandle} and closes any resources tied to its connection.
	 * Closed SessionHandles will no longer be returned by calls to {@link #getSession(InetSocketAddress) getSession(address)}
	 * or {@link #openSession(InetSocketAddress) openSession(address)}.
	 * @param handle The SessionHandle to close.
	 */
	void closeSession(SessionHandle handle);
	
	/**
	 * Begins listening for connections and receiving packets. When this ReceiverHandle detects
	 * that a new client has connected, a SessionHandle will be created and {@link me.michael4797.network.PacketReceiver#onConnect(SessionHandle)
	 * PacketReceiver.onConnect(handle)} will be called. When this ReceiverHandle detects
	 * that a client has disconnected, the SessionHandle will be closed and {@link me.michael4797.network.PacketReceiver#onDisconnect(SessionHandle)
	 * PacketReceiver.onDisconnect(handle)} will be called. When this ReceiverHandle detects
	 * that a Packet has been received, {@link me.michael4797.network.PacketReceiver#onReceive(SessionHandle, me.michael4797.network.packet.Packet)
	 * PacketReceiver.onReceive(handle, packet)} will be called. This method will return only if there is
	 * an unrecoverable error in the underlying protocol, or if a call has been made to {@link #close()}.
	 * If an unrecoverable error is encountered by the underlying protocol, all of the resources tied to this
	 * ReceiverHandle and its respective {@link SessionHandle SessionHandles} will be closed prior to returning.
	 */
	void receive();
	
	/**
	 * Closes all the resources tied this this ReceiverHandle and its respective {@link SessionHandle SessionHandles}
	 * and stops this ReceiverHandle from listening for connections and receiving packets.
	 */
	void close();
}

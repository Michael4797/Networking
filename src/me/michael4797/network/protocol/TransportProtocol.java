package me.michael4797.network.protocol;

import me.michael4797.network.PacketReceiver;

/**
 * The underlying protocol used to send and receive data over a network.
 */
public interface TransportProtocol {

	/**
	 * A UDP-based connection where all {@link me.michael4797.network.PacketHandler PacketHandler}
	 * methods are called synchronously from the Thread that invokes {@link ReceiverHandle#receive()}.
	 */
	public static final TransportProtocol UDP_SYNC = (p, r) -> new UDPReceiverHandle(p, r);
	
	/**
	 * A TCP-based connection where {@link me.michael4797.network.PacketHandler PacketHandler}
	 * methods are called asynchronously. Each {@link me.michael4797.network.Session Session}
	 * will have its own worker thread responsible for invoking {@link me.michael4797.network.PacketHandler PacketHandlers}
	 * for Packets received from that client.
	 */
	public static final TransportProtocol TCP_ASYNC = (p, r) -> new TCPReceiverHandle(p, r);
	
	/**
	 * A UDP-based connection where {@link me.michael4797.network.PacketHandler PacketHandler}
	 * methods are called asynchronously. Each {@link me.michael4797.network.Session Session}
	 * will have its own worker thread responsible for invoking {@link me.michael4797.network.PacketHandler PacketHandlers}
	 * for Packets received from that client.
	 */
	public static final TransportProtocol UDP_ASYNC = (p, r) -> new UDPAsyncReceiverHandle(p, r);
	
	/**
	 * A TCP-based connection where all {@link me.michael4797.network.PacketHandler PacketHandler}
	 * methods are called synchronously from the Thread that invokes {@link ReceiverHandle#receive()}.
	 */
	public static final TransportProtocol TCP_SYNC = (p, r) -> new TCPSyncReceiverHandle(p, r);
	
	
	/**
	 * Creates a {@link ReceiverHandle} bound to the specified port, that is responsible for
	 * implementing the base networking functionality for the specified {@link PacketReceiver}.
	 * @param port The port to which the connection should be bound.
	 * @param receiver The PacketReceiver creating the connection.
	 * @return The {@link ReceiverHandle}, bound to the specified port.
	 */
	ReceiverHandle createInstance(int port, PacketReceiver<?> receiver);
}

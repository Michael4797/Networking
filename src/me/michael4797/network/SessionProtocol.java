package me.michael4797.network;

/**
 * Defines the protocol for connecting, disconnecting, and sustaining the connection
 * between two clients.
 */
public interface SessionProtocol {

	/**
	 * Called to initialize the protocol when a {@link Session} is established.
	 * @param session The newly created Session.
	 */
	void onInit(Session session);
	
	/**
	 * Called when the {@link Session} opens a connection to another client. This
	 * is not called if the connection is established by a remote client.
	 */
	void onConnect();
	
	/**
	 * Called when the {@link Session} is disconnected by a local call to
	 * {@link Session#disconnect()}.
	 */
	void onDisconnect();
	
	/**
	 * Called when the {@link Session} receives a {@link me.michael4797.packet.PacketPoke PacketPoke}
	 * or, more specifically, when {@link Session#onPoke()} is called. If this SessionProtocol uses a
	 * HeartBeat, this method should forward the event by a call to {@link HeartBeat#poke()}.
	 */
	void onPoke();
	
	/**
	 * Called when the {@link Session} receives a special message, or, more specifically,
	 * when {@link Session#onMessage()} is called. If this SessionProtocol uses a HeartBeat,
	 * this method should forward the event by a call to {@link HeartBeat#response()}.
	 */
	void onMessage();
	
	/**
	 * Called when the connection timeouts, this is typically triggered by a {@link HeartBeat},
	 * but may be triggered by any call to {@link Session#onTimeout()}.
	 */
	void onTimeout();
	
	/**
	 * Gets a byte representation of this protocol's version. This is used to ensure a newly connected client
	 * supports the same protocol.
	 * @return The protocol's version.
	 */
	byte getVersion();
}

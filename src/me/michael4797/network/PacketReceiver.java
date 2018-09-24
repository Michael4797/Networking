package me.michael4797.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import me.michael4797.annotation.ProcessedListener;
import me.michael4797.network.packet.Packet;
import me.michael4797.network.protocol.ReceiverHandle;
import me.michael4797.network.protocol.SessionHandle;
import me.michael4797.network.protocol.TransportProtocol;
import me.michael4797.util.BinaryInput;
import me.michael4797.util.BinaryWriter;

/**
 * Handles receiving packets and forwarding the events to the relevant {@link PacketListener PacketListeners}.
 * This class is also responsible for creating {@link Session Sessions} and managing their connection
 * state.
 *
 * @param <T> The type of Session created by this PacketReceiver.
 */
public abstract class PacketReceiver<T extends Session> extends Thread{

	protected boolean started = false;
	protected int maxPacketSize = 8192;
	protected int packetBufferSize = 64;
	
	private final ArrayList<PacketReader<?>> packetReaders = new ArrayList<>();
	private final HashMap<Class<? extends Packet>, Integer> packetIDs = new HashMap<>();
	
	private final int port;
	private final TransportProtocol protocol;
	private ReceiverHandle handle;
	
	private final HashMap<String, ArrayDeque<BiConsumer<Session, Packet>>> packetHandlers = new HashMap<>();
	private final HashMap<InetSocketAddress, T> sessions = new HashMap<>();
	private final ReentrantReadWriteLock lock;
	
	/**
	 * Creates a PacketReceiver that is bound to the specified port and uses a synchronous UDP protocol.
	 * @param port The port to bind this PacketReceiver to.
	 */
	public PacketReceiver(int port){
		
		this(port, TransportProtocol.UDP_SYNC);
	}

	/**
	 * Creates a PacketReceiver that is bound to the specified port and uses the specified protocol.
	 * @param port The port to bind this PacketReceiver to.
	 * @param protocol The protocol used for sending and receiving data.
	 */
	public PacketReceiver(int port, TransportProtocol protocol){
				
		lock = new ReentrantReadWriteLock();
		this.protocol = protocol;
		this.port = port;
	}
	
	/**
	 * Sets the maximum allowed size for a single Packet. If the specified size is larger than is
	 * supported by the underlying protocol, the size may be capped to a smaller value. If the
	 * underlying protocol does not have a maximum packet size, this value may be ignored entirely.
	 * This value cannot be changed after the PacketReceiver is started.
	 * @param size The maximum allowed size for a single Packet.
	 */
	public void setMaxPacketSize(int size) {

		synchronized(protocol) {
			if(started)
				throw new RuntimeException("Max packet size can not be changed after starting the PacketReceiver");
					
			maxPacketSize = size;
		}
	}
	
	/**
	 * The maximum allowed size for a single packet. If the specified size is larger than is
	 * supported by the underlying protocol, the actual maximum packet size might be capped
	 * to a smaller value.
	 * @return The maximum packet size, set by {@link #setMaxPacketSize(int)}.
	 */
	public int getMaxPacketSize() {
		
		return maxPacketSize;
	}
	
	/**
	 * For protocols that use a {@link PacketBuffer}, this sets the maximum amount of
	 * Packets stored in the PacketBuffer.
	 * @param size The amount of Packets to backlog.
	 */
	public void setPacketBufferSize(int size) {

		synchronized(protocol) {
			if(started)
				throw new RuntimeException("Packet buffer size can not be changed after starting the PacketReceiver");
			
			packetBufferSize = size;
		}
	}
	
	/**
	 * For protocols that use a {@link PacketBuffer}, this is the maximum amount of
	 * Packets stored in the PacketBuffer.
	 * @return The amount of Packets to backlog.
	 */
	public int getPacketBufferSize() {
		
		return packetBufferSize;
	}
	
	/**
	 * The handle for this PacketReceiver, constructed by the {@link TransportProtocol}.
	 * @see ReceiverHandle
	 * @return The handle.
	 */
	protected ReceiverHandle getHandle() {

		synchronized(protocol) {
			if(!started)
				throw new RuntimeException("Handle cannot be retrieved prior to starting the PacketReceiver");
			
			return handle;
		}
	}
	
	/**
	 * Registers a Packet so that it may be sent and received by this PacketReceiver and its
	 * {@link Sessions}. For two PacketReceivers to communicate with one another, both must
	 * have the exact same Packets registered in the exact same order.
	 * @param packetType The Packet's Class.
	 * @param reader The {@link PacketReader} responsible for deserializing this Packet.
	 */
	protected final <P extends Packet> void addPacket(Class<P> packetType, PacketReader<P> reader){
		
		int id = packetReaders.size();
		
		packetReaders.add(reader);
		packetIDs.put(packetType, id);
	}
	
	/**
	 * Gets the ID used to distinguish this packet over the network. If the specified PacketType is
	 * not registered, an Exception is thrown.
	 * @param packet The Packet to lookup.
	 * @return The id of the specified Packet.
	 */
	public final int getPacketID(Packet packet) {
		
		try {
			
			return packetIDs.get(packet.getClass());
		}catch(NullPointerException e) {
			
			throw new RuntimeException("No id for packet " + packet.getClass().getCanonicalName() + ". Did you add the packet to this receiver?");
		}
	}
	
	/**
	 * Gets the number of bytes needed to send packet IDs over the network.
	 * @return The required width of a Packet ID.
	 */
	public final byte getIDWidth() {

		if(packetIDs.size() == 1)
			return 0;
		if(packetIDs.size() <= 256)
			return 1;
		if(packetIDs.size() <= 65536)
			return 2;
		
		return 4;
	}
	
	/**
	 * Writes the ID of the specified Packet to the specified ByteWriter.
	 * @param packet The Packet whose ID should be written.
	 * @param writer The ByteWriter to which the ID should be written.
	 */
	public void writePacketID(Packet packet, BinaryWriter writer) {
		
		byte width = getIDWidth();
		if(width == 0)
			return;
		else if(width == 1)
			writer.writeByte((byte) getPacketID(packet));
		else if(width == 2)
			writer.writeShort((short) getPacketID(packet));
		else
			writer.writeInt(getPacketID(packet));
	}
	
	/**
	 * Reads the ID of the next Packet in the specified ByteInput.
	 * @param reader The ByteInput from which to read.
	 * @return The next Packet ID.
	 * @throws IOException If an error is encountered while reading from the ByteInput.
	 */
	public int readPacketID(BinaryInput reader) throws IOException {
		
		int id = 0;
		byte width = getIDWidth();
		if(width == 1)
			id = reader.readByte()&255;
		else if(width == 2)
			id = reader.readShort()&65535;
		else if(width == 4)
			id = reader.readInt();
		
		return id;
	}
	
	/**
	 * Reads a Packet of the specified ID from the specified ByteInput.
	 * @param id The ID of the Packet to read.
	 * @param reader The ByteInput from which the packet should be read.
	 * @return The read Packet.
	 * @throws IOException If an error is encountered while reading from the ByteInput.
	 */
	public Packet readPacket(int id, BinaryInput reader) throws IOException {
		
		return packetReaders.get(id).readPacket(reader);
	}
	
	/**
	 * Writes the specified Packet to the specified ByteWriter.
	 * @param packet The Packet to write.
	 * @param writer The ByteWriter to which the Packet should be written.
	 */
	public void writePacket(Packet packet, BinaryWriter writer) {
		
		packet.send(writer);
	}
	
	/**
	 * Registers a listener with this PacketReceiver so that its {@link PacketHandler} methods
	 * will be called when the appropriate Packets are received.
	 * @param listener The {@link PacketListener} to register.
	 */
	public void addListener(PacketListener listener){

		try {
			
			ProcessedListener processed = (ProcessedListener) Class.forName(listener.getClass().getCanonicalName() + "$$ProcessedListener", true, listener.getClass().getClassLoader()).getConstructors()[0].newInstance(listener);
			processed.addHandlers(getSessionType(), packetHandlers);
		} catch (Exception e) {

			throw new RuntimeException("Listener " + listener.getClass().getCanonicalName() + " is not a registered PacketListener. Make sure your project is correctly using the annotation processor.", e);
		}
	}
	
	
	public void start(){
		
		synchronized(protocol) {
			handle = protocol.createInstance(port, this);
			started = true;
		}
		
		setName("PacketReceiver-" + handle.getPort());
		super.start();
	}
	
	/**
	 * Stops this PacketReceiver and closes all its {@link Session Sessions}.
	 */
	public void halt(){
		
		forAll((s) -> true);
		handle.close();
	}
	
	
	public void run(){

		handle.receive();
	}
	
	/**
	 * Called by the underlying protocol when a client connects.
	 * @see ReceiverHandle#receive()
	 * @param handle The {@link SessionHandle} for the newly connected client.
	 */
	public synchronized void onConnect(SessionHandle handle) {
		
		T session = createSession(handle);
		session.connected();
		lock.readLock().lock();
		sessions.put(handle.getAddress(), session);
		lock.readLock().unlock();
	}
	
	/**
	 * Called by the underlying protocol when client disconnects.
	 * @see ReceiverHandle#receive()
	 * @param handle The {@link SessionHandle} of the disconnected client.
	 */
	public synchronized void onDisconnect(SessionHandle handle) {
		
		lock.readLock().lock();
		T session = sessions.remove(handle.getAddress());
		if(session != null)
			session.disconnect();
		lock.readLock().unlock();
	}
	
	/**
	 * Called by the underlying protocol when a Packet is received from a client.
	 * @see ReceiverHandle#receive()
	 * @param from The {@link SessionHandle} of the client who sent the Packet.
	 * @param packet The received Packet.
	 */
	public void onReceive(SessionHandle from, Packet packet) {

		T session = sessions.get(from.getAddress());
		if(session == null){

			System.out.println("Received packet from unknown client: " + from.getAddress());
			return;
		}
		
		handlePacket(session, packet);
	}
	
	/**
	 * Called every time a Packet is received by a client. Propagates the even to all
	 * relevant {@link PacketListener PacketListeners}.
	 * @param session The {@link Session} of the client who sent the Packet.
	 * @param packet The received Packet.
	 */
	protected void handlePacket(T session, Packet packet){
		
		ArrayDeque<BiConsumer<Session, Packet>> handlers = packetHandlers.get(packet.getClass().getCanonicalName());
		
		if(handlers == null){
			
			System.out.println("Warning received valid packet " + packet.getClass() + " but have no handlers for it.");
			return;
		}
		
		for(BiConsumer<Session, Packet> handler: handlers){
			
			try {
				handler.accept(session, packet);
			} catch (Exception e) {
				throw new RuntimeException("Exception executing event", e);
			}
		}
	}
	
	/**
	 * Creates a {@link Session} to wrap the specified {@link SessionHandle}.
	 * This method is called when a client connects.
	 * @see {@link #onConnect(SessionHandle)}<br/> {@link#openConnection(InetSocketAddress)}
	 * @param handle The SessionHandle of the new client.
	 * @return A Session wrapping the specified SessionHandle.
	 */
	protected abstract T createSession(SessionHandle handle);
	
	/**
	 * The Class of the type of Session returned by {@link #createSession(SessionHandle)}.
	 * This is used to check the type prior to casting Sessions when executing {@link PacketHandler}
	 * methods.
	 * @return The type of Session.
	 */
	protected abstract Class<? extends T> getSessionType();
	
	
	protected void disconnectSession(Session session) {

		lock.readLock().lock();
		sessions.remove(session.handle.getAddress());
		lock.readLock().unlock();
		handle.closeSession(session.handle);
	}
	
	
	/**
	 * Performs the Predicate on each {@link Session} managed by this PacketReceiver.
	 * If the Predicate returns true, the Session will be disconnected.
	 * @param predicate The Predicate to perform on each Session.
	 */
	public void forAll(Predicate<T> predicate){
		
		lock.writeLock().lock();
		Iterator<T> iterator = sessions.values().iterator();
		while(iterator.hasNext()) {
			
			T session = iterator.next();
			if(predicate.test(session)) {
				
				iterator.remove();
				session.disconnect();
			}
		}
		lock.writeLock().unlock();
	}
	
	/**
	 * Opens a connection to specified address and creates a {@link Session} for the
	 * new client.
	 * @param to The address to which the new Session should be connected.
	 * @return The newly created Session.
	 */
	public synchronized T openConnection(InetSocketAddress to){
		
		SessionHandle sessionHandle = handle.openSession(to);
		T session = sessions.get(sessionHandle.getAddress());
		
		if(session != null)
			return session;		
		
		session = createSession(handle.openSession(to));
		session.connect();
		lock.readLock().lock();
		sessions.put(session.handle.getAddress(), session);
		lock.readLock().unlock();
		return session;
	}
}
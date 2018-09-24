package me.michael4797.network.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;

public class TCPReceiverHandle implements ReceiverHandle{
	
	protected final ServerSocket socket;
	protected final PacketReceiver<?> receiver;
	protected final WorkerPool workers;
	protected final HashMap<InetSocketAddress, TCPSessionHandle> sessions = new HashMap<>();
	protected boolean closed;
	
	
	public TCPReceiverHandle(int port, PacketReceiver<?> receiver) {
		
		try {
			
			socket = new ServerSocket(port);
		} catch (IOException e) {
			
			throw new RuntimeException("Error initializing TCP socket", e);
		}
		
		this.receiver = receiver;
		workers = new WorkerPool();
	}

	
	@Override
	public int getPort() {

		return socket.getLocalPort();
	}

	
	@Override
	public SessionHandle openSession(InetSocketAddress to) {

		synchronized(this) {
			
			TCPSessionHandle handle = sessions.get(to);
			if(handle == null) {
				
				handle = new TCPSessionHandle(to, this);
				sessions.put(to, handle);
				workers.open(to);
				workers.execute(to, handle::receive);
			}
			
			return handle;
		}
	}

	
	@Override
	public void closeSession(SessionHandle handle) {
		
		synchronized(this) {
			InetSocketAddress address = handle.getAddress();
			handle.close();
			sessions.remove(address);
			workers.close(address);
		}
	}


	@Override
	public SessionHandle getSession(InetSocketAddress to) {

		return sessions.get(to);
	}
	
	
	protected void receivePacket(SessionHandle handle, Packet packet) {
		
		receiver.onReceive(handle, packet);
	}

	
	@Override
	public void receive() {
		
		while(!socket.isClosed()) {
			
			try {
				
				Socket client = socket.accept();
				
				synchronized(this) {
					
					InetSocketAddress address = (InetSocketAddress) client.getRemoteSocketAddress();
					TCPSessionHandle handle = new TCPSessionHandle(client, this);
					SessionHandle old = sessions.put(address, handle);
					if(old != null) {
						
						old.close();
						receiver.onDisconnect(old);
					}
					else {
					
						workers.open(address);
					}
					
					receiver.onConnect(handle);
					workers.execute(address, handle::receive);
				}
			} catch (IOException e) {

				if(!socket.isClosed())
					e.printStackTrace();
			}
		}
		
		close();
	}
	

	@Override
	public synchronized void close() {
		
		if(closed)
			return;
		
		closed = true;
		
		try {
			socket.close();
		} catch (IOException e) {}
		
		synchronized(this) {
		
			workers.close();
			sessions.clear();
		}		
	}
}

package me.michael4797.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;
import me.michael4797.util.BinaryInputStream;
import me.michael4797.util.BinaryWriter;

public class TCPSessionHandle implements SessionHandle{
	
	protected final Socket socket;
	protected final TCPReceiverHandle handle;
	protected final InputStream in;
	protected final OutputStream out;
	protected final BinaryWriter writer;
	protected boolean closed;
	
	
	public TCPSessionHandle(Socket socket, TCPReceiverHandle handle) {
		
		this.socket = socket;
		this.handle = handle;
		writer = new BinaryWriter();
		try {
			socket.setTcpNoDelay(true);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public TCPSessionHandle(InetSocketAddress to, TCPReceiverHandle handle) {
		
		try {
			this.socket = new Socket(to.getAddress(), to.getPort());
			socket.setTcpNoDelay(true);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		this.handle = handle;
		writer = new BinaryWriter();
	}
	
	
	public void receive() {			
		
		boolean running = true;
		BinaryInputStream reader = new BinaryInputStream(in);
		while(running && !socket.isClosed()) {
			
			try {
				if(!reader.hasMoreData())
					break;
				
				int id = handle.receiver.readPacketID(reader);
				Packet packet = handle.receiver.readPacket(id, reader);
				handle.receivePacket(this, packet);
			} catch (Throwable t) {
				if(!socket.isClosed() && !socket.isConnected()) {
				
					System.err.println("Error reading packet: ");
					t.printStackTrace();
				}
				else
					running = false;
			}
		}
		
		if(!closed) {

			closed = true;
			handle.closeSession(this);
			handle.receiver.onDisconnect(this);
		}
	}
	

	@Override
	public PacketReceiver<?> getReceiver() {

		return handle.receiver;
	}

	
	@Override
	public InetSocketAddress getAddress() {

		return (InetSocketAddress) socket.getRemoteSocketAddress();
	}

	
	@Override
	public void close() {
		
		if(closed)
			return;
		
		closed = true;
		try {
			socket.close();
		}catch(IOException e) {}
	}


	@Override
	public void forceReliability(boolean reliable) throws IOException {}


	@Override
	public void sendPacket(Packet packet) throws IOException {

		synchronized(this) {
			
			handle.receiver.writePacketID(packet, writer);
			handle.receiver.writePacket(packet, writer);
		}
	}


	@Override
	public void launchPacket() throws IOException {
		
		synchronized(this) {
			
			out.write(writer.getRawData(), 0, writer.getPosition());
			out.flush();
			writer.setPosition(0);
		}
	}		
}

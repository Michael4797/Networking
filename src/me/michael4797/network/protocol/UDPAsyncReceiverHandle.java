package me.michael4797.network.protocol;

import java.net.InetSocketAddress;
import me.michael4797.network.PacketReceiver;
import me.michael4797.util.BinaryReader;

public class UDPAsyncReceiverHandle extends UDPReceiverHandle{
	
	protected final WorkerPool workers;	
	
	
	public UDPAsyncReceiverHandle(int port, PacketReceiver<?> receiver) {
		
		super(port, receiver);
		workers = new WorkerPool();
	}

	
	@Override
	public UDPSessionHandle openSession(InetSocketAddress to) {

		synchronized(this) {
			
			UDPSessionHandle handle = super.openSession(to);
			workers.open(to);
			return handle;
		}
	}

	
	@Override
	public void closeSession(SessionHandle handle) {
		
		synchronized(this) {

			workers.close(handle.getAddress());
			super.closeSession(handle);
		}
	}
	
	
	@Override
	public void readPackets(UDPSessionHandle handle, BinaryReader reader) {
		
		workers.execute(handle.getAddress(), () -> super.readPackets(handle, reader));
		int packetSize = Math.min(65507, receiver.getMaxPacketSize());
		toReceive.setData(new byte[packetSize], 0, packetSize);
	}

	
	@Override
	public void close() {
		
		super.close();
		workers.close();
	}
}

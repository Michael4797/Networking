package me.michael4797.network.protocol;

import java.util.ArrayDeque;

import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;

public class TCPSyncReceiverHandle extends TCPReceiverHandle implements Runnable{
	
	protected final ArrayDeque<ReceivedPacket> packets = new ArrayDeque<>();
	
	
	public TCPSyncReceiverHandle(int port, PacketReceiver<?> receiver) {
		
		super(port, receiver);
	}
	
	
	@Override
	protected void receivePacket(SessionHandle handle, Packet packet) {
		
		packets.add(new ReceivedPacket(handle, packet));
		synchronized(packets) {
			packets.notifyAll();
		}
	}

	
	@Override
	public void receive() {
		
		Thread thread = new Thread(this, "TCP-Sync-Accept");
		thread.setDaemon(true);
		thread.start();
		
		while(!closed) {
			
			while(packets.isEmpty()) {
				synchronized(packets) {
					try {
						packets.wait();
					} catch (InterruptedException e) {}
				}
			}
			
			ReceivedPacket received = packets.poll();
			if(received == null)
				continue;
			
			receiver.onReceive(received.handle, received.packet);
		}
	}


	@Override
	public void run() {
		
		super.receive();
	}
	
	@Override
	public void close() {		
		
		super.close();
		
		synchronized(packets) {
			packets.notifyAll();
		}
	}
}

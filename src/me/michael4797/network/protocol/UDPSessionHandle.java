package me.michael4797.network.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import me.michael4797.network.PacketBuffer;
import me.michael4797.network.PacketData;
import me.michael4797.network.PacketReceiver;
import me.michael4797.network.packet.Packet;
import me.michael4797.network.packet.PacketKick;
import me.michael4797.util.BinaryReader;
import me.michael4797.util.BinaryWriter;

public class UDPSessionHandle implements SessionHandle{

	protected static final int PACKET_BUFFER_SIZE = 1024;
	protected static final byte MISSING_PACKETS_HEADER = 13;
	protected static final byte RELIABLE_HEADER = 12;
	protected static final byte UNRELIABLE_HEADER = 11;

	protected int lastSent;
	protected int lastReceived;
	protected int waitingForPacket;
	protected long lastResendTime;
	
	protected boolean reliable = false;
	
	protected final InetSocketAddress address;
	protected final UDPReceiverHandle handle;
	protected final PacketBuffer packetBuffer;
	protected final BinaryWriter writer;
	
	
	public UDPSessionHandle(InetSocketAddress address, UDPReceiverHandle handle) {
		
		this.address = address;
		this.handle = handle;
		packetBuffer = new PacketBuffer(handle.receiver.getPacketBufferSize());
		writer = new BinaryWriter();
		lastReceived = -1;
		lastSent = -1;
		waitingForPacket = -1;
	}
	

	@Override
	public PacketReceiver<?> getReceiver() {

		return handle.receiver;
	}

	
	@Override
	public InetSocketAddress getAddress() {

		return address;
	}


	@Override
	public void close() {}


	@Override
	public void forceReliability(boolean reliable) throws IOException {
		
		if(!this.reliable && reliable)
			launchPacket();
		
		this.reliable = reliable;
	}


	public void writeHeader(BinaryWriter writer) {
		
		if(this.reliable) {
		
			writer.writeByte(RELIABLE_HEADER);
			writer.writeInt(++lastSent);
		}
		else
			writer.writeByte(UNRELIABLE_HEADER);
	}
	
	
	@Override
	public synchronized void launchPacket() throws IOException{
		
		if(!writer.hasData())
			return;
			
		byte[] packet = writer.getData();
		if(reliable)
			packetBuffer.addPacket(lastSent, packet);
		
		writer.setPosition(0);
		DatagramPacket toSend = new DatagramPacket(packet, 0, packet.length, address);
		handle.socket.send(toSend);
	}
	
	
	@Override
	public synchronized void sendPacket(Packet packet) throws IOException{
		
		
		int startIndex = writer.getPosition();
		boolean empty = startIndex == 0;
		
		if(!empty){

			handle.receiver.writePacketID(packet, writer);
			handle.receiver.writePacket(packet, writer);
			
			if(writer.getPosition() > handle.receiver.getMaxPacketSize()){
				
				writer.setPosition(startIndex);
				launchPacket();
			}
			else
				return;
		}

		writeHeader(writer);
		handle.receiver.writePacketID(packet, writer);
		handle.receiver.writePacket(packet, writer);
		
		if(writer.getPosition() > handle.receiver.getMaxPacketSize()){
			
			writer.setPosition(0);
			throw new RuntimeException("Packet overflow exception: Packet " + packet.getClass() + " is larger than the specified max packet size.");
		}
	}


	public boolean readHeader(BinaryReader reader) throws IOException {

		byte header = reader.readByte();
		if(header == UNRELIABLE_HEADER)
			return true;

		if(header == RELIABLE_HEADER){
			
			int packetNumber = reader.readInt();
			if(waitingForPacket == packetNumber)
				waitingForPacket = -1;
			
			if(packetNumber - lastReceived > 1){

				if(waitingForPacket != -1)
					return true;
				
				long time = System.nanoTime();
				if(time - lastResendTime < 3000)
					return true;
				
				lastResendTime = time;
		
				waitingForPacket = lastReceived + 1;
				synchronized(this) {
					
					launchPacket();
					writer.writeByte(MISSING_PACKETS_HEADER);
					writer.writeInt(waitingForPacket);
					launchPacket();
				}
				
				reader.setPosition(reader.getData().length);
			}
			else if(packetNumber <= lastReceived)
				reader.setPosition(reader.getData().length);
			else
				lastReceived = packetNumber;

			return true;
		}
		
		if(header == MISSING_PACKETS_HEADER) {
			
			int packetNumber = reader.readInt();
			if(packetBuffer.maxSize < packetNumber - lastSent) {
				
				synchronized(this) {
					
					forceReliability(false);
					sendPacket(new PacketKick("Too many missed packets"));
					launchPacket();
					handle.closeSession(this);
					handle.receiver.onDisconnect(this);
				}
				
				return true;
			}
			
			for(PacketData data: packetBuffer){
				
				if(data.getPacketNumber() >= packetNumber) {
					
					DatagramPacket toSend = new DatagramPacket(data.getPacketData(), 0, data.getPacketData().length, address);
					handle.socket.send(toSend);
				}
			}
			
			return true;
		}
		
		return false;
	}
}

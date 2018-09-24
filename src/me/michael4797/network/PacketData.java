package me.michael4797.network;

/**
 * A utility class used by {@link PacketBuffer} to store previously sent Packets.
 */
public class PacketData {

	private final int number;
	private final byte[] data;
	
	PacketData(int number, byte[] data){
		
		this.number = number;
		this.data = data;
	}
	
	
	public int getPacketNumber(){
		
		return number;
	}
	
	
	public byte[] getPacketData(){
		
		return data;
	}
}

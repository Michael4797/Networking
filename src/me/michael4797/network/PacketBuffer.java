package me.michael4797.network;

import java.util.Iterator;

/**
 * Used by unreliable {@link TransportProtocol TransportProtocols} to save previously sent Packets
 * in the case that they need to be resent to enforce reliability.
 */
public class PacketBuffer implements Iterable<PacketData>{

	public final int maxSize;
	
	private Node head;
	private Node tail;
	private int size;
	
	private class Node{
		
		private PacketData data;
		private Node prev;
		
		private Node(PacketData data){
			
			this.data = data;
			if(head != null)
				head.prev = this;
			head = this;
		}
	}
	
	/**
	 * Creates a PacketBuffer with the specified capacity.
	 * The PacketBuffer will only store the most recently sent
	 * Packets.
	 * @param size The capacity of the PacketBuffer, after this many packets have
	 * been sent, the oldest Packets will start to be discarded.
	 */
	public PacketBuffer(int size){
		
		maxSize = size;
	}
	
	/**
	 * Stores the specified Packet data as the most recently sent Packet.
	 * @param number The Packet number of the specified packet data.
	 * @param data The Packet's data.
	 */
	public void addPacket(int number, byte[] data){
		
		Node node = new Node(new PacketData(number, data));
		
		if(tail == null)
			tail = node;
		
		if(size == maxSize) {
			
			Node newTail = tail.prev;
			tail.prev = null;
			tail = newTail;			
		}
		else {
			
			++size;
		}
	}
	
	/**
	 * Removes all Packets from the PacketBuffer.
	 */
	public void clear(){
		
		Node temp = tail;
		while(temp != null) {
			
			Node next = tail.prev;
			tail.prev = null;
			temp = next;
		}
		
		head = null;
		tail = null;
	}


	@Override
	public Iterator<PacketData> iterator() {

		return new Iterator<PacketData>(){

			Node node = tail;
			
			@Override
			public boolean hasNext() {
				
				return node != null;
			}

			@Override
			public PacketData next() {

				PacketData next = node.data;
				node = node.prev;
				return next;
			}

			@Override
			public void remove() {
				
				throw new UnsupportedOperationException();
			}			
		};
	}
}

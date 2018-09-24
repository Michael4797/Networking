package me.michael4797.network;

import me.michael4797.annotation.PacketHandler;
import me.michael4797.network.packet.PacketConnect;
import me.michael4797.network.packet.PacketDisconnect;
import me.michael4797.network.packet.PacketInvalidProtocol;
import me.michael4797.network.packet.PacketKick;
import me.michael4797.network.packet.PacketPoke;

/**
 * A default PacketListener class that handles the base Packets.
 * This PacketListener is automatically added to any {@link BasePacketReceiver}.
 */
public class BasePacketListener extends PacketListener{
	
	
	@PacketHandler
	public void handleConnect(Session session, PacketConnect packet){
		
		byte protocol = session.protocol.getVersion();
		if(protocol != packet.protocol) {
			
			session.sendPacket(new PacketInvalidProtocol(protocol));
			session.launchPacket();
			session.disconnect();
		}
	}

	
	@PacketHandler
	public void handleInvalidProtocol(Session session, PacketInvalidProtocol packet){
		
		session.disconnect();
		System.out.println("Incorrect protocol, using: " + session.protocol.getVersion() + ", correct version: " + packet.protocol);
	}

	
	@PacketHandler
	public void handleDisconnect(Session session, PacketDisconnect packet){
		
		session.disconnect();
	}

	
	@PacketHandler
	public void handleKick(Session session, PacketKick packet){
		
		session.disconnect();
		System.out.println("Kicked from server: \"" + packet.reason + "\"");
	}

	
	@PacketHandler
	public void handlePoke(Session session, PacketPoke packet){
		
		session.onPoke();
	}
}

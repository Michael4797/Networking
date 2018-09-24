package me.michael4797.network;

import me.michael4797.network.packet.PacketConnect;
import me.michael4797.network.packet.PacketDisconnect;
import me.michael4797.network.packet.PacketInvalidProtocol;
import me.michael4797.network.packet.PacketKick;
import me.michael4797.network.packet.PacketPoke;
import me.michael4797.network.protocol.SessionHandle;
import me.michael4797.network.protocol.TransportProtocol;

/**
 * A default PacketReceiver that uses the {@link BasePacketListener} and
 * implements the {@link BaseSessionProtocol}.
 */
public class BasePacketReceiver extends PacketReceiver<Session>{
	
	
	public BasePacketReceiver(int port){
		
		this(port, TransportProtocol.UDP_SYNC);
	}

	
	public BasePacketReceiver(int port, TransportProtocol protocol){
		
		super(port, protocol);

		addPacket(PacketConnect.class, PacketConnect::read);
		addPacket(PacketInvalidProtocol.class, PacketInvalidProtocol::read);
		addPacket(PacketDisconnect.class, PacketDisconnect::read);
		addPacket(PacketKick.class, PacketKick::read);
		addPacket(PacketPoke.class, PacketPoke::read);
		addListener(new BasePacketListener());
	}
	
	
	@Override
	public void halt() {
		
		forAll((s) -> {
			s.sendPacketReliably(new PacketDisconnect());
			s.launchPacket();
			return true;
		});
		
		super.halt();
	}

	
	@Override
	protected Session createSession(SessionHandle handle) {

		return new Session(handle, new BaseSessionProtocol());
	}

	
	@Override
	protected Class<? extends Session> getSessionType() {

		return Session.class;
	}
}

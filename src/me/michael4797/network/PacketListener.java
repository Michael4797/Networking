package me.michael4797.network;

import me.michael4797.annotation.RegisterPacketListener;

/**
 * An event handler class for Packets received by a {@link PacketReceiver}.
 * PacketListeners must have at least one {@link me.michael4797.annotation.PacketHandler PacketHandler}
 * method in order to be processed by the annotation processor.
 * @see PacketReceiver#addListener(PacketListener)
 */
@RegisterPacketListener
public abstract class PacketListener {}

package me.michael4797.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event handler method for a specific type of packet.
 * PacketHandler methods must be located inside of a {@link PacketListener}
 * in order to function. PacketHandler methods may be inherited and overridden.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface PacketHandler {}

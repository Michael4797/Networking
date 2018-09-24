package me.michael4797.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The root annotation used by {@link NetworkingProcessor} to locate and process
 * PacketListeners. Classes should never explicitly use this annotation,
 * but rather extend {@link PacketListener} to inherit the annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Inherited
public @interface RegisterPacketListener {}

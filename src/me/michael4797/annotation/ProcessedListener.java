package me.michael4797.annotation;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.function.BiConsumer;

import me.michael4797.network.Session;
import me.michael4797.network.packet.Packet;

/**
 * An interface for the source files generated by {@link NetworkingProcessor}.
 * Classes should never explicitly implement this interface, but rather rely on
 * the annotation processor to do so.
 */
public interface ProcessedListener {
	
	void addHandlers(Class<? extends Session> sessionType, HashMap<String, ArrayDeque<BiConsumer<Session, Packet>>> handlers);
	
	static boolean isCompatibleSession(Class<? extends Session> sessionType, String className) {		
		Class<?> temp = sessionType;
		while(!temp.equals(Session.class)) {			
			if(className.equals(temp.getCanonicalName()))
				return true;			
			temp = temp.getSuperclass();
		}		
		return false;
	}
	
	static void addHandler(String className, BiConsumer<Session, Packet> handler, HashMap<String, ArrayDeque<BiConsumer<Session, Packet>>> handlers){
		ArrayDeque<BiConsumer<Session, Packet>> list = handlers.get(className);
		if(list == null){
			list = new ArrayDeque<>();
			handlers.put(className, list);
		}
		list.add(handler);
	}
}

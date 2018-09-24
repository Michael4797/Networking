package me.michael4797.annotation;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;

import me.michael4797.network.Session;
import me.michael4797.network.packet.Packet;

import javax.tools.Diagnostic.Kind;

/**
 * Annotation processor for the {@link PacketHandler} and {@link RegisterPacketListener} annotations.
 * Generates a source file for each registered {@link PacketListener} that is responsible for
 * adding callbacks for each PacketHandler method to a PacketReceiver for event handling.
 */
public class NetworkingProcessor extends AbstractProcessor{
	
	private static final HashSet<String> annotations = new HashSet<String>();
	
	static {
		
		annotations.add(PacketHandler.class.getCanonicalName());
	}
	
	private Messager messager;
	private Filer filer;
	private Types typeUtil;
	private Elements elementUtil;
	private TypeMirror sessionType;
	private TypeMirror packetType;

	
	@Override
	public synchronized void init(ProcessingEnvironment env){

		messager = env.getMessager();
		filer = env.getFiler();
		typeUtil = env.getTypeUtils();
		elementUtil = env.getElementUtils();
		
		sessionType = elementUtil.getTypeElement(Session.class.getCanonicalName()).asType();
		packetType = elementUtil.getTypeElement(Packet.class.getCanonicalName()).asType();
	}

	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) { 
	
		HashSet<TypeElement> listeners = new HashSet<>();
		for(Element e: env.getElementsAnnotatedWith(PacketHandler.class)) {

			Element enclosing = e.getEnclosingElement();
			if(enclosing == null || enclosing.getKind() != ElementKind.CLASS)
				continue;
			
			if(enclosing.getAnnotation(RegisterPacketListener.class) != null)
				listeners.add((TypeElement)enclosing);
		}
		
		for(TypeElement e: listeners) {
			
			ArrayDeque<PacketHandlerMethod> handlers = new ArrayDeque<>();
			
			try {

				TypeElement type = (TypeElement) e;	
				String listener = type.getQualifiedName().toString();
				if(!processListener(type, handlers))
					continue;			
				
				if(handlers.isEmpty()) {
					
					messager.printMessage(Kind.WARNING, "PacketListener object has no declared PacketHandlers.", e);
					continue;
				}

				String className = listener + "$$ProcessedListener";
				FileObject generated = filer.createSourceFile(className, e);
				String packageName = listener.substring(0, listener.lastIndexOf('.'));
				className = className.substring(className.lastIndexOf('.')+1);

				StringBuilder source = new StringBuilder("package ");
				source.append(packageName);
				source.append(";\n");
				source.append("public class ");
				source.append(className);
				source.append(" implements me.michael4797.annotation.ProcessedListener{\n");
				source.append("\tprivate final ");
				source.append(listener);
				source.append(" l;\n");
				source.append("\tpublic ");
				source.append(className);
				source.append("(");
				source.append(listener);
				source.append(" l){\n");
				source.append("\t\tthis.l = l;\n");
				source.append("\t}\n");
				source.append("\tpublic void addHandlers(Class<? extends me.michael4797.network.Session> sessionType, java.util.HashMap<String, java.util.ArrayDeque<java.util.function.BiConsumer<me.michael4797.network.Session, me.michael4797.network.packet.Packet>>> handlers){\n");
				for(PacketHandlerMethod handler: handlers)
					source.append(handler);
				source.append("\t}\n");
				source.append("}\n");
				
				Writer writer = generated.openWriter();
				writer.write(source.toString());
				writer.close();
			}catch(Throwable t) {
				
				messager.printMessage(Kind.ERROR, "Failed to generate listener.", e);
			}
		}
		
		return true;
	}
	

	@Override
	public Set<String> getSupportedAnnotationTypes() { 
		
		return annotations;
	}

	
	@Override
	public SourceVersion getSupportedSourceVersion() {
	
		return SourceVersion.RELEASE_8;
	}
	
	
	private void traverseTypeHierarchy(TypeElement e, ArrayDeque<TypeElement> hierarchy, ArrayDeque<TypeElement> interfaces, TypeElement test) {
		
		if(e == null)
			return;
		
		if(e.getKind() == ElementKind.CLASS) {
			
			hierarchy.add(e);
			for(TypeMirror t: e.getInterfaces())
				traverseTypeHierarchy(elementUtil.getTypeElement(t.toString()), hierarchy, interfaces, test);
			traverseTypeHierarchy(elementUtil.getTypeElement(e.getSuperclass().toString()), hierarchy, interfaces, test);
		}
		else if(e.getKind() == ElementKind.INTERFACE) {
			
			interfaces.add(e);
			traverseTypeHierarchy(elementUtil.getTypeElement(e.getSuperclass().toString()), hierarchy, interfaces, test);
		}
	}
	
	
	private boolean processListener(TypeElement e, ArrayDeque<PacketHandlerMethod> handlers) {
		
		if(e.getModifiers().contains(Modifier.ABSTRACT))
			return false;
		
		ArrayDeque<TypeElement> typeStack = new ArrayDeque<>();
		ArrayDeque<TypeElement> interfaces = new ArrayDeque<>();
		traverseTypeHierarchy(e, typeStack, interfaces, e);
		
		typeStack.addAll(interfaces);
		while(!typeStack.isEmpty()) {
			
			TypeElement type = typeStack.pollLast();
			for(Element enclosed: type.getEnclosedElements())
				processHandler(enclosed, handlers);
		}
		
		return true;
	}
	
	
	private void processHandler(Element enclosed, ArrayDeque<PacketHandlerMethod> handlers) {
		
		if(enclosed.getAnnotation(PacketHandler.class) == null) {
			
			if(enclosed.getKind() != ElementKind.METHOD)
				return;
			
			if(enclosed.getModifiers().contains(Modifier.STATIC))
				return;
			
			ExecutableElement e = (ExecutableElement) enclosed;
			PacketHandlerMethod handler = getHandler(e, false);
			if(handler == null)
				return;
			
			if(removeOverridenElement(handler, handlers) == null)
				return;
			
			if(!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
				
				messager.printMessage(Kind.ERROR, "Illegal modifier for the PacketHandler method. PacketHandler methods must be public.", enclosed);
				return;
			}
			
			if(!e.getThrownTypes().isEmpty()) {
				
				messager.printMessage(Kind.ERROR, "Illegal modifier for the PacketHandler method. PacketHandlers must not throw any exceptions.", e);
				return;
			}
			
			handlers.add(handler);			
			return;
		}
		
		if(enclosed.getKind() != ElementKind.METHOD) {
		
			messager.printMessage(Kind.ERROR, "PacketHandler annotation must only be used on methods", enclosed);
			return;
		}

		if(!enclosed.getModifiers().contains(Modifier.PUBLIC)) {
			
			messager.printMessage(Kind.ERROR, "Illegal modifier for the PacketHandler method. PacketHandler methods must be public.", enclosed);
			return;
		}
		
		if(enclosed.getModifiers().contains(Modifier.STATIC)) {
			
			messager.printMessage(Kind.ERROR, "Illegal modifier for the PacketHandler method. PacketHandler methods cannot be static.", enclosed);
			return;
		}
		
		ExecutableElement e = (ExecutableElement) enclosed;
		if(!e.getThrownTypes().isEmpty()) {
			
			messager.printMessage(Kind.ERROR, "Illegal modifier for the PacketHandler method. PacketHandlers must not throw any exceptions.", e);
			return;
		}

		PacketHandlerMethod handler = getHandler(e, true);
		removeOverridenElement(handler, handlers);
		handlers.add(handler);
	}
	
	
	private PacketHandlerMethod getHandler(ExecutableElement e, boolean print) {
		
		List<? extends VariableElement> parameters = e.getParameters();
		if(parameters.size() < 1 || parameters.size() > 2) {
			
			if(print)
				messager.printMessage(Kind.ERROR, "Illegal parameters for the PacketHandler method. PacketHandlers must have either one or two parameters.", e);
			return null;
		}
		
		VariableElement parameter = parameters.get(0);
		String session = Session.class.getCanonicalName();
		
		if(parameters.size() == 2) {
			
			if(!typeUtil.isAssignable(parameter.asType(), sessionType)) {
			
				if(print)
					messager.printMessage(Kind.ERROR, "Illegal parameters for the PacketHandler method. First parameter must be of type Session or a subtype of Session.", e);
				return null;
			}

			session = parameter.asType().toString();
			parameter = parameters.get(1);
		}
		
		String packet = parameter.asType().toString();
		if(!typeUtil.isAssignable(parameter.asType(), packetType)) {
			
			if(print) {
				if(parameters.size() == 2)
					messager.printMessage(Kind.ERROR, "Illegal parameters for the PacketHandler method. Second parameter must be of type Packet or a subtype of Packet.", e);
				else
					messager.printMessage(Kind.ERROR, "Illegal parameters for the PacketHandler method. First parameter must be of type Packet or a subtype of Packet.", e);
			}
			
			return null;
		}

		return new PacketHandlerMethod(e.getSimpleName().toString(), session, packet, e);
	}
	
	
	private PacketHandlerMethod removeOverridenElement(PacketHandlerMethod handler, ArrayDeque<PacketHandlerMethod> handlers) {
		
		for(Iterator<PacketHandlerMethod> iterator = handlers.iterator(); iterator.hasNext();) {
			
			PacketHandlerMethod existing = iterator.next();
			if(existing.isOverriden(handler)) {
				
				iterator.remove();
				if(handler.element.getAnnotation(Override.class) == null)					
					messager.printMessage(Kind.NOTE, "PacketHandler method overrides exisitng PacketHandler but does not use the Override annotation. Is this intentional?", handler.element);
				
				return existing;
			}
		}
		
		return null;
	}
	
	
	private static class PacketHandlerMethod{
		
		private final String method;
		private final String session;
		private final String packet;
		private final ExecutableElement element;
		
		
		private PacketHandlerMethod(String method, String session, String packet, ExecutableElement element) {
			
			this.method = method;
			this.session = session;
			this.packet = packet;
			this.element = element;
		}
		
		
		private boolean isOverriden(PacketHandlerMethod other) {
			
			if(!other.method.equals(method))
				return false;
			
			if(!other.session.equals(session))
				return false;
			
			if(!other.packet.equals(packet))
				return false;
			
			return true;
		}
		
		
		@Override
		public String toString() {

			StringBuilder builder = new StringBuilder("\t\tme.michael4797.annotation.ProcessedListener.addHandler(\"");
			builder.append(packet);
			builder.append("\", (s, p) -> l.");
			builder.append(method);
			builder.append('(');
			
			if(!session.equals("me.michael4797.network.Session")) {
				builder.insert(0, "\"))\n\t");
				builder.insert(0, session);
				builder.insert(0, "\t\tif(me.michael4797.annotation.ProcessedListener.isCompatibleSession(sessionType, \"");
				builder.append('(');
				builder.append(session);
				builder.append(") ");
			}
			
			builder.append("s, (");
			builder.append(packet);
			builder.append(") p), handlers);\n");
			
			return builder.toString();
		}
	}
}

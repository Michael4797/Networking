package me.michael4797.network;

/**
 * A convenience class used to periodically send data to a 
 * connected client. If a response is not heard in time, the
 * connection is closed. The HeartBeat makes a distinction
 * between a poke and a message. Though, according to the base
 * protocol, a poke is only triggered by received a {@link PacketPoke},
 * a poke may triggered by any data received from the client,
 * whereas a message should only be triggered by some awaited
 * response, signaled by a call to {@link Session#onMessage()}.
 * This is to allow for a connection to timeout if a specific
 * Packet is not received in time, if so desired.
 */
public abstract class HeartBeat extends Thread{
	
	private long pulseInterval;
	private long timeout;
	private long respondBy;
	
	protected final Session session;
	
	private boolean suspended;
	private boolean running;
	private long sentTime;
	private long receivedTime;
	
	/**
	 * Creates a HeartBeat with a default pulse interval of 5 seconds
	 * and a default timeout of 15 seconds.
	 * @see #HeartBeat(Session, long, long)
	 * @param session The session to monitor.
	 */
	public HeartBeat(Session session){
		
		this(session, 5000L, 15000L);
	}
	
	/**
	 * Creates a HeartBeat that sends pulses of data to the specified Session
	 * at regular intervals. If no data is received from the Session after the
	 * specified timeout interval, the connection is closed and the Session is
	 * disconnected.
	 * @param session The session to monitor.
	 * @param pulseInterval The length of time between pulses.
	 * @param timeout The amount of time that the HeartBeat will wait for a
	 * response before closing the connection.
	 */
	public HeartBeat(Session session, long pulseInterval, long timeout){

		if(session == null)
			throw new NullPointerException("Session must be non null");

		if(pulseInterval < 0)
			throw new IllegalArgumentException("pulseInterval must be positive");
		
		if(timeout < 0)
			throw new IllegalArgumentException("timeout must be positive");
		
		if(pulseInterval == 0 && timeout == 0)
			throw new IllegalArgumentException("At least one of pulseInterval and timeout must be non zero");
		
		setDaemon(true);
		
		this.session = session;
		this.pulseInterval = pulseInterval;
		this.timeout = timeout;
		sentTime = System.currentTimeMillis();
		receivedTime = System.currentTimeMillis();
	}
	
	/**
	 * Sets the length of time between pulses. If this HeartBeat is already running, this will not
	 * take effect until the next pulse.
	 * @param interval The new pulse interval.
	 */
	public void setPulseInterval(long interval) {
		
		pulseInterval = interval;
	}
	
	/**
	 * Sets the amount of time this HeartBeat will wait for a response before closing the connection.
	 * If this HeartBeat is already running, this will not take effect until the next pulse.
	 * @param timeout The new timeout interval.
	 */
	public void setTimeout(long timeout) {
		
		this.timeout = timeout;
	}
	
	/**
	 * Sends a pulse of data to the Session monitored by this HeartBeat.
	 * This function is called once every pulse interval.
	 */
	protected abstract void pulse();
	
	/**
	 * Starts pulsing this HeartBeat and waiting for responses.
	 */
	public void start(){
		
		running = true;
		super.start();
	}
	
	/**
	 * Stops the HeartBeat.
	 */
	public void halt(){
		
		running = false;
		
		synchronized(this){
			notifyAll();
		}
		
		interrupt();
	}
	
	/**
	 * Called by {@link Session#onPoke()} to signal that a response
	 * has been received from the Session monitored by this HeartBeat.
	 */
	public void poke(){
		
		receivedTime = System.currentTimeMillis();
	}
	
	/**
	 * Called by {@link Session#onMessage()} to signal that a
	 * desired response has been received from the Session monitored
	 * by this HeartBeat.
	 */
	public void response() {
		
		respondBy = 0;		
		poke();
	}
	
	/**
	 * Sets a timer for receiving a desired response by the Session monitored by this HeartBeat.
	 * If the response is not received in time, the connection is closed and the Session disconnected.
	 * The desired response being received should be signaled by calling {@link Session#onMessage()}.
	 * @param timeout The length of time to wait for the response.
	 */
	public void expectResponse(long timeout) {
		
		respondBy = System.currentTimeMillis() + timeout;
	}
	
	/**
	 * Sets the suspended state of this HeartBeat. Suspended HeartBeats will not send pulses, or timeout
	 * connections. 
	 * @param suspended The suspended state of this HeartBeat.
	 */
	public synchronized void setSuspended(boolean suspended){
		
		if(!suspended && this.suspended){
			
			sentTime = System.currentTimeMillis();
			receivedTime = System.currentTimeMillis();

			this.suspended = suspended;				
			notifyAll();
		}
		else
			this.suspended = suspended;
	}
	
	
	private synchronized boolean waitWhileSuspended(){
		
		boolean waited = false;

		while(suspended){
				
			waited = true;			
			try {
				wait();
			} catch (InterruptedException e) {
					
				if(!running)
					break;
			}
		}
		
		return waited;
	}
	
	
	public void run(){

		long time = 0;
		long sendDif = 0;
		long receiveDif = 0;
		long respondDif = 0;
		long dif = 0;
		while(running){

			if(waitWhileSuspended()){
				
				if(!running)
					break;
			}
			
			time = System.currentTimeMillis();
			
			if(timeout != 0) {
				
				receiveDif = time - receivedTime;				
				if(receiveDif >= timeout){

					session.disconnect();
					break;
				}
			}
			
			if(pulseInterval != 0) {
				
				sendDif = time - sentTime;
				if(sendDif >= pulseInterval){
					
					pulse();					
					sentTime = System.currentTimeMillis();
					sendDif = 0;
				}
			}
			
			if(respondBy != 0) {
				
				respondDif = respondBy - time;
				if(respondDif <= 0) {

					session.onTimeout();
					respondBy = 0;
				}
			}
			
			dif = calculateSleepDuration(receiveDif, sendDif, respondDif);
			
			while(running && System.currentTimeMillis() - time < dif){
	
				try{

					Thread.sleep(dif);
				}catch(InterruptedException e){

					if(!running)
						break;
					
					System.out.println("Interrupted " + e.getMessage());
					time = System.currentTimeMillis();
					dif = calculateSleepDuration(time - receivedTime, time - sentTime, respondBy - time);			
				}
			}			
		}
	}
	
	
	private long calculateSleepDuration(long receiveDif, long sendDif, long respondDif) {
		

		if(pulseInterval == 0 && respondBy == 0)
			return timeout - receiveDif;
		
		if(timeout == 0 && respondBy == 0)
			return pulseInterval - sendDif;
		
		if(respondBy == 0)
			return Math.min(pulseInterval - sendDif, timeout - receiveDif);
		
		if(pulseInterval == 0)
			return Math.min(respondDif, timeout - receiveDif);
		
		if(timeout == 0)
			return Math.min(pulseInterval - sendDif, respondDif);

		return Math.min(pulseInterval - sendDif, Math.min(respondDif, timeout - receiveDif));		
	}
}

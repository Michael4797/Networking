package me.michael4797.network.protocol;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A convenience class used by the asynchronous {@link TransportProtocol TransportProtocols}.
 * This class creates a pool of worker threads used to execute basic tasks. One worker thread
 * is created per remote client.
 */
public class WorkerPool {

	private final HashMap<InetSocketAddress, Worker> workers;
	private boolean closed;
	
	/**
	 * Creates a new WorkerPool with no worker threads.
	 */
	public WorkerPool() {
		
		this.workers = new HashMap<>();
	}
	
	/**
	 * Executes the specified task on the worker thread for the specified client.
	 * If no such worker thread exists, the task is ignored.
	 * @param to The address of the client whose worker thread should be used.
	 * @param action The task to perform.
	 */
	public void execute(InetSocketAddress to, Runnable action) {
		
		Worker worker = workers.get(to);
		if(worker != null)
			worker.execute(action);
	}
	
	/**
	 * Creates a worker thread for a client with the specified address, if one does not
	 * already exist.
	 * @param to The client for which a worker thread should be created.
	 */
	public synchronized void open(InetSocketAddress to) {

		if(closed)
			return;
		
		if(workers.containsKey(to))
			return;
		
		workers.put(to, new Worker(to));
	}
	
	/**
	 * Closes the worker thread for the client with the specified address.
	 * @param to The client whose worker thread should be closed.
	 */
	public synchronized void close(InetSocketAddress to) {

		if(closed)
			return;
		
		Worker worker = workers.remove(to);
		if(worker != null)
			worker.close();
	}
	
	/**
	 * Closes all worker threads in this WorkerPool.
	 */
	public synchronized void close() {

		if(closed)
			return;
		
		ArrayList<Worker> values = new ArrayList<>(workers.size());
		values.addAll(workers.values());
		for(Worker worker: values)
			worker.close();
		
		workers.clear();
		closed = true;
	}
	
	
	private class Worker implements Runnable{
		
		private final ArrayDeque<Runnable> queue;
		private volatile boolean open;
		
		
		private Worker(InetSocketAddress address) {
			
			queue = new ArrayDeque<>();
			open = true;
			Thread thread = new Thread(this, "Worker-Thread-" + address);
			thread.setDaemon(true);
			thread.start();
		}
		
		
		private void close() {
			
			open = false;
			queue.clear();
		}
		
		
		private synchronized void execute(Runnable action) {
			
			queue.add(action);
			notifyAll();
		}
		
		
		@Override
		public void run() {
			
			while(open) {
				
				while(queue.isEmpty()) {
					synchronized(this) {
						try {
							wait();
						} catch (InterruptedException e) {}
					}
				}
				
				Runnable action = queue.poll();
				if(action == null)					
					continue;
				
				action.run();
			}
		}
	}
}

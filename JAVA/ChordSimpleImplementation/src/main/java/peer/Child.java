
package peer;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class Child extends Thread implements ServletContextListener{
	private NodoInfo nodo;
	private Client client = ClientBuilder.newClient();
	private String ipNodoEntrada = "192.168.1.41";
	
	public Child() {
		nodo = new NodoInfo();
		this.nodo.hashIP();
		System.out.println ("Ip del Nodo: " + this.nodo.ip + ", Id del nodo: " + this.nodo.id);
	}

	public void run() {
		boolean success = init();
		while(!success) {
			success = init();
			System.out.println("NOT JOIN");
		}
		while(true) {
			
			try {
				this.coroutine();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	

	
	public synchronized boolean init() {
		try {
			this.wait(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		
		String uri = "http://" + nodo.ip + ":8080/ChordSimpleImplementation/nodo";
		try {
		client.target(uri)
             .path("join")
             .request(MediaType.TEXT_PLAIN)
             .post(Entity.entity(ipNodoEntrada, MediaType.TEXT_PLAIN));
		
		} catch (Exception e){
			return false;
		}
		return true;
	}
	
	private synchronized void coroutine() throws InterruptedException {
		this.wait(1000);
		
		String uri = "http://" + nodo.ip + ":8080/ChordSimpleImplementation/nodo";
		try {
			client.target(uri)
	             .path("check_predecesor")
	             .request(MediaType.APPLICATION_JSON)
	             .get();
			 
			client.target(uri)
		        .path("estabilizar")
		        .request(MediaType.APPLICATION_JSON)
		        .get();
		} catch(Exception e) {
			//
		}
	}
}

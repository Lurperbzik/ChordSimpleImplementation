
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
	private String ipNodoEntrada = "192.168.1.134";
	
	public Child() {
		nodo = new NodoInfo();
		this.nodo.hashIP();
		System.out.println ("Ip del Nodo: " + this.nodo.ip + ", Id del nodo: " + this.nodo.id);
	}

	public void run() {
		
		while(!init()) {
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
		}
		String uri = "http://" + ipNodoEntrada+ ":8000";
		try {
			client.target(uri)
            .path("creaAnillo")
            .request(MediaType.TEXT_PLAIN)
            .post(null);
		
		} catch (Exception ex) {
			return false;
		}
		uri = "http://" + nodo.ip + ":8080/ChordSimpleImplementation/nodo";
		try {
		client.target(uri)
             .path("join")
             .request(MediaType.TEXT_PLAIN)
             .post(Entity.entity("http://" + ipNodoEntrada + ":8000", MediaType.APPLICATION_JSON));
		
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

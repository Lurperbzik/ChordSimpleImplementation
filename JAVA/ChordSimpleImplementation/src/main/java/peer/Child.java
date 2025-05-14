package peer;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;


@WebListener
public class Child extends Thread implements ServletContextListener{
	private NodoInfo nodo;
	private Client client = ClientBuilder.newClient();

	@Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Servidor iniciado. Ejecutando método...");
        try {
			this.init();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
	
   @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

	public Child(NodoInfo nodo) {
		this.nodo = nodo;
	}
	public void run() {
		try {
			this.init();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while(true) {
			try {
				this.coroutine();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	

	
	private synchronized void init() throws InterruptedException {
		this.wait(2000);
		
		String uri = "http://" + nodo.ip + ":8080/ChordSimpleImplementation/nodo";
		client.target(uri)
             .path("join")
             .request(MediaType.TEXT_PLAIN)
             .post(Entity.entity("http://192.168.1.134:8000", MediaType.APPLICATION_JSON));
	}
	
	private synchronized void coroutine() throws InterruptedException {
		this.wait(1000);
		
		String uri = "http://" + nodo.ip + ":8080/ChordSimpleImplementation/nodo";
		client.target(uri)
             .path("check_predecesor")
             .request(MediaType.APPLICATION_JSON)
             .get();

		client.target(uri)
	        .path("estabilizar")
	        .request(MediaType.APPLICATION_JSON)
	        .get();
	}
}

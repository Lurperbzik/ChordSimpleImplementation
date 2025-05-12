package peer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;


@Singleton
@Path("nodo")
public class Peer {
	
	 public static class AnilloInfo {
	        public String ipEntrada;
	        public Integer tamanoAnillo;
	    }

	int tamanoAnillo;
	int numSucesores = 2;
	
	private String uriNodoEntrada;
	NodoInfo nodo = new NodoInfo();
	NodoInfo predecesor = new NodoInfo();
	NodoInfo sucesor = new NodoInfo();
	HashMap <Integer, String> recursos;
	private Client client = ClientBuilder.newClient();
	//private final ScheduledExecutorService scheduler;

	
	
	public Peer() throws UnknownHostException {
		String[] data = InetAddress.getLocalHost().toString().split("/");
		this.nodo.ip = data[data.length - 1];
		System.out.println("IP " + this.nodo.ip);
		
		/*
        scheduler = Executors.newScheduledThreadPool(1);
	    
        Runnable tarea = () -> {
	    	if (this.sucesor.id != -1) {
		    	this.estabilizar();
		    	this.check_predecesor();
		    	System.out.println("Hola");
	    	}
	    };
        scheduler.scheduleAtFixedRate(tarea, 0, 1, TimeUnit.SECONDS);
		 */
	}
	
	
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	@Path("status")
	public String getSucesor() {
		return "Id: " + this.nodo.id + " Ip: " + this.nodo.ip +  " Id Sucesor: " + this.sucesor.id +  " Id Predecesor: " + this.predecesor.id;
	}
	
	@POST
	@Path("join")
	@Consumes(MediaType.TEXT_PLAIN)
	public void join(String url) {
		this.uriNodoEntrada = url;
		AnilloInfo result = client.target(url)
				.path("Anillo")
				.request(MediaType.APPLICATION_JSON)
	            .get(AnilloInfo.class);
		
		System.out.println("Hola");
		tamanoAnillo = result.tamanoAnillo;
		this.nodo.tamanoAnillo = tamanoAnillo;
		
		this.nodo.hashIP();
		System.out.println(this.nodo.id);
		if (this.nodo.ip.equals(result.ipEntrada)) {
			this.creaAnillo();
		}
		else {
			String uri = "http://" + result.ipEntrada + ":8080/ChordSimpleImplementation/nodo";
			System.out.println(uri);
			NodoInfo nuevoSucesor = client.target(uri)
			             .path("find_sucesor")
			             .queryParam("id", nodo.id)
			             .request(MediaType.APPLICATION_JSON)
			             .get(NodoInfo.class);
			sucesor = nuevoSucesor;
		}
	}
	
	public void creaAnillo() {
		sucesor = this.nodo;
	}
	

	@GET
	@Path("check_sucesor")
	public void check_predecesor() {
		if (predecesor.id == -1) {
			return;
		}
		
		String uri = "http://" + predecesor.ip + ":8080/ChordSimpleImplementation/nodo";
		try {
			client.target(uri)
	            .path("status")
	            .request(MediaType.TEXT_PLAIN)
	            .get(String.class);
		} catch (Exception e) {
			this.predecesor.id = -1;
		}
		
	}
	
	@GET
	@Path("find_sucesor")
	@Produces(MediaType.APPLICATION_JSON)
	public NodoInfo findSucesor(@QueryParam("id") Integer id) {
		System.out.println("Find sucesor");
		
		if (this.nodo.id == this.sucesor.id) {
			return sucesor;
		}
		if (id > this.nodo.id && id <= this.sucesor.id) {
			return sucesor;
		}
		
		if(this.sucesor.id < this.nodo.id) {
			if (id > this.nodo.id && id <= this.sucesor.id + Math.pow(2, tamanoAnillo)) {
				return sucesor;
			}
		}
		
		String uri = "http://" + sucesor.ip + ":8080/ChordSimpleImplementation/nodo";
		return client.target(uri)
		             .path("find_sucesor")
		             .queryParam("id", id)
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
	}
	
	@GET
	@Path("predecesor")
	@Produces(MediaType.APPLICATION_JSON)
	public NodoInfo predecesor() {
		return this.predecesor;
	}

	@GET
	@Path("estabilizar")	
	public void estabilizar() {
		System.out.println("Estabilizando...");
		NodoInfo posibleSucesor;
		String uri = "http://" + this.sucesor.ip + ":8080/ChordSimpleImplementation/nodo";

		try {
			posibleSucesor = client.target(uri)
		             .path("predecesor")
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
		} catch(Exception exception) {
			this.sucesor.id = -1;
			this.predecesor.id = -1;
			this.join(uriNodoEntrada);
			return;
		}
		

		if (posibleSucesor.id != -1 && posibleSucesor.id != nodo.id) {
			
			if(posibleSucesor.id > nodo.id  && posibleSucesor.id < this.sucesor.id) {
				this.sucesor = posibleSucesor;
				System.out.println("Cambiando sucesor a: " + posibleSucesor.id);
			}
			else if(this.sucesor.id <= nodo.id) {
				if(posibleSucesor.id > nodo.id) {
					this.sucesor = posibleSucesor;
					System.out.println("Cambiando sucesor a: " + posibleSucesor.id);
				}
				else if (posibleSucesor.id < this.sucesor.id) {
					this.sucesor = posibleSucesor;
					System.out.println("Cambiando sucesor a: " + posibleSucesor.id);
				}
			}
		}
	    uri = "http://" + this.sucesor.ip + ":8080/ChordSimpleImplementation/nodo";
		client.target(uri)
	             .path("notificar")
	             .request(MediaType.APPLICATION_JSON)
	             .post(Entity.entity(this.nodo, MediaType.APPLICATION_JSON));
		return;
	}
	
	
	@POST
	@Path("notificar")
	@Consumes(MediaType.APPLICATION_JSON)
	public void notificar(NodoInfo posiblePred) {
		System.out.println("Notificando");
		
		System.out.println("id: " + posiblePred.id);
		 
		if (this.predecesor.id == -1) {
			this.predecesor = posiblePred;
			return;
		}
		
		if(posiblePred.id > this.predecesor.id && posiblePred.id < this.nodo.id) {
			this.predecesor = posiblePred;
		}
		else if(this.predecesor.id >= nodo.id) {
			if(posiblePred.id > nodo.id) {
				this.predecesor = posiblePred;
				System.out.println("Cambiando predecesor a: " + posiblePred.id);

			}
			else if (posiblePred.id < this.predecesor.id) {
				this.predecesor = posiblePred;
				System.out.println("Cambiando predecesor a: " + posiblePred.id);

			}
		}
		return;
	}
	
}

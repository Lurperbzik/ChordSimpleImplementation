package peer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.HashMap;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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
	
	NodoInfo nodo = new NodoInfo();
	NodoInfo predecesor = new NodoInfo();
	ArrayList<NodoInfo> sucesores = new ArrayList<NodoInfo>();
	HashMap <Integer, String> recursos;
	private Client client = ClientBuilder.newClient();

	
	public Peer() throws UnknownHostException {
		String[] data = InetAddress.getLocalHost().toString().split("/");
		this.nodo.ip = data[data.length - 1];
		System.out.println("IP " + this.nodo.ip);
	}
	
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	@Path("hola")
	public String hola() {
		return "Hello";
	}
	@POST
	@Path("join")
	@Consumes(MediaType.TEXT_PLAIN)
	public void join(String url) {
		System.out.println(url);
		AnilloInfo result = client.target(url)
				.path("Anillo")
				.request(MediaType.APPLICATION_JSON)
	            .get(AnilloInfo.class);
		
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
			sucesores.add(nuevoSucesor);
		}
	}
	
	public void creaAnillo() {
		this.sucesores.add(this.nodo);
	}
	
	@GET
	@Path("find_sucesor")
	@Produces(MediaType.APPLICATION_JSON)
	public NodoInfo findSucesor(@QueryParam("id") Integer id) {
		System.out.println("Find sucesor");
		
		if (this.nodo.id == this.sucesores.get(0).id) {
			return sucesores.get(0);
		}
		if (id > this.nodo.id && id <= this.sucesores.get(0).id) {
			return sucesores.get(0);
		}
		
		if(this.sucesores.get(0).id < this.nodo.id) {
			if (id > this.nodo.id && id <= this.sucesores.get(0).id + Math.pow(2, tamanoAnillo)) {
				return sucesores.get(0);
			}
		}
		
		String uri = "http://" + sucesores.get(0).ip + ":8080/ChordSimpleImplementation/nodo";
		return client.target(uri)
		             .path("find_sucesor")
		             .queryParam("id", id)
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
	}

	
}

package peer;

import java.util.ArrayList;
import java.util.HashMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;



@Path("nodo")
public class Peer {
	
	 public static class AnilloInfo {
	        public String ipEntrada;
	        public Integer tamanoAnillo;
	    }

	int tamanoAnillo;
	int numSucesores = 2;
	public class NodoInfo {
		public int id;
		public String ip;	
		
		public void hashIP() {
			//this.id = this.ip(hashed) ;
		}
	}
	NodoInfo nodo = new NodoInfo();
	NodoInfo predecesor = new NodoInfo();
	ArrayList<NodoInfo> sucesores = new ArrayList<NodoInfo>();
	HashMap <Integer, String> recursos;
	private Client client = ClientBuilder.newClient();

	
	@POST
	@Path("join")
	@Consumes(MediaType.TEXT_PLAIN)
	public void join(String url) {
		AnilloInfo result = client.target(url)
				.path("Anillo")
				.request(MediaType.APPLICATION_JSON)
	            .get(AnilloInfo.class);
		
		tamanoAnillo = result.tamanoAnillo;

		String uri = "http://" + result.ipEntrada + ":8080/nodo";
		NodoInfo nuevoSucesor = client.target(uri)
		             .path("find_sucesor")
		             .queryParam("id", nodo.id)
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
		sucesores.add(nuevoSucesor);
		
	}
	
	@GET
	@Path("find_sucesor")
	@Produces(MediaType.APPLICATION_JSON)
	public NodoInfo findSucesor(@QueryParam("id") Integer id) {
		if (id < this.nodo.id && id >= sucesores.get(0).id) {
			return sucesores.get(0);
		}
		String uri = "http://" + sucesores.get(0).ip + ":8080/nodo";
		return client.target(uri)
		             .path("find_sucesor")
		             .queryParam("id", id)
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
	}

	
}

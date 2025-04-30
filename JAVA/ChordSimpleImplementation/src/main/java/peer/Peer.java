package peer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.codec.digest.DigestUtils;
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
	public class NodoInfo {
		public int id;
		public String ip;	
		
		public void hashIP() {
			byte[] hash = DigestUtils.sha1(this.ip);
			BigInteger bigInt = new BigInteger(1, hash);
			BigInteger modulo = BigInteger.ONE.shiftLeft(tamanoAnillo);  
	        BigInteger result = bigInt.mod(modulo);

	        this.id =  (int) result.longValue(); 
		}
	}
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
	
	@POST
	@Path("join")
	@Consumes(MediaType.TEXT_PLAIN)
	public void join(String url) {
		AnilloInfo result = client.target(url)
				.path("Anillo")
				.request(MediaType.APPLICATION_JSON)
	            .get(AnilloInfo.class);
		
		tamanoAnillo = result.tamanoAnillo;
		
		this.nodo.hashIP();
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
		// TODO despues del 255 va el 0
		if (id > this.nodo.id && id <= sucesores.get(0).id) {
			return sucesores.get(0);
		}
		String uri = "http://" + sucesores.get(0).ip + ":8080/ChordSimpleImplementation/nodo";
		return client.target(uri)
		             .path("find_sucesor")
		             .queryParam("id", id)
		             .request(MediaType.APPLICATION_JSON)
		             .get(NodoInfo.class);
	}

	
}

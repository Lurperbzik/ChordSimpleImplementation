package peer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
	public NodoInfo nodo = new NodoInfo();
	NodoInfo predecesor = new NodoInfo();
	NodoInfo sucesor = new NodoInfo();
	HashMap <Integer, String> recursos = new HashMap<Integer, String>();
	private Client client = ClientBuilder.newClient();
	private Child child;
	
	
	public Peer() throws UnknownHostException {
		String[] data = InetAddress.getLocalHost().toString().split("/");
		this.nodo.ip = data[data.length - 1];
		this.child = new Child(this.nodo);
		this.child.start();
	}

  
	@POST 
	@Path("alta")
	@Consumes(MediaType.APPLICATION_JSON)
	public void alta(String nombreRecurso) {
		// hashear el recurso
		byte[] hash = DigestUtils.sha1(nombreRecurso);
		BigInteger bigInt = new BigInteger(1, hash);
		BigInteger modulo = BigInteger.ONE.shiftLeft(tamanoAnillo);  
        BigInteger result = bigInt.mod(modulo);

        Integer idRec =  (int) result.longValue();
        System.out.println(idRec);
        NodoInfo nodoEncargado = this.findSucesor(idRec);
        if (nodoEncargado.id == this.nodo.id) {
        	// subimos el recurso
        	recursos.put(idRec, nombreRecurso);
        	
        	HashMap<String, String> datosEnvio = new HashMap<String, String>();
        	datosEnvio.put("id", idRec.toString());
        	datosEnvio.put("name", nombreRecurso);
        	datosEnvio.put("hostId", this.nodo.id + "");
        	// mandar mensaje al servidor central
			client.target(uriNodoEntrada)
		             .path("recursos")
		             .request(MediaType.APPLICATION_JSON)
		             .post(Entity.entity(datosEnvio, MediaType.APPLICATION_JSON));
        	
        } else {
        	// llamamos subida de otro
        	String uri = "http://" + nodoEncargado.ip + ":8080/ChordSimpleImplementation/nodo";
			client.target(uri)
	             .path("subida")
	             .request(MediaType.APPLICATION_JSON)
	             .post(Entity.entity(nombreRecurso, MediaType.APPLICATION_JSON));
        }
        
	}
	
	@DELETE
	@Path("baja")
	@Consumes(MediaType.APPLICATION_JSON)
	public String baja(String nombreRecurso) {
		for (Entry<Integer, String> entry : recursos.entrySet()) {
			if (entry.getValue().equals(nombreRecurso)) {
				Integer id = entry.getKey();

				String result = client.target(uriNodoEntrada)
					.path("recursos")
					.queryParam("id", id.toString())
					.request(MediaType.APPLICATION_JSON)
					.delete(String.class);

				return result;
			}
		}
		return "Recurso no disponible";
	}
	
	@GET 
	@Path("descarga")
	@Produces(MediaType.APPLICATION_JSON)
	public Recurso descarga(@QueryParam("idRec") int idRec) {
	    NodoInfo nodoEncargado = this.findSucesor(idRec);

	    if (nodoEncargado.id == this.nodo.id) {
	        for (Entry<Integer, String> entry : recursos.entrySet()) {
	            if (entry.getKey().equals(idRec)) {
	                return new Recurso(entry.getKey(), entry.getValue(), this.nodo.id+"");
	            }
	        }
	        return null;
	    } else {
	        // Consultamos al nodo encargado
	        String uri = "http://" + nodoEncargado.ip + ":8080/ChordSimpleImplementation/nodo";
	        Recurso recurso = client.target(uri)
	            .path("descarga")
	            .queryParam("idRec", idRec)
	            .request(MediaType.APPLICATION_JSON)
	            .get(Recurso.class);
	        
	        return recurso;
	    }
	}
	
	@GET 
	@Produces(MediaType.TEXT_PLAIN)
	@Path("status")
	public String getSucesor() {
		return "Id: " + this.nodo.id + " Ip: " + this.nodo.ip +  " Id Sucesor: " + this.sucesor.id +  " Id Predecesor: " + this.predecesor.id;
	}
	
	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("join")
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
	@Path("check_predecesor")
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
		
		
		
		if( this.nodo.id > this.sucesor.id) {
			// id id > n 
			if(id > this.nodo.id) {
				return sucesor;
			}
			else if(id < this.sucesor.id){
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

package peer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Singleton
@Path("nodo")
public class Peer {
	
	 public static class AnilloInfo {
	        public String ipEntrada;
	        public Integer tamanoAnillo;
	    }

	int tamanoAnillo = 6;
	int numSucesores = 2;
	
	public NodoInfo nodo = new NodoInfo();
	NodoInfo predecesor = new NodoInfo();
	NodoInfo sucesor = new NodoInfo();
	HashMap <Integer, String> recursos = new HashMap<Integer, String>();
	ArrayList<Recurso> recursosLocales = new ArrayList<Recurso>();
	private Client client = ClientBuilder.newClient();
	private String ipNodoEntrada;
	
	
	public Peer() {
		
	}

	private int hashRecurso(String data) {
		byte[] hash = DigestUtils.sha1(data);
		BigInteger bigInt = new BigInteger(1, hash);
		BigInteger modulo = BigInteger.ONE.shiftLeft(tamanoAnillo);  
        BigInteger result = bigInt.mod(modulo);
        return (int)result.longValue();
	}
  
	@POST 
	@Path("alta")
	@Consumes(MediaType.APPLICATION_JSON)
	public String alta(String nombreRecurso) {
        /*
         * Recibe el nombre de una IMAGEN
         * El host que recibe el nombre debe tener la imagen en local
         * Si la tine dos opciones: 
         * 	- Le toca exponer ese recurso
         *  - Comparte ese recurso con el nodo indicado y lo elimina de su host
         * 
         * Si NO la tiene devuleve ERROR
         * 
         * */
		
        Integer idRec =  this.hashRecurso(nombreRecurso);

		// Comprobamos que tiene esa imagen en local
		Recurso existe = new Recurso(idRec, nombreRecurso, this.nodo.id);
		String imageBase64 = existe.loadFromDisk();
		
		if (imageBase64 == null) {
			return "Recurso no disponible en este host";
		}
		
        NodoInfo nodoEncargado = this.findSucesor(idRec);
        
        if (nodoEncargado.id == this.nodo.id) {
        	// subimos el recurso
        	recursosLocales.add(existe);
			return "Recurso dado de alta en nodo con ip: " + this.nodo.ip;
        } else {
        	
        	// Eliminamos la imagen de forma local
        	existe.deleteImage();
        	
        	// Transferimos la imagen al otro host
        	String uri =  "http://" + nodoEncargado.ip + ":8080/ChordSimpleImplementation/nodo";
        	
        	Form form = new Form();
        	form.param("name", nombreRecurso);
        	form.param("image", imageBase64);
        	form.param("imageId", idRec.toString());
        	
        	Response response = client.target(uri)
        		.path("saveImage")
        		.request(MediaType.TEXT_PLAIN)
        		.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        	
        	String resultText = response.readEntity(String.class);
            boolean success = Boolean.parseBoolean(resultText.trim());
            response.close();
            
            if(!success) {
            	return "Error al transferir archivo a host con IP: " + nodoEncargado.ip;
            }
            
        	uri = "http://" + nodoEncargado.ip + ":8080/ChordSimpleImplementation/nodo";
			response = client.target(uri)
	             .path("alta")
	             .request(MediaType.APPLICATION_JSON)
	             .post(Entity.entity(nombreRecurso, MediaType.APPLICATION_JSON));
			return response.readEntity(String.class);
        }
        
	}
	
	@DELETE
	@Path("baja/{nombreRecurso}")
	@Consumes(MediaType.TEXT_PLAIN)
	public String baja(@PathParam("nombreRecurso") String nombreRecurso) {

		boolean removed = recursosLocales.removeIf((rec -> rec.name.equals(nombreRecurso)));
		if(!removed) {
			return "Recurso no disponible en host con IP: " + this.nodo.ip;
		}
		return "Recurso eliminado en host con IP: " + this.nodo.ip;
	}
	
	@GET 
	@Path("descarga")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] descarga(@QueryParam("nombreRecurso") String nombreRecurso) {
		System.out.println(nombreRecurso);
        Integer idRec =  this.hashRecurso(nombreRecurso);
	    NodoInfo nodoEncargado = this.findSucesor(idRec);
	    
	    if (nodoEncargado.id == this.nodo.id) {
	        for (Recurso rec: recursosLocales) {
	        	if(rec.name.equals(nombreRecurso)) {
	        		return rec.loadBytesFromDisk();
	        	}
	        }
	        return null;
	    } else {
	        // Consultamos al nodo encargado
	        String uri = "http://" + nodoEncargado.ip + ":8080/ChordSimpleImplementation/nodo";
	        byte[] image = client.target(uri)
	            .path("descarga")
	            .queryParam("nombreRecurso", nombreRecurso)
	            .request(MediaType.APPLICATION_OCTET_STREAM)
	            .get(byte[].class);
	        System.out.println(image.length);
	        return image;
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
	public void join(String ipNodoEntrada) {
		System.out.println("JOIN");
		this.ipNodoEntrada = ipNodoEntrada;
		this.nodo.hashIP();
		System.out.println(this.nodo.id);
		if (this.nodo.ip.equals(ipNodoEntrada)) {
			this.creaAnillo();
		}
		else {
			String uri = "http://" + ipNodoEntrada + ":8080/ChordSimpleImplementation/nodo";
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
			this.join(ipNodoEntrada);
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
	
	@GET
	@Path("getImage")
	public String getImage(@QueryParam("name") String name) {
		for (Recurso rec: recursosLocales) {
			if(rec.name.equals(name)) {
				return rec.loadFromDisk();
			}
		}
		return null;
	}
	
	@POST 
	@Path("saveImage")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public boolean saveImage(@FormParam("name") String name, @FormParam("image") String imageBase64, @FormParam("imageId") int imageId) {
		Recurso rec = new Recurso(imageId, name, this.nodo.id);
		return rec.saveToDisk(imageBase64);
	}
	
}

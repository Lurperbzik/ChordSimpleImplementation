package peer;

public class Recurso {
	public int id;
	public String name;
	public String hostId;
	
	public Recurso() {
		
	}

    public Recurso(Integer id, String name, String hostId) {
        this.id = id;
        this.name = name;
        this.hostId = hostId;
    }
}

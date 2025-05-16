package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;


public class Recurso {
	public int id;
	public String name;
	public int hostId;
	private String path= System.getProperty("user.home") + "/Escritorio/Images";
	
	public Recurso() {
		
	}

    public Recurso(Integer id, String name, int hostId) {
        this.id = id;
        this.name = name;
        this.hostId = hostId;
    }
    
    public byte[] loadBytesFromDisk() {
    	File image = new File(path + "/" + this.name);
    	FileInputStream f = null;
    	byte[] imageBytes = null;
    	
		try {
			f = new FileInputStream(image);
		} catch (FileNotFoundException e) {
			return null;
		}
		try {
			imageBytes = f.readAllBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	try {
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return imageBytes;
    }
    public String loadFromDisk() {
    	File image = new File(path + "/" + this.name);
    	FileInputStream f = null;
    	byte[] imageBytes = null;
    	
		try {
			f = new FileInputStream(image);
		} catch (FileNotFoundException e) {
			return null;
		}
		try {
			imageBytes = f.readAllBytes();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	try {
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	if (imageBytes == null) {
    		return null;
    	}
    	return Base64.getEncoder().encodeToString(imageBytes);
    }
    
    public boolean saveToDisk(String base64Image) {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        String path = this.path + "/"  + this.name;

        try (OutputStream out = new FileOutputStream(new File(path))) {
            out.write(imageBytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteImage() {
    	String path = this.path + "/" + this.name;
    	File image = new File(path);
    	return image.delete();
    }
}

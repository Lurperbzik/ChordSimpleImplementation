package peer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.codec.digest.DigestUtils;

public class NodoInfo {
	public int id;
	public String ip;
	public int tamanoAnillo = 6;
	
	public NodoInfo() {
		this.id = -1;
		
		Enumeration<?> e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements())
			{
			    NetworkInterface n = (NetworkInterface) e.nextElement();
			    Enumeration<?> ee = n.getInetAddresses();
			    while (ee.hasMoreElements())
			    {
			        InetAddress i = (InetAddress) ee.nextElement();
			        if (i.getHostAddress().matches("^192\\.168\\..*") || i.getHostAddress().matches("^172\\.20\\..*")) {
			        	this.ip = i.getHostAddress();
			        	return;
			        }
			    }
			}
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
	}
	
	
	public void hashIP() {
		byte[] hash = DigestUtils.sha1(this.ip);
		BigInteger bigInt = new BigInteger(1, hash);
		BigInteger modulo = BigInteger.ONE.shiftLeft(tamanoAnillo);  
        BigInteger result = bigInt.mod(modulo);

        this.id =  (int) result.longValue(); 
	}
	
}
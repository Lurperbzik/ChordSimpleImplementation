package peer;

import java.math.BigInteger;

import org.apache.commons.codec.digest.DigestUtils;

public class NodoInfo {
	public int id;
	public String ip;
	public int tamanoAnillo;
	
	public NodoInfo() {
		// pa que no se queje jakarta
	}
	
	public void hashIP() {
		byte[] hash = DigestUtils.sha1(this.ip);
		BigInteger bigInt = new BigInteger(1, hash);
		BigInteger modulo = BigInteger.ONE.shiftLeft(tamanoAnillo);  
        BigInteger result = bigInt.mod(modulo);

        this.id =  (int) result.longValue(); 
	}
}
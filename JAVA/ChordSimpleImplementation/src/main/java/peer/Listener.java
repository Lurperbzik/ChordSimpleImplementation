package peer;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class Listener implements ServletContextListener{

	@Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Servidor iniciado. Ejecutando método...");
        Child child = new Child(); 
        child.start();
    }
	
	@Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}

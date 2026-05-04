package services;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.nio.file.*;
import java.util.List;
import java.util.Scanner;


//LA INTERFAZZZZZZZZZZZZZZZZZ

@Path("Cliente") //cliente tiene que ser una clase normal para que pueda ejecutar consola unu
public class Cliente {
	//Le da la info a los 3 servidores sobre, con quien hablan (procesos), quienes son (servidores)
	private int i;
	private boolean cola;
	private Client clienteRest = ClientBuilder.newClient(); 							// creamos el cliente HTTP para hacer peticiones
    URI uri=UriBuilder.fromUri("http://localhost:8080/PBFT/rest/Servicio").build();		// url del servicio REST que quiero acceder
    WebTarget target = clienteRest.target(uri);											// atamos a la web. target es el endpoint al que hacer peticiones
    WebTarget servicios[]= new WebTarget[3];											// creamos array de webtargets
    
	
	//Varios server
	//ArrayList<Client> clientes = new ArrayList<Client>();
	//List<String> todas = Files.readAllLines(Paths.get("config.txt"));
    //String[] url = new String[3];
	//ArrayList<String> urls= new ArrayList<String>();
	/*for (i=0 ; i< 3 ;i++ ) {
		//.txt
		url[i] = todas.get(i);
		clientes.add( ClientBuilder.newClient());
		URI uri=UriBuilder.fromUri(url[i]).build(); //urlServer1, urlServer2, urlServer2
		servicios[i]=clienteRest.target(uri);
		
	}
	Y luego se lo mandariamos al cada uno, en el .txt este sería el formato
	
	1;http://localhost:8080/procesos/rest/Servicio
	2;http://localhost:8080/procesos/rest/Servicio
	3;http://localhost:8080/procesos/rest/Servicio
	
	Las url para las comunicaciones y usando el como separador el ";" para indicar que procesos se encarga num=x, Servidorx se encarga de x y x+1
	*/
    
    public static void main(String[] args) {
        try {
            Cliente c = new Cliente();
            c.iniciar(); // Esto activa el Scanner y el bucle del teclado
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void iniciar() {  
        menuInteractivo();			// Aquí lanzas el bucle del teclado
    }
    
    
    public void menuInteractivo() {
        Scanner sc = new Scanner(System.in);
        String comando;
        
        String N, X, res1;
        int propuesta, fallo;
        
        System.out.println("Bienvenido al servicio PBFT");
        System.out.println("Para poder continuar por favor introduzca un comando:");
            System.out.println("Si necesita ayuda pulse h.");
            System.out.println("");
            
        while (true) {
        	System.out.print("> ");
            comando = sc.nextLine().toLowerCase();
            
            if (comando.startsWith("s") && comando.length()<2) {
            	res1 = target.path("info").request(MediaType.TEXT_PLAIN).get(String.class);
            	System.out.println(res1);
            } else if (comando.startsWith("h") && comando.length()==1) {
            	System.out.println("Comandos disponibles: sX (propuesta), fN (fallos), s (estado), exit");
            } else if (comando.startsWith("s")&& comando.length()<=3) {
            	X = comando.substring(1);																							//obtenemos propuesta s(X)
            	propuesta = Integer.parseInt(X);
                res1 = target.path("inicio").queryParam("propuesta", propuesta).request(MediaType.TEXT_PLAIN).get(String.class); 	//pasamos propuesta para cada proceso    
                /*
                  Dos opciones
                  En ambos casos le pasamos el .txt a todos, para que saquen su info, junto con su número, para que se reconozcan     
                  Si solo hay uno que se compare con la URL del cliente y envie el resto
                 */        	
            } else if (comando.startsWith("f")){
            	N = comando.substring(1);																							//obtenemos fallo f(N)
            	fallo = Integer.parseInt(N);
            	res1 = target.path("inicio").queryParam("fallo", fallo).request(MediaType.TEXT_PLAIN).get(String.class);			//pasamos fallo para cada proceso
            } else{
            	break;
            }   
            // ... añadir más comandos fN, etc.
        }
        sc.close();
        System.out.println("");
        System.out.println("El servicio ha terminado correctamente. Gracias por su participación");
    }
    
    @GET
    @Path("final")
    @Produces(MediaType.TEXT_PLAIN)
    public synchronized void fin(@QueryParam("array") int propuesta[],@QueryParam("numProc") int numProc/*@QueryParam("id") int id */) {
    		//Hay dos  formas
    	
    		//Se lo envia un solo proceso\\ El array ya esta montado, en ese caso, mira por quorum
    		/*
    		 for (int i=0, i<numProc; i++){
    		 	comprobar=compromisos[i];
				quorum=0;
				if (comprobar==0)
					continue;
				for(int j=0;j<numProcesos;j++) {
					if(compromisos[j]==0)
							continue;
						else if (compromisos[j]==comprobar) 
							quorum++;
				}
				if(quorum>=4) {
					//Llamada al srver
					System.out.printf("ACABOSE, el quorum es: %d" comprobar );	
				}
    		 }
    		 */
    	
    		//O no hay server centralizado, por lo cual funciona igual que los métodos quorum de los procesos
    	
    		/*
    		 	int quorum=0;
				int comprobar=0;
				if(compromisos[id]==0)
					compromisos[id]=propuesta;
				
				for(int i=0;i<numProcesos;i++)
				{
					comprobar=compromisos[i];
					quorum=0;
					if (comprobar==0)
						continue;
					for(int j=0;j<numProcesos;j++) {
						if(compromisos[j]==0)
							continue;
						else if (compromisos[j]==comprobar) 
							quorum++;
					}
					if(quorum>=4) {
						//Llamada al srver
						System.out.printf("ACABOSE, el quorum es: %d" comprobar );
					}
				}
    		 */
    		
    	}


}




//QUIERO HACER ESTO--> Server 1: Entendido --> Servers 2, 3: Entendido--> Se ponen



//QUIERO HACER ESTO--> Server 1,2,3: OKEI--> Cliente de vuelta
//Como sincronizamos

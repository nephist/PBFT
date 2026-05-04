package services;

import javax.inject.Singleton;
import java.net.URI;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import java.util.ArrayList;
import java.util.List;


//ESTA ES LA CARRERA
@Singleton
@Path("Servicio")
public class Servicio {
	
	//Esto no existirá se hara con el .txt en Servicio
	private ArrayList<Proceso> miProcesos = new ArrayList<Proceso>();
	String url="http://localhost:8080/PBFT/rest/Servicio";
	WebTarget servicios[]= new WebTarget[2];
	ArrayList<Client> clientes = new ArrayList<Client>();
	String[] urls = new String[2];

	public Servicio() {
		int i;
		for (i=0; i<6; i++) {
			miProcesos.add(new Proceso(i, false, url)); 
		}
		/*
		for (int i; i<2; i++) {
			clientes.add( ClientBuilder.newClient());
			URI uri=UriBuilder.fromUri(urls[i]).build(); //urlServer1, urlServer2, urlServer2
			servicios[i]=clienteRest.target(uri);
		}
		*/
        
    }
	
	@GET
    @Path("inicio")
    @Produces(MediaType.TEXT_PLAIN)
	public String inicio(@QueryParam("propuesta")int propuesta) {
		String s="propuesta: "+propuesta;
		int i;
		for(Proceso proc : miProcesos) {
            proc.propuesta(propuesta);
        }
		return s;
    }
	
	
	//pquorum
	@GET
	@Path("compromiso")
	@Produces(MediaType.TEXT_PLAIN)
	public String pquorum(@QueryParam("vCompromiso") int vCompromiso, @QueryParam("id") int id) {		
		String s="compromiso: "+vCompromiso+"id:" +id ;
		for(Proceso proc : miProcesos) {		//obviamos el proceso que llama a la funcion. asi eliminamod un bucle de cada proceso
			if(proc.getIdProceso()!= id) {
	            proc.compromiso(vCompromiso, id);
			}
        }
		return s;
		//En cada uno de estos hacer una llamada a los otros servidores
	}
	
	@GET
    @Path("comision")
    @Produces(MediaType.TEXT_PLAIN)
	public String comision(@QueryParam("vComision") int vComision, @QueryParam("id") int id) {
		String s="comision: "+vComision+"id:" +id ;
		for(Proceso proc : miProcesos) {
			if(proc.getIdProceso()!= id) {
				proc.comision(vComision, id);
			}
	    }
		return s;
		// a otros servidores
	}
	
	
	@GET
    @Path("confirmacion")
    @Produces(MediaType.TEXT_PLAIN)
	public String confirmacion(@QueryParam("vConfirmacion") int vConfirmacion, @QueryParam("id") int id) {
		String s="comision: "+vConfirmacion+"id:" +id ;
		return s;

		//Creo que es asi
		//Hay dos opciones
		//Se lo envia todo uno, buscan la url del server central y le envia la info
		
		//Server central:
		/*
		  for()
		  	Comprueba que no haya 0 si no los hoy lo madanda, si lo hay es que falta un server por mandar toda su info
		 */
		
		//Si no hay central
		/*
		  Hacemos como los procesos le hacian a los servicios, le envian al cliente lo que tienen, y el cliente mira si hay quorum cada vez
		 */
	}
	
	@GET
    @Path("info")
    @Produces(MediaType.TEXT_PLAIN)
	public synchronized String info() {
		String s="s";
		
		return s;
		
    }
	
	public synchronized  String cambio() {
		String s="s";
		
		return s;
	}
	

}

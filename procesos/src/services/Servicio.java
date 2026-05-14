package services;

import javax.inject.Singleton;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.util.List;

import java.util.ArrayList;
import java.util.Collections;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

//ESTA ES LA CARRERA
@Singleton
@Path("Servicio")
public class Servicio {
	
	//Esto no existirá se hara con el .txt en Servicio
	private ArrayList<Proceso> misProcesos = new ArrayList<Proceso>();
	private Client clienteRest = ClientBuilder.newClient();
	
	private int id;
	private String ip;
	private String ipCliente;
	private String uriServer;
	private int totalProcesos;
	private int numProcesos;
	private int numServidores;
	private String cadenaProcesos;

	String url="http://localhost:8080/procesos/rest/Cliente";
	URI uriCliente;
    WebTarget miServicio;
    List <WebTarget> Servicios = new ArrayList<>();
	String RUTA = System.getProperty("user.home");
	private List<Integer> valoresRecibidos = Collections.synchronizedList(new ArrayList<>());
	
	private int procesosFinalizados = 0;
	private final Object candadoConsenso = new Object();

	public Servicio() throws IOException {					//al iniciar servicio primero mandaremos la ip al cliente
		try {												
			this.ip = InetAddress.getLocalHost().getHostAddress();
			System.out.println("IP server:"+ip);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
    }
	
	@GET
    @Path("configuracion")
	public synchronized void configuracion(@QueryParam("ips") String ipServers, @QueryParam("cadenaProcesos") String cadenaProcesos, @QueryParam("totalProcesos") int totalProcesos,  @QueryParam("ipServicio") String ipServicio) {
		String[] partes0, partes, partes1, partes2;	
		
		/*
		 * 	con total procesos pasamos todos los procesos
		 * 	con ipServers pillamos las ips que van concatenadas y las metemos en el array partes
		 * 	sacamos cantidad de servidores con partes.length
		 * 	sacamos nuestra ip
		 */
		partes0 =ipServicio.split("/");
		this.id=Integer.parseInt(partes0[1]);
		this.uriServer="http://"+partes0[0]+":8080/procesos/rest/Servicio";								//uris para comunicarse con sus procesos
		miServicio=clienteRest.target(uriServer);
		System.out.println("MI id["+id+"] MI uri:"+uriServer);
		partes = ipServers.split(",");
		this.numServidores = partes.length;
		this.totalProcesos = totalProcesos;
		for(int i=0;i<partes.length;i++) 
		{
			if(i!=id) {
				int idServer=i;
				Servicios.add(clienteRest.target("http://"+partes[i]+":8080/procesos/rest/Servicio"));		//uris para comunicarse con otros servicios
				System.out.println("id["+idServer+"] ip:"+partes[i]);
			}
		}		
	    
	    /*	basicamente en cliente dividimos los procesos de forma equitativa y lo convertimos en una cadena de numeros
	     * 	ej: 6 proceso 4 servidores -> listaProcesos[4]= {2,2,1,1}  -> "2,2,1,1"
	     * 	con el id (1/2/3/4)- 1 pillamos el que es nuestro                ^
	     */
		/* esta cambiado, ahora cadena procesos tiene "1,5;2,6;3;4" siendo 1,5 lo que tenemos que pillar */
		
		partes1 = cadenaProcesos.split(";");
		partes2 = partes1[id].split(",");
		this.numProcesos = partes2.length;
		
	    //creamos nuestros procesos
		for (int i=0; i<numProcesos; i++) {
			System.out.println("proceso "+partes2[i]+"creado");
			misProcesos.add(new Proceso(Integer.parseInt(partes2[i]), false, uriServer, totalProcesos)); 												//se lo enviamos a los procesos
		}
	}


	@GET
    @Path("inicio")
    @Produces(MediaType.TEXT_PLAIN)							//aqui no se comunican con otros servicios
	public String inicio(@QueryParam("propuesta") int propuesta) {
		synchronized (candadoConsenso) {
	        valoresRecibidos.clear();
	        procesosFinalizados = 0;
	    }
		for (Proceso proc : misProcesos) {
	        proc.propuesta(propuesta);
	    }//Para quitar problemas de rondas y tal
	    for (Proceso proc : misProcesos) {
	        proc.enviar();
	    }
	    synchronized (candadoConsenso) {
	        try {
	            candadoConsenso.wait(5000);
	        } catch (InterruptedException e) { e.printStackTrace(); }
	    }
        return valoresRecibidos.toString(); // Enviará algo como "[10, 10]"
    }
	
	
	//pquorum
	@GET
	@Path("compromiso")
	@Produces(MediaType.TEXT_PLAIN)
	public void pquorum(@QueryParam("propuesta") int vCompromiso, @QueryParam("pid") int pid, @QueryParam("propagado") boolean propagado) {
		//mandamos a nuestros procesos
		for(Proceso proc : misProcesos) {					//obviamos el proceso que llama a la funcion. asi eliminamod un bucle de cada proceso
			if(proc.getIdProceso()!= pid) {
	            proc.compromiso(vCompromiso, pid);
			}
        }
		//mandamos a los otros servers
		for (WebTarget s : Servicios)
		{
			if(propagado)	//es basicamente comprobar si la uri del web target del for es distinta a la uri del servicio
			{
				s.path("compromiso").queryParam("propuesta", vCompromiso).queryParam("pid", pid).queryParam("propagado", false).request().async().get();
				System.out.println("mando compromiso con id"+pid);
			}	 
		}
	}
	
	@GET
    @Path("comision")
    @Produces(MediaType.TEXT_PLAIN)
	public void comision(@QueryParam("compromiso") int vComision, @QueryParam("pid") int pid, @QueryParam("propagado") boolean propagado) {
		//Creo que es asi
		for(Proceso proc : misProcesos) {				//obviamos el proceso que llama a la funcion. asi eliminamod un bucle de cada proceso
			if(proc.getIdProceso()!= pid) {
	            proc.comision(vComision, pid);
			}
        }
		//mandamos a los otros servers
		for (WebTarget s : Servicios)
		{
			if(!propagado)	//es basicamente comprobar si la uri del web target del for es distinta a la uri del servicio
			{
				s.path("comision").queryParam("compromiso", vComision).queryParam("pid", pid).queryParam("propagado", true).request().async().get();
			}
		}
	}
	
	
	@GET
    @Path("confirmacion")
    @Produces(MediaType.TEXT_PLAIN)
	public void confirmacion(@QueryParam("comision") int comision, @QueryParam("pid") int pid, @QueryParam("propagado") boolean propagado) {
		//Creo que es asi
		//Hay dos opciones
		//Se lo envia todo uno, buscan la url del server central y le envia la info
		
		//Server central:
		/*
		  for()
		  	Comprueba que no haya 0 si no los hoy lo madanda, si lo hay es que falta un server por mandar toda su info
		 
		
		//Si no hay central
		
		  Hacemos como los procesos le hacian a los servicios, le envian al cliente lo que tienen, y el cliente mira si hay quorum cada vez
		 */
		for (WebTarget s : Servicios) {
	        if(!propagado) {
	            s.path("confirmacion")
	                .queryParam("comision", comision)
	                .queryParam("pid", pid)
	                .queryParam("propagado", true)
	                .request().async().get();
	        }
	    }
		synchronized(candadoConsenso) {
	        // Guardamos el valor que nos acaba de llegar por el túnel REST
	        valoresRecibidos.add(comision);
	        procesosFinalizados++;

	        // Si ya han llamado todos los procesos que este servidor gestiona
	        if (procesosFinalizados == misProcesos.size()) {
	            candadoConsenso.notify(); // Despertamos al hilo que está esperando para hacer el return
	        }
	    }
	}
	

	@GET
    @Path("estado")
    @Produces(MediaType.TEXT_PLAIN)
	public String estado() {
		/*	por cada proceso que nos envía una string del tipo "2/1/1,1,8/false"
		 * 	los separamos por ";"
		 * 		EJEMPLO:
		 * 			3 procesos
		 * 			s= "0/1/1,1,8/false;1/8/8,1,1/true;2/1/1,8,1/false"
		 */
		String sEstado=null;
		String aux;
		for (Proceso proc : misProcesos)
		{
			aux=proc.estado();
			if(sEstado == null) {
                sEstado= aux;
            } else {
                sEstado=sEstado+";"+aux;
            }
		}
		System.out.println();
		System.out.println(sEstado);
		System.out.println();
		return sEstado;
    }

	@GET
    @Path("fallo")
	public String fallo(@QueryParam("pid") int pid) {
		for (Proceso proc : misProcesos)
		{
			if (proc.getIdProceso()==pid)
			{
				if(proc.getError())
				{
					proc.setError(false);
				} else {
					proc.setError(true);
				}
			}
		}
		return "hecho";
	}
}

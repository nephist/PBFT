package services;

import java.net.URI;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;



@Singleton
@Path("Proceso")
public class Proceso {
	
	private int pid, valor, vCompromiso, vComision, vConfirmacion;
	private int procesosCompromiso=0, procesosComision=0;
	private int numProcesos;
	private boolean error;
	WebTarget Server;
	private Client clienteRest = ClientBuilder.newClient();
	URI uri=UriBuilder.fromUri("http://localhost:8080/procesos/rest/Servicio").build(); // Cada proceso tendr� su servidor
	int compromisos[]; 
	int comisiones[];
	boolean compromiso;
	boolean comision;
	
	public Proceso(int pid, boolean error, String url, int numProcesos) {
        this.pid = pid;
        this.error=error;
		this.numProcesos = numProcesos;
		this.compromisos = new int[numProcesos];   
		this.comisiones  = new int[numProcesos];

        URI uri=UriBuilder.fromUri(url).build();
        this.Server = clienteRest.target(uri);
    }
	
	
	/*	los getter y setter	*/
	public int getIdProceso() 
	{
		return this.pid;				
	}
	public boolean getError() 
	{
		return this.error;				
	}
	public void setError(boolean error) 
	{
		this.error=error;				
	}
	
	
	
	
	public synchronized void propuesta (int variable) {
		//Todos deberian ya estar aqui
		procesosCompromiso=0;
		procesosComision=0;
		compromiso=false;
		comision=false;
		for(int i=0; i<numProcesos; i++)
		{
			compromisos[i]=0;
			comisiones[i]=0;
		}
		//Les llega lo del Cliente
		if (error==true ) {
			//Si hay error manda otra cosa
			int random;
			do {
				random = (int)(Math.random() * 201);
			}while(random==variable || random==0);
			compromisos[pid]=random;
			vCompromiso=random;
			procesosCompromiso++;
		}
		else {
			//Si no lo hay les manda el que nos llego
			compromisos[pid]=variable;
			procesosCompromiso++;
			vCompromiso=variable;
		}
		this.valor=vCompromiso;
			
    }
	
	public synchronized void enviar () {
		Server.path("compromiso").queryParam("propuesta", vCompromiso).queryParam("pid", this.pid).queryParam("propagado", true).request().async().get();		
	}
	
	public synchronized void compromiso(int propuesta, int pid) {
		int quorum=0;
		compromisos[pid]=propuesta;
		procesosCompromiso++;
		if(!compromiso)
		{
			if(procesosCompromiso>numProcesos/2)
			{
				for(int i=0;i<numProcesos;i++)
				{
					quorum=0;
					if (compromisos[i]==0)
						continue;
					for(int j=0;j<numProcesos;j++) {
						if(compromisos[j]==0)
							continue;
						else if (compromisos[j]==compromisos[i]) 
							quorum++;
					}
					if(quorum>numProcesos/2) {
						//Llamada al srver
						this.vComision=compromisos[i];
						comisiones[this.pid]=vComision;
						compromiso=true;
						procesosComision++;
						Server.path("comision").queryParam("compromiso", vComision).queryParam("pid", this.pid).queryParam("propagado", false).request().async().get();
						return;		
					}
				}
				if(procesosCompromiso==numProcesos)
			    {
					procesosComision++;
			    	Server.path("comision").queryParam("compromiso", -1).queryParam("pid", this.pid).queryParam("propagado", false).request().async().get();
			    }
			}
		}
	}
		
	public synchronized void comision(int compromiso , int pid) {
		//Les envia al resto el numero que el tiene
		//Y ellos nos lo envian a nosotros
		//Vemos que numero tiene mayoria y lo pasamos a comisiones

	    comisiones[pid] = compromiso;
	    int quorum = 0;
	    procesosComision++;
	    if(!comision) {
		    if(procesosComision>numProcesos/2)
		    {
			    for(int i = 0; i < numProcesos; i++) {
			        if (comisiones[i] == 0) continue;
		
			        quorum = 0;
			        for(int j = 0; j < numProcesos; j++) {
			        	if(comisiones[j]==0)
							continue;
						else if (comisiones[j]==comisiones[i]) 
							quorum++;
			        }
		
			        if(quorum > numProcesos/2) {
						this.vConfirmacion=comisiones[i];
						comision=true;
			        	Server.path("confirmacion").queryParam("comision", vConfirmacion).queryParam("pid", this.pid).queryParam("propagado", false).request().async().get(); 
			            return;
			        }
			    }
			    if(procesosComision==numProcesos)
			    {
			    	Server.path("confirmacion").queryParam("comision", -1).queryParam("pid", this.pid).queryParam("propagado", false).request().async().get();
			    }
		    }
	    }
	}
	public synchronized void confirmacion(int actualizado) {
		
	}
	public synchronized String estado() {
		/*	para mandar el estado mandaremos una string de la siguiente forma
		 * 		id						"2"
		 * 		valor					"1"
		 * 		lista de compromisos 	"1,1,1,8"
		 * 		error 					"false"
		 * 	TODAS SEPARADAS POR "/" EN ESTE EJEMPLO QUEDARiA ALGO ASi "2/1/1,1,1,8/false" 
		 * */
		String aux, sError;
		String sCompromisos=null;
		for(int i =0; i<numProcesos;i++)
		{
			aux=compromisos[i]+"";
			if(sCompromisos == null) {
                sCompromisos= aux;
            } else {
                sCompromisos=sCompromisos+","+aux;
            }
		}
		if (error)				//si error es true
		{
			sError="true ";
		}else
		{
			sError = "false";
		}
		return (pid+"/"+valor+"/"+sCompromisos+"/"+sError);

	}

}
package services;
import java.util.*;

import java.net.URI;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.net.URI;



@Singleton
@Path("Proceso")
public class Proceso extends Thread {
	
	static int numProcesos=6;
	private int id, variable, vCompromiso, vComision, vConfirmacion;
	private int procesosCompromiso=0, procesosComision=0;
	private boolean error;
	WebTarget Server;
	private Client clienteRest = ClientBuilder.newClient();
	URI uri=UriBuilder.fromUri("http://localhost:8080/PBFT/rest/Servicio").build(); // Cada proceso tendrá su servidor
	int compromisos[] = new int[numProcesos];
	int comisiones[] = new int[numProcesos];
	
	public Proceso(int id, boolean error, String url) {
        this.id = id;
        this.error=error;
        URI uri=UriBuilder.fromUri(url).build();
        this.Server = clienteRest.target(uri);
    }
	
	public int getIdProceso() 
	{
		return this.id;				
	}
	
	//valor es el numero recibido de servicio y de cliente para actualizar el valor
	public void propuesta (int valor) {
		//variable=-1;
		//les llega una propuesta del cliente
		if (error==true ){					//Si hay error manda otra cosa
			int random;
			do {
				random =(int)(Math.random() * 201);
			}while(random==valor || random==0);
			compromisos[id]=random;			//registramos nuestro compromiso MAL en un array
			this.vCompromiso=random;				//tenemos que mandar el compromiso MAL a los demas procesos
		}
		else {
			//Si no hay error manda el valor recibido
			compromisos[id]=valor;			//registramos nuestro compromiso en un array
			this.vCompromiso=valor;				//tenemos que mandar el compromiso a los demas procesos
		}
		Server.path("compromiso").queryParam("vCompromiso", vCompromiso).queryParam("id", this.id).request().get();
		System.out.println("soy "+this.id+"me ha llegado"+valor);
    }
	
	public void compromiso(int propuesta, int id) {
		int quorum=0;
		compromisos[id]=propuesta;
		procesosCompromiso++;
		if(procesosCompromiso>numProcesos/2){
			for(int i=0;i<numProcesos;i++){
				quorum=0;
				if (compromisos[i]==0)
					continue;
				for(int j=0;j<numProcesos;j++){
					if(compromisos[j]==0)
						continue;
					else if (compromisos[i]==compromisos[j]) 
						quorum++;
				}
				if(quorum>numProcesos/2) {
					//Llamada al server
					this.vComision=compromisos[i];
					comisiones[this.id]=vComision;
					Server.path("comision").queryParam("vComision", vComision).queryParam("id", this.id).request().get();
					System.out.println("soy "+this.id+"mi comision"+ vComision);
					return;		
				}
			}
		}
	}
	//EN la lista hay numero para el queorum?
		//SI
			//Manda mensaje al server para pasar a la siguiente fase
		//NO
					
	public void comision(int compromiso , int id) {
		//hugo en sus momentos mas lucidos (es tonto el pobre):
			//Les envia al resto el número que el tiene
			//Y ellos nos lo envián a nosotros
			//Vemos que número tiene mayoría y lo pasamos a comisiones
	    int quorum = 0;		
	    comisiones[id] = compromiso;
	    procesosComision++;
	    if (procesosComision>numProcesos/2) {
		    for(int i = 0; i < numProcesos; i++) {
		        if (comisiones[i] == 0) continue;
		        quorum = 0;
		        for(int j = 0; j < numProcesos; j++) {
		        	if(comisiones[j]==0)
						continue;
					else if (comisiones[j]==comisiones[i]) 
						quorum++;
		        }
		        if(quorum >numProcesos/2) {
		        	this.vConfirmacion= comisiones[i];
		        	Server.path("confirmacion").queryParam("vConfirmacion", vConfirmacion).queryParam("id", this.id).request().get(); 
		        	System.out.println("soy "+this.id+"mi confirmacion"+ vConfirmacion);
		        	return;
		        }
		    }
	    }
		int mayoria = 1;
		//comision(mayoria);
	}
	
	/*public void comision(int consenso) {
		//Una vez hemos decidido que "consenso" es nuestro número se lo decimos al resto y actualizamos nuestro valor
		//Y ellos a nosotros
		int actualizado = 2;
		confirmacion(actualizado);
	}*/
	
	public void confirmacion(int actualizado) {
    	System.out.println("se ha actualizado el valor con el siguiente numero"+ actualizado);

	}

}

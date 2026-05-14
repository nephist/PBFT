package services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import java.net.URI;

import java.util.ArrayList;
import java.util.Scanner;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

//@Path("Cliente")
public class Cliente {

    private Client clienteRest = ClientBuilder.newClient();
    URI uri = UriBuilder.fromUri("http://localhost:8080/procesos/rest/Servicio").build();
    WebTarget target = clienteRest.target(uri);
    
    List <WebTarget> servicios = new ArrayList<>();
    List <String> ips = new ArrayList<>();
    
    int servers, procesos, cont=0;
    String RUTA = System.getProperty("user.home");
    
    private ArrayList<String> listaEstados = new ArrayList<>();
    private final ArrayList<String> cola = new ArrayList<>();
    private Object lock = new Object();
    private int finalizados;
    Scanner sc = new Scanner(System.in);
    
    public static void main(String[] args) {
    	Scanner sc = new Scanner(System.in);
        try {
            Cliente c = new Cliente();
            c.iniciar();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sc.close();
    }

    public void iniciar() throws IOException {
    	//antes que nada hacemos una comprobacion
    	crearConfiguración();
    	
        Thread hiloTeclado = new Thread(() -> {
            menuInteractivo();
        });

        Thread hiloProcesador = new Thread(() -> {
            while (true) {
                String comando = null;
                synchronized (cola) {
                    while (cola.isEmpty()) {
                        try {
                            cola.wait(); // espera hasta que haya algo
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    comando = cola.remove(0);
                }

                if (comando.equals("exit")) {
                    System.out.println("Procesador cerrado.");
                    break;
                }
                procesarComando(comando);
            }
        });

        hiloTeclado.start();
        hiloProcesador.start();

        try {
            hiloTeclado.join();
            hiloProcesador.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Cliente finalizado.");
    }

    public void menuInteractivo() {
        String comando;
        System.out.println("");
        System.out.println("");
        while (true) {
            System.out.print("> ");
            comando = sc.nextLine();
            if (comando.startsWith("s") && comando.length() < 2) {
                synchronized (cola) {
                    cola.add(comando);
                    cola.notify();
                }
            } else if (comando.startsWith("h")) {
                System.out.println("Comandos disponibles: sX (propuesta), fN (fallos), s (estado), exit");
            } else if (comando.startsWith("s")) {
                synchronized (cola) {
                    cola.add(comando);
                    cola.notify();
                }
            } else if (comando.startsWith("f")) {
                synchronized (cola) {
                    cola.add(comando);
                    cola.notify();
                }
            } else if (comando.equals("exit")) {
                synchronized (cola) {
                    cola.add("exit");
                    cola.notify();
                }
                break;
            } else {
                System.out.println("Comando desconocido");
            }
        }
    }

    public void procesarComando(String comando) {
        if (comando.equals("s")) {
        	File f = new File(RUTA+"/a.txt");
            //intentamos escribir la ip
            try{
            	BufferedWriter bw = new BufferedWriter(new FileWriter(RUTA+"/a.txt",true));
                
            	listaEstados.clear();
	            String res1;
	            String partes[],partes0[];
	            
	            for(int i=0;i<servers; i++)
	            {
	                /*	obtenemos de cada servicio: "0/1/1,1,8/false;1/8/8,1,1/true;2/1/1,8,1/false"
	                 *	vamos a separar de cada servicio, los procesos que maneja por ";"
	                 * 	a continuación los añadiremos a lista de estados
	                 * 	finalmente se ordenan
	                 */
	            	res1 = target.path("estado").request(MediaType.TEXT_PLAIN).get(String.class);
	                partes0 = res1.split(";");
	                for(int j=0;j<partes0.length;j++)
	                {
	                    listaEstados.add(partes0[j]);
	                } 
	            }
	            /*ordenamos por el id*/
	            Collections.sort(listaEstados, (a, b) -> {          
	                int aux1 = Integer.parseInt(
	                    a.substring(0, a.indexOf('/'))
	                );
	                int aux2 = Integer.parseInt(
	                    b.substring(0, b.indexOf('/'))
	                );
	                return Integer.compare(aux1, aux2);
	            });
	            System.out.printf("---------------------------------");
	            System.out.printf("             PBFT                ");
	            System.out.printf("---------------------------------");
	            System.out.println(" id\t| var\t| compromisos\t| error\t |");
	            /*	AQUI vamos a pillar por separado cada string ordenada de la  listaEstados
	             * 	"0/1/1,1,8/false"
	             * 	separamos toda la info
	             * */
	            for (String s: listaEstados)
	            {
	            	bw.write(s);
	            	
	            	partes = s.split("/");
	                System.out.printf(" %d\t| %s\t| %s\t| %s\t \n", Integer.parseInt(partes[0]), partes[1], partes[2], partes[3]);
	            }
	            System.out.println("---------------------------------");
            } catch (Exception e) {
                e.printStackTrace();
            }	

        } else if (comando.startsWith("s")) {
        	String numero = comando.substring(1);
            int valor = Integer.parseInt(numero);

            try {
                System.out.println("[HiloProcesador] Enviando propuesta al servidor y esperando procesos...");
                //Generar varios hilos para cada servicio, asi no hay espera ocupada 
                List <String> resultados = new ArrayList<>();
                List<Thread> hilos = new ArrayList<>();
                synchronized (lock) {
                    finalizados = 0;
                }
                for (int i = 0; i < servicios.size(); i++) {
                    WebTarget s = servicios.get(i);
                    Thread t = new Thread(() -> {
                        String res = s.path("inicio").queryParam("propuesta", valor).request(MediaType.TEXT_PLAIN).get(String.class);
                        synchronized (lock) {
                            resultados.add(res);
                            finalizados++;
                            if (finalizados == servicios.size()) {
                                lock.notify(); // todos han terminado
                            }
                        }
                    });
                    hilos.add(t);
                    t.start();
                }
                
                synchronized (lock) {
                    if (finalizados < servicios.size()) {
                        lock.wait(10000); // espera máximo 10 segundos
                    }
                }

                if (resultados.isEmpty()) {
                    System.out.println("No se recibió respuesta de ningún servidor.");
                } else {
                    System.out.println("El valor cambiado es: " + resultados.get(0));
                }
/*
                String[] partes = res1.replace("[", "").replace("]", "").replace(" ", "").split(",");
                int[] valores = new int[partes.length];
                for(int i = 0; i < partes.length; i++) {
                    valores[i] = Integer.parseInt(partes[i]);
                }
                if(valores[0]==-1)
                {
                	System.out.println("No ha habido consenso");
                }
                else {
                	int escogido=0;
	                for(int i=0; i<valores.length; i++)
	                {
	                	int quorum=0;
	                	for(int j=0; j<valores.length; j++)
	                	{
	                		if(valores[i]==valores[j]) {
	                			quorum++;
	                		}
	                		if(quorum>(valores.length)/2)
	                		{
	                			escogido= valores[i];
	                			break;
	                		}
	                	}
	                }
	                if(escogido==0)
	                {
	                	System.out.println("No ha habido consenso, me da a mi q es imposible a estas alturas");
	                }
	                else
	                {
	                	System.out.println("El valor cambiado es: "+escogido);
	                }
	                
                }*/

            } catch (Exception e) {
                System.err.println("Error al recibir el return del servidor: " + e.getMessage());
            }

        } else if (comando.startsWith("f")) {
            String numero = comando.substring(1);
            int valor = Integer.parseInt(numero);
            String res1 = target.path("fallo").queryParam("pid", valor).request(MediaType.TEXT_PLAIN).get(String.class);
        }
    }
    public void crearConfiguración() throws IOException{
        String ipServers=null;

        System.out.println("Bienvenido, estimado cliente al servicio PBFT");
        System.out.println("Antes de comenzar por favor indique los siguientes datos");

        System.out.print("Número de procesos: ");
        while (!sc.hasNextInt()) {
            System.out.println("Introduce un número válido:");
        }
        procesos = sc.nextInt();
        sc.nextLine();

        String ip = InetAddress.getLocalHost().getHostAddress();
        
        System.out.println("IP Local: " + ip);
        
         /*	
         * leemos del fichero config_server
         * 	donde aparecen las ip de todos los servicios
         * 	con eso sacamos los webtarget y la cantidad de procesos
         */
        
        try{
            BufferedReader br = new BufferedReader(new FileReader(RUTA+"/config_server.txt"));
            String linea;
			while ((linea = br.readLine()) != null) {
				if(ipServers == null) {
	                ipServers = linea;
	            } else {
	                ipServers += "," + linea;
	            }
				ips.add(linea);
				URI uriServicio = UriBuilder.fromUri("http://"+linea+":8080/procesos/rest/Servicio").build();
                servicios.add(clienteRest.target(uriServicio));
			}
			this.servers= servicios.size();
            br.close();
        } catch (Exception e) {
                e.printStackTrace();
        }
        
        /*
        *   aqui basicamente estamos repartiendo todos los ids de manera equitativa con el %
        *   los ponemos en un array de strings [ej] : [1,4,7] [2,5,8] [3,6,9] para 3 servidores 9 procesos
        *   a continuacion creamos la super duper string que separa:
        *       "," los procesos de un mismo servidor 
        *       ";" los procesos de cada servidor
        *       [ej] : 1,4,7;2,5,8;3,6,9
        */

	    String[] listaProcesos = new String[servers];
        String cadenaProcesos=null;
        for(int i = 0; i < procesos; i++) {
            if(listaProcesos[i%servers] == null) {
                listaProcesos[i%servers] = "" + i;
            } else {
                listaProcesos[i%servers] += "," + i;
            }
        }
        for(int i = 0; i < servers; i++)
        {
            if(cadenaProcesos == null) {
                cadenaProcesos = listaProcesos[i];
            } else {
                cadenaProcesos += ";" + listaProcesos[i];
            }
        }
        /*
         * 	llamamos a los servers y le mandamos
         * 		string ips separadas por "," ejemplo: 127.0.0.1, 127.0.0.2, 127.0.0.3
         * 
         * */
        for (int i = 0; i < servicios.size(); i++) {
        	String aux=ips.get(i)+"/"+i;
        	WebTarget serverTarget = servicios.get(i);
        	serverTarget.path("configuracion").queryParam("ips", ipServers).queryParam("cadenaProcesos", cadenaProcesos).queryParam("totalProcesos", procesos).queryParam("ipServicio", aux).request().get();
        	
        }
    }
}

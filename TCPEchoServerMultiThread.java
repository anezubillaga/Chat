package networkpractice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPEchoServerMultiThread extends Thread{
    
    public static void main(String[] args) {
        TCPEchoServerMultiThread server = new TCPEchoServerMultiThread(12000);
        server.start();
    }
    
    final int port;
    final List<ClientThread> clients = new LinkedList<>();
    
    public TCPEchoServerMultiThread(int port) {
        this.port = port;
    }

    @Override
    public void run(){
        try( ServerSocket serverSocket = new ServerSocket(port); ){
        System.out.println("Started server on port " + port);
            // repeatedly wait for connections
            while(! interrupted() ){
                Socket clientSocket = serverSocket.accept();
                ClientThread clientThread = new ClientThread(clients, clientSocket);
                clientThread.start();
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public class ClientThread extends Thread{
        final List<ClientThread> clients;
        final Socket socket;
        DataOutputStream out;
        private String name;

        public ClientThread(List<ClientThread> clients, Socket socket) throws IOException {
            this.clients = clients;
            this.socket = socket;
            System.out.println("y socketInputstream: "+socket.getInputStream());
        }
        
        //only one thread at the time can send messages through the socket
        synchronized public void sendMsg(String msg){
            try {
                out.writeUTF(msg);
            } catch (IOException ex) {
                Logger.getLogger(TCPEchoServerMultiThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            try {
                int i = 0;
                System.out.println("Connection from " + 
                        socket.getInetAddress() + ":" + socket.getPort());
                
                DataInputStream in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                //now that we have managed to stablish proper connection, we add ourselve into the list
                synchronized (clients) { //we must sync because other clients may be iterating over it
                    clients.add(this);
                }
                //Encuentro el cliente que se ha conectado y le pongo ese nombre.
                clients.stream().filter((c) -> (c.socket.getPort() == socket.getPort())).forEachOrdered((c) -> {
                    c.name = ""+socket.getInetAddress()+": "+socket.getPort();
                });
                   try{
                       boolean imprimir;
                for (String line; (line = in.readUTF()) != null;) {
                    imprimir = true;
                    for (ClientThread c : clients)
                    {
                     if (c.socket.getPort() == socket.getPort())
                         i = clients.lastIndexOf(c);
                    }
                    String mayus = line.toUpperCase();
                    if (mayus.contains("/NAME "))
                    {
                        mayus = clients.get(i).name+" ha cambiado su nombre";
                        clients.get(i).name = line.toUpperCase().substring(6);
                    }
                    else if (mayus.equals("/EXIT"))
                    {
                        socket.close();
                        mayus = "El usuario "+clients.get(i).name+" se ha desconectado";
                        clients.remove(this);
                        //Tengo que imprimir algo diferente cuando le doy a exit, por ello lo pongo en false para poder imprimir otra cosa.
                        imprimir = false;
                    }
                    else if (mayus.equals("/LIST"))
                    {
                        String aux = "Lista de conectados: ";
                        aux = clients.stream().map((c) -> c.name + " ").reduce(aux, String::concat);
                        mayus = aux;
                    }
                    String aux="";
                    if (clients.size()>0 && imprimir)
                       aux = clients.get(i).name+"-> "+mayus;
                    else if (imprimir == false)
                        aux = mayus;
                    //when we read a line we send it to the rest of the clients in mayus
                    String mensaje = aux;
                    synchronized (clients) { //other clients may be trying to add to the list
                        clients.forEach(c -> c.sendMsg( mensaje ));
                    }
                }
                   }catch(EOFException ignore){}
            } catch (Exception ex) {
            } finally { //we have finished or failed so let's close the socket and remove ourselves from the list
                try{ socket.close();} catch(Exception ex){} //this will make sure that the socket closes
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }

        
    }
    
}

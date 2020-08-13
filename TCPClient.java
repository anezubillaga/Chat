package networkpractice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class TCPClient implements Runnable{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Menu interfaz;
    private boolean conectado;
    
    public TCPClient(String hostAddr,int port, Menu inter) throws IOException {
        this.interfaz = inter;
        conectado = true;
        socket = new Socket(hostAddr, port);
        System.out.println("Socket connected to " + socket.getInetAddress() + ":" + socket.getPort());
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        
    }
    
    public void sendMessage(String msg){
        try {
            if(! msg.isEmpty()){ 
                out.writeUTF(msg);
                out.flush(); //flush forces to send everything that may be in the buffer
            }
        }catch(IOException ex) {
        }
    }
    
    @Override
    public void run() {
        while(conectado){
            try { 
               String recv = in.readUTF();
               interfaz.setTextinArea(recv);
            } catch (IOException ex) {
           }
       }
    }
    
    public void interrumpir() throws IOException{
        in.close();
        out.close();
        socket.close();       
        Thread.currentThread().interrupt();
        conectado = false;
    }
    
    public boolean estaConectado(){
        return conectado;
    }
    
}

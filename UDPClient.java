import java.io.*;
import java.net.*;

class UDPClient {
   
   public static void main(String args[]) throws Exception {
        
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        
        DatagramSocket clientSocket = new DatagramSocket();
        
        InetAddress IPAddress = InetAddress.getByName("hostname");
     
        byte[] sendData = new Byte[1024];
        byte[] receiveData = new byte[1024];
        
        String sentence = inFromUser.readLine();
        
        sendData = sentence.getBytes();
        
        
        //Begin UDP Datagram
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, IPAddress, 9876); 
        
        clientSocket.send(sendPacket);
        
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        
        clientSocket.receive(receivePacket);
        
        String modifiedSentence = new String(receivePacket.getData());
        
        System.out.println("FROM SERVER:" + modifiedSentence);
        
        clientSocket.close();
         
   }
   
}
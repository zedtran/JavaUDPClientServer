import java.io.*;
import java.net.*;

class UDPServer {
   
   public static void main(String args[]) throws Exception {
                   
        // Create datagram socket at port: 10003
      DatagramSocket serverSocket = new DatagramSocket(10003);
                
      byte[] receiveData = new byte[256];
      byte[] sendData = new byte[256];
       
      while(true) {
         
         // Create space for received datagram
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         
         // Receive datagram
         serverSocket.receive(receivePacket);
         
         String sentence = new String(receivePacket.getData());
         
         // Get IP Address & Port # of sender
         InetAddress IPAddress = receivePacket.getAddress();
         
         int port = receivePacket.getPort();
         
         String capitalizedSentence = sentence.toUpperCase();
         
         sendData = capitalizedSentence.getBytes();
         
         // Create datagram to send to client
         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
         
         // Write out datagram to socket
         
         serverSocket.send(sendPacket);
        
      }   
   } 
}
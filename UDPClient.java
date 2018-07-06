import java.io.*;
import java.net.*;

class UDPClient {
  
   public static void main(String args[]) throws Exception {
      
      // Create Input Stream
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
      
      // Create Client Socket
      DatagramSocket clientSocket = new DatagramSocket();
      
      // Translate hostname to IP address using DNS
      InetAddress IPAddress = InetAddress.getByName("hostname");
   
      byte[] sendData = new byte[256];
      byte[] receiveData = new byte[256];
      
      String sentence = inFromUser.readLine();
      
      sendData = sentence.getBytes();
      
      
      // Begin UDP Datagram with data-to-send, length, IP Address, Port
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, IPAddress, 10003); 
      
      // Send datagram to server
      clientSocket.send(sendPacket);
      
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      
      // Read datagram from server
      clientSocket.receive(receivePacket);
      
      String modifiedSentence = new String(receivePacket.getData());
      
      System.out.println("FROM SERVER:" + modifiedSentence);
      
      clientSocket.close();
       
   }
}
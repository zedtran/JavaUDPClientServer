//////////////////////////////////////////////////////////////////////////////
// File:             UDPClient.java
// Semester:         COMP4320 - SUMMER 2018
//
// Authors:          DON TRAN, THADDEUS HATCHER, EVAN MCCARTHY
// Lecturer's Name:  Dr. ALVIN LIM
// Course Section:   001
//
//////////////////// CREDITS/SOURCES /////////////////////////////////////////
//
// Persons:          
//
// Online sources:   
//
//////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.util.*;
import java.nio.*;
import java.net.*;

/** 
*
* DESCRIPTION:  This program demonstrates how to implement a UDP Client Program
*
* ALLOCATED/ASSIGNED Port Numbers:     10000, 10001, 10002, 10003
* Selected Client/Server:              tux055 --> tux065
*
*/

public class UDPClient {
 
   public static void main(String[] args) throws Exception {
      if (args.length < 1) {
         System.out.println("Syntax: UDPClient <port>");
         return;
      }
   
      String hostname = InetAddress.getLocalHost().getHostName().trim();
      int port = Integer.parseInt(args[0]);
    
      try {
         
         // Create Client Socket
         DatagramSocket clientSocket = new DatagramSocket();
      
         // Translate hostname to IP address using DNS
         InetAddress IPAddress = InetAddress.getByName(hostname);
         
         // Begin UDP Datagram with data-to-request, length, IP Address, Port
         DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, IPAddress, port);
         clientSocket.send(requestPacket);
         System.out.println("\nRequest packet sent to server.\n");
            
         // Read datagram from server //
         // IF using Latin-Lipsum.txt: Byte Count = 29097
         // IF using Latin-Lipsum.html: Byte Count = 30738
      
         // numPackets contains the number of packets that are being sent by the Server
         // containing the file data
         byte[] numPacketsBeingSent = new byte[256];
         DatagramPacket firstResponse = new DatagramPacket(numPacketsBeingSent, numPacketsBeingSent.length);
         int packetCount = numPacketsBeingSent[0];
         System.out.println("\nFROM SERVER: ");
         while (packetCount < (int) numPacketsBeingSent[0]) {     
            byte[] buffer = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(responsePacket);
            if (detectErrors(buffer))
               System.out.println("This next packet has been corrupted.");
            System.out.print(new String(buffer));
            packetCount += 1;
         }
         clientSocket.close();   
             
      
      } 
      catch (SocketTimeoutException ex) {
         System.out.println("Timeout error: " + ex.getMessage());
         ex.printStackTrace();
      } 
      catch (IOException ex) {
         System.out.println("Client error: " + ex.getMessage());
         ex.printStackTrace();
      } 
     
   }

   private static boolean detectErrors(byte[] packet) {
      boolean errorsDetected = false;
      int sum = 0, checkSum = 0;
      byte[] chkSumBytes = new byte[4];
      for (int i = 4; i < packet.length; i++)
      {
         sum += packet[i];
      }
      chkSumBytes = Arrays.copyOfRange(packet, 0, 4);
      checkSum = ByteBuffer.wrap(chkSumBytes).order(ByteOrder.BIG_ENDIAN).getInt();
      if (sum != checkSum)
         errorsDetected = true;
      return errorsDetected;
   }
}
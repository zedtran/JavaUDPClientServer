///////////////////////////////////////////////////////////////////////////////
// File:             UDPServer.java
// Semester:         COMP4320 - SUMMER 2018
//
// Authors:          DON TRAN, THADDEUS HATCHER, EVAN MCCARTHY
// Lecturer's Name:  Dr. ALVIN LIM
// Course Section:   001
//
///////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/** 
*
* DESCRIPTION: This program demonstrates how to implement a UDP Server program 
*
* ALLOCATED/ASSIGNED Port Numbers:     10000, 10001, 10002, 10003
* Selected Client/Server:              tux055 --> tux065
*
*/
public class UDPServer {
   
   private DatagramSocket serverSocket;
   private List<String> loremList = new ArrayList<String>();
   private Random random;
   String requestHeader = "GET ipsumFile.html HTTP/1.0";
   String responseHeader = "HTTP/1.0 200 Document Follows\r\nContent-Type: " 
                            +  "text/plain\r\nContent-Length: xxx\r\n\r\n";
 
   public UDPServer(int port) throws SocketException {
      serverSocket = new DatagramSocket(port);
      random = new Random();
   }
   
   public static void main(String[] args) throws Exception {             
      if (args.length < 2) {
         System.out.println("Syntax: java UDPServer <file> <port>");
         return;
      }
   
      // IP Address of server for client to transmit
      String localHost = InetAddress.getLocalHost().getHostAddress().trim(); 
      System.out.println("\nConnected to " + localHost + "\n"); // Server IP      
   
      String ipsumFile = args[0];
      int port = Integer.parseInt(args[1]);
   
      try {
         UDPServer server = new UDPServer(port);
         server.loadIpsumFromFile(ipsumFile);
         server.service(args);
      } 
      catch (SocketException ex) {
         System.out.println("Socket error: " + ex.getMessage());
      } 
      catch (IOException ex) {
         System.out.println("I/O error: " + ex.getMessage());
      }
   }
                
   
   private void service(String[] args) throws IOException {
      boolean sentNumPackets = false;
      while (true) {
         DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, InetAddress.getByName("131.204.14.65"), 10003);
         serverSocket.receive(requestPacket);
         System.out.println("\nServer received inital request packet\n");
         String LoremIpsumString = loremList.toString();
         byte[] buffer = LoremIpsumString.getBytes();
         InetAddress clientAddress = requestPacket.getAddress(); // Return IPAddress of client
         int clientPort = requestPacket.getPort(); // Return port number of client
            
         // Send the number of packets in the file to the client so the receive loop exits
         // once all packets have been sent
         if (!sentNumPackets) {
            byte[] numPackets = packetsCountFile(new File(args[0]), 256);
            String numOfPacketsToSend = new String(numPackets);
            System.out.println("Server sending # of packets in requested file: " + numOfPacketsToSend + "\n");
            DatagramPacket numPacketsToBeSent = new DatagramPacket(numPackets, numPackets.length, clientAddress, clientPort);
            serverSocket.send(numPacketsToBeSent);
            sentNumPackets = true;
         }
      
         // end is the final index to be copied (exclusive), i is the initial index to be copied (inclusive)
         int end; 
         int i = 0; // The index for data copied from the original packet buffer
         int j; // The index for data copied to the outgoing packet buffer
         int packetNum = 1;
         int checksum;
         Byte tempByte; // Boxing conversion from "byte" to "Byte" for byte dereferencing 
         
         while (i < buffer.length) {
            // Creating header bytes for checksum value and copying them to the outgoing packet 
            byte[] packet_buffer = new byte[256];
            end = i + 252; 
            checksum = 0;
            j = 4; // Start index of data of each outgoing packet
            
            if (end >= buffer.length) {
               end = buffer.length;
            }
            while(i < end) {
               packet_buffer[j++] = buffer[i];
               tempByte = buffer[i++];
               checksum += tempByte.intValue(); 
            }
            System.out.println("\nCHECKSUM: " + checksum);
            byte[] chkSumBytes = intToBytes(checksum); // Creating checksum
            // Populating outgoing packet with checksum header
            for (int k = 0; k < 4; k++) {
               packet_buffer[k] = chkSumBytes[k];
            }
            String strToSend = new String(Arrays.copyOfRange(packet_buffer, 4, packet_buffer.length), StandardCharsets.UTF_8);
            System.out.println("Server sending: " + strToSend + " (From packet #: " + (packetNum++) + ")\n");
            DatagramPacket responsePacket = new DatagramPacket(packet_buffer, packet_buffer.length, clientAddress, clientPort);
            serverSocket.send(responsePacket);             
         }
      }
   }

   private static  byte[] intToBytes(int myInteger) {
      return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
   }
   
   private void loadIpsumFromFile(String ipsumFile) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(ipsumFile));
      String lorem;
      while ((lorem = reader.readLine()) != null) {
         loremList.add(lorem);
      }
      reader.close();
   }

   /** 
   * bytesCountFile calculates the number of packets that a file will be broken down in to
   * based on the number of bytes per packet.
   * 
   * @param file  file to be broken into packets
   * @param numBytesPerPacket number of bytes that will go into each packet 
   * @return  number of packets the file will be broken in to in a byte array 
   */
   private byte[] packetsCountFile(File file, int numBytesPerPacket) {
      int length = (int) file.length() / numBytesPerPacket;
      if (file.length() % numBytesPerPacket != 0) {
         length += 3;  
      }
      return intToBytes(length);
   } 
 
}

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
import java.lang.System.*;

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
      boolean notFinished = true;
      
      DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, InetAddress.getByName("131.204.14.65"), 10001);
      serverSocket.receive(requestPacket);
      System.out.println("\nServer received inital request packet\n");
      String LoremIpsumString = loremList.toString();
      byte[] buffer = LoremIpsumString.getBytes();
      InetAddress clientAddress = requestPacket.getAddress(); // Return IPAddress of client
      int clientPort = requestPacket.getPort(); // Return port number of client
      int pNum;
      // Send the number of packets in the file to the client so the receive loop exits
      // once all packets have been sent
      byte[] numPackets = packetsCountFile(new File(args[0]), 504);
      pNum = ByteBuffer.wrap(numPackets).order(ByteOrder.BIG_ENDIAN).getInt();
      pNum += 1;
	    //String numOfPacketsToSend = new String(numPackets);
      System.out.println("Server sending # of packets in requested file: " + pNum + "\n");
      DatagramPacket numPacketsToBeSent = new DatagramPacket(numPackets, numPackets.length, clientAddress, clientPort);
      serverSocket.send(numPacketsToBeSent);
      sentNumPackets = true;
      
      // end is the final index to be copied (exclusive), i is the initial index to be copied (inclusive)
      int end; 
      int i = 0; // The index for data copied from the original packet buffer
      int j; // The index for data copied to the outgoing packet buffer
      int packetNum = 0;
      int checksum;
      Byte tempByte; // Boxing conversion from "byte" to "Byte" for byte dereferencing 
      ArrayList data = new ArrayList();
      while (i < buffer.length) 
      {
        // Creating header bytes for checksum value and copying them to the outgoing packet 
        byte[] packet_buffer = new byte[512];
        
        end = i + 504; 
        checksum = 0;
        j = 8; // Start index of data of each outgoing packet
        
        if (end >= buffer.length) 
        {
          end = buffer.length;
        }
        
        while(i < end) 
        {
          packet_buffer[j++] = buffer[i];
          tempByte = buffer[i++];
          checksum += tempByte.intValue(); 
        }
        
        //System.out.println("\nCHECKSUM: " + checksum);
        byte[] chkSumBytes = intToBytes(checksum); // Creating checksum
        // Populating outgoing packet with checksum header
        for (int k = 0; k < 4; k++) 
        {
          packet_buffer[k] = chkSumBytes[k];
        }
        byte[] pacNumBytes = intToBytes(packetNum); // Creating checksum
        // Populating outgoing packet with checksum header
        for (int k = 4; k < 8; k++) {
          packet_buffer[k] = pacNumBytes[k-4];
        }
        data.add(packet_buffer);
        
        //String strToSend = new String(Arrays.copyOfRange(packet_buffer, 5, packet_buffer.length), StandardCharsets.UTF_8);
        //data[packetNumber] =
        System.out.println("Packet number " + packetNum + " (Sequence Number " + (packetNum % 24) + ") added to list.\n");
        packetNum++;
      }
      
      //System.out.println("Server sending: " + strToSend + " (From packet #: " + (packetNum++) + ")\n");
      int[] size = new int[pNum + 1];
      double pb = 0.3; 
      int first = 0;
      int last = 7;
      for (int z = 0; z < 8; z++) 
      {
        byte[] buf = (byte[])data.get(z);
        DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
        serverSocket.send(responsePacket);
        System.out.println("Server sent packet " + z + "\n");
      }
         
      int y = 8;
      int f = 0;
      long timeOut = 40;
      long check = System.currentTimeMillis();
      while (first < pNum) 
      {
        double timeout = Math.random();
         //try to recieve
         if(first == (pNum)) 
         {
           for (int d = first; d < (last + 1); d++) 
           {
             if (size[d] == 1) 
               f++;
           }
           
           if(f == 8) 
           {
             first = pNum + 5;
             System.out.print("Received all ACKs, server should shut down.");
             continue;
           }
         } 
         while ((y < pNum) && (y < last + 1)) 
         {
           byte[] buf = (byte[])data.get(y);
           DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
           serverSocket.send(responsePacket);
           
           System.out.println("Server sent packet " + y + " with sequence number " + (y % 24) + "\n");
           y++;
         }
         
         try {
            byte[] nack = new byte[5];
            DatagramPacket responsePacket = new DatagramPacket(nack, nack.length, InetAddress.getByName("131.204.14.55"), 10001);
            serverSocket.receive(responsePacket);
            int ackNum;
            ackNum = getAckNum(nack);

            if (nack[4] == 1) 
            {
              size[ackNum] = 1;
              System.out.println("Server received ACK for packet " + ackNum + " with sequence number " + (ackNum % 24) + "\n");
              //for (int a = first; a < last; a++) 
              while (ackNum == first) 
              {
                if (size[ackNum] == 1) 
                {
                  first++;
                  last++;
                  ackNum++;
                  System.out.println("Window moved " + first + " to " + last + "\n");
                }
                  
                else {break;}
              }
            }
            
            else 
            {
              if (timeout > pb)
                System.out.println("Server received NAK--CORRUPTION for packet " + ackNum + " Sequence# " + (ackNum % 24) + "\nResending packet.\n");
              else
                System.out.println("Timeout " + ackNum + " Sequence #: " + (ackNum % 24) + "\nResending packet.\n");
              byte[] resend = (byte[])data.get(ackNum);
              DatagramPacket resendPacket = new DatagramPacket(resend, resend.length, clientAddress, clientPort);
              serverSocket.send(resendPacket);
            }
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
      
      System.out.println("\n\n***SERVER FILE TRANSMISSION COMPLETE***\n\n");
      //notFinished = false;
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
         length += 1;  
      }
      return intToBytes(length);
   } 
   
   public static int getAckNum(byte[] packet){
      byte[] temp = new byte[5];
      temp = Arrays.copyOfRange(packet, 0, 4);
      int num = ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getInt();
      return num;
   }

   private static int extractSeqNum(byte[] packetData) {
    byte[] temp = Arrays.copyOfRange(packetData, 4, 8);
    return ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getInt();
   }
 
}

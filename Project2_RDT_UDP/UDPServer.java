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
   final int WINDOW_SIZE = 8;
 
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

   private void service(String[] args) throws IOException 
   {
       String LoremIpsumString = loremList.toString();
       byte[] buffer = LoremIpsumString.getBytes();
       InetAddress clientAddress = requestPacket.getAddress(); // Return IPAddress of client
       int clientPort = requestPacket.getPort(); // Return port number of client
       // create array with size equal to number of packets being sent

       int numPkts = packetsCountFile(new File(args[0]), 512);  // change to 512 bytes
       int[] window = {0, 1, 2, 3, 4, 5, 6, 7};
       // segment file into 512 byte packets
       DatagramPacket[] packets = segmentFile(buffer, numPkts, 512);
       // this Array List will hold the packets that fit within the window and are to be sent or are awaiting ACKs
       ArrayList<DatagramPacket> pktsInWindow = new ArrayList<DatagramPacket>(WINDOW_SIZE);
       for (int i = 0; i < WINDOW_SIZE; i++)        // put first 8 elements into window
       {
           pktsInWindow.add(packets[i]);
       }

       boolean sentNumPackets = false;
       boolean notFinished = true;
       while (notFinished)
       {
           // receive request
           DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, InetAddress.getByName("131.204.14.65"), 10003);
           serverSocket.receive(requestPacket);
           System.out.println("\nServer received inital request packet\n");

           // send header packet containing number of packets to be sent
           // Client will use this info to make buffer or appropriate size
           if (!sentNumPackets) 
           {
               byte[] numPackets = intToBytes(numPkts);
               int pNum = ByteBuffer.wrap(numPackets).order(ByteOrder.BIG_ENDIAN).getInt();
               pNum += 1;
               //String numOfPacketsToSend = new String(numPackets);
               System.out.println("Server sending # of packets in requested file: " + pNum + "\n");
               DatagramPacket numPacketsToBeSent = new DatagramPacket(numPackets, numPackets.length, clientAddress, clientPort);
               serverSocket.send(numPacketsToBeSent);
               sentNumPackets = true;
           }

           ArrayList<boolean> ackBuffer = new ArrayList<boolean>(WINDOW_SIZE);
           boolean sentNextSeven = false;
           // send first 8 packets (window size is 8)
           serverSocket.send(pktsInWindow.get(0));
           socket.setSoTimeout(40);
            do {
                try {
                   if (!sentNextSeven) 
                   {
                       for (int i = 1; i < WINDOW_SIZE; i++)
                       {
                           //String strToSend = new String(Arrays.copyOfRange(packet_buffer, 4, packet_buffer.length), StandardCharsets.UTF_8);
                           //System.out.println("Server sending: " + strToSend + " (From packet #: " + (packetNum++) + ")\n");
                           serverSocket.send(pktsInWindow.get(i));
                       }
                       sentNextSeven = true;
                   }
                   receiveACK(ackBuffer);
                   if (ackBuffer.get(0) == extractSeqNum(pktsInWindow.get(0).getData()))
                   {
                       // shift window
                       pktsInWindow.remove(0);
                       pktsInWindow.add(packets.get(0));
                       packets.remove(0);
                       ackBuffer.remove(0);
                       ackBuffer.add(0);

                       // send newly added packet
                       serverSocket.send(pktsInWindow.get(WINDOW_SIZE - 1));
                   }
               } 
               catch (SocketTimeoutException e)
               {
                   socket.send(pktsInWindow.get(0));
                   socket.setSoTimeout(40);
               }
           } while (packets.size() > 0);
           
           // wait for ACK
           // if ACK received for the first of the 8 packets, shift window by 1
           // send new packet that was added to the window
       }
   }

   private static void receiveACK(ArrayList<int> ackBuffer) {
       byte[] seqNumArray = new byte[5];
       DatagramPacket ackPacket = new DatagramPacket(seqNum, seqNum.length, InetAddress.getByName("131.204.14.65"), 10003);
       serverSocket.receive(ackPacket);
       int seqNum = ByteBuffer.wrap(seqNumArray).order(ByteOrder.BIG_ENDIAN).getInt();
       if (seqNumArray[4] == 1)
       {
           ackBuffer.set(seqNum, true);
       }
       System.out.println("\nServer received ACK for packet with Sequence Number " + seqNum + "\n");
   } 

   private static int extractSeqNum(byte[] packetData) {
       byte[] temp = Arrays.copyOfRange(packetData, 4, 8);
       return ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getInt();
   }
   
   private static ArrayList<DatagramPacket> segmentFile(byte[] fileBuffer, int numPkts, int size) {
       ArrayList<DatagramPacket> packets = new ArrayList<DatagramPacket>(numPkts);
       
       // end is the final index to be copied (exclusive), i is the initial index to be copied (inclusive)
       int end; 
       int i = 0; // The index for data copied from the original packet buffer
       int j; // The index for data copied to the outgoing packet buffer
       int packetNum = 0;
       int checksum;
       Byte tempByte; // Boxing conversion from "byte" to "Byte" for byte dereferencing 

       while (i < buffer.length)
       {
           // Creating header bytes for checksum value and copying them to the outgoing packet 
           byte[] packet_buffer = new byte[512];
           end = i + 504;  // 512 - 8 = 504
           checksum = 0;
           j = 8; // Start index of data of each outgoing packet // change to 8 to fit 2 ints
           
           if (end >= buffer.length) 
           end = buffer.length;
           
           while (i < end) 
           {
               packet_buffer[j++] = buffer[i];
               tempByte = buffer[i++];
               checksum += tempByte.intValue(); 
           }
           
           byte[] chkSumBytes = intToBytes(checksum); // Creating checksum
           //byte[] sequenceNumber = getSequenceNumber(packetNum);
           byte[] packetNumber = intToBytes(packetNum);
           // Populating outgoing packet with checksum header and sequence number
           for (int k = 0; k < 8; k++) 
           {
               packet_buffer[k] = (k < 4) ? chkSumBytes[k] : packetNumber[k];
           }
           packets.add(new DatagramPacket(packet_buffer, packet_buffer.length, clientAddress, clientPort), packetNum++);
       }
   }

   private static byte[] intToBytes(int myInteger) {
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
   private int packetsCountFile(File file, int numBytesPerPacket) {
      int length = (int) file.length() / numBytesPerPacket;
      if (file.length() % numBytesPerPacket != 0) {
         length += 3;  
      }
      return length;
   }

   private byte[] getSequenceNumber(int packetNum) {
       int sequenceNumber = packetNum % 24;
       return intToBytes(sequenceNumber);
   }
   
}
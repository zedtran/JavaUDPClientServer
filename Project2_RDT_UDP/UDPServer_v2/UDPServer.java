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
import java.util.concurrent.ArrayBlockingQueue;

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
   final int WINDOW_SIZE = 8;

   // BUFFER FOR MANAGING RESPONSE WINDOW
   private PacketNode[] rwnd; // This is the buffer that will contain packet status

   // GLOBAL Helper Vars
   private LinkedList<DatagramPacket> pktWindowList;
   private ArrayBlockingQueue<DatagramPacket> pktsQueue; // The queue containing all packets
   public boolean isQueueBlocked; //
   private Timer packetTimer; // timeout timer
   private int rwndSize; // response window size
   private int timeOut; // The timeOut time
   private final int PACKET_SIZE = 512; //the size of each packets


   /////////////////////// END GLOBAL VARIABLES /////////////////////////

   /** Class Constructor **/
   public UDPServer(int port) throws SocketException {
      serverSocket = new DatagramSocket(port);
      random = new Random();
   }

    /** BEGINNING OF MAIN DRIVER **/
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
   //// END MAIN DRIVER ////

     /**
     * A separate runnable Thread for calling sendData() for each packet
     */
   private class Server extends Thread {
      UDPServer server;
   
      public Server(UDPServer server) {
         this.server = server;
      }
   
      public void run() {
         while (true) {
            try {
               server.SendData();
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }


   private void service(String[] args) throws IOException
   {
      String LoremIpsumString = loremList.toString();
      byte[] buffer = LoremIpsumString.getBytes();
      InetAddress clientAddress; // Return IPAddress of client
      int clientPort; // Return port number of client
   
       // create array with size equal to number of packets being sent
      int numPkts = packetsCountFile(new File(args[0]), 512);  // change to 512 bytes
   
       /**
        This is the buffer which will be used to monitor individual packet statuses.
        The statuses are enumerated as follows:
            READY(1), SENT(2), ACKED(3), RECEIVED(4), LOST(5), CORRUPTED(6), RESENT(7);
       */
      rwnd = new PacketNode[numPkts];
   
       // segment file into 512 byte packets
      ArrayBlockingQueue<DatagramPacket> pktsQueue = segmentFile(buffer, numPkts, 512, InetAddress.getByName("131.204.14.55"), 10003);
   
     //   // this Array List will hold the packets that fit within the window and are to be sent or are awaiting ACKs
      // ArrayList<DatagramPacket> pktsInWindow = new ArrayList<DatagramPacket>(WINDOW_SIZE);
      // for (int i = 0; i < WINDOW_SIZE; i++)        // put first 8 elements into window
      // {
         // pktsInWindow.add(packets[i]);
      // }
   
      boolean sentNumPackets = false;
      boolean notFinished = true;
   
      while (notFinished)
      {
           // receive request
         DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, InetAddress.getByName("131.204.14.65"), 10003);
         serverSocket.receive(requestPacket);
         System.out.println("\nServer received inital request packet\n");
      
         clientAddress = requestPacket.getAddress();
         clientPort =  requestPacket.getPort();
      
      
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
      
           // //ArrayList<boolean> ackBuffer = new ArrayList<boolean>(WINDOW_SIZE);
           // //ArrayList<int> seqBuffer = new ArrayList<boolean>(WINDOW_SIZE);
           // boolean sentNextSeven = false;
           // // send first 8 packets (window size is 8)
           // serverSocket.send(pktsInWindow.get(0));
           // socket.setSoTimeout(40);
           //  do {
           //      try {
           //         if (!sentNextSeven)
           //         {
           //             for (int i = 1; i < WINDOW_SIZE; i++)
           //             {
           //                 //String strToSend = new String(Arrays.copyOfRange(packet_buffer, 4, packet_buffer.length), StandardCharsets.UTF_8);
           //                 //System.out.println("Server sending: " + strToSend + " (From packet #: " + (packetNum++) + ")\n");
           //                 serverSocket.send(pktsInWindow.get(i));
           //             }
           //             sentNextSeven = true;
           //         }
           //         receiveACK(ackBuffer);
           //         if (ackBuffer.get(0) == extractSeqNum(pktsInWindow.get(0).getData()))
           //         {
           //             // shift window
           //             pktsInWindow.remove(0);
           //             pktsInWindow.add(packets.get(0));
           //             packets.remove(0);
           //             ackBuffer.remove(0);
           //             ackBuffer.add(0);
           //
           //             // send newly added packet
           //             serverSocket.send(pktsInWindow.get(WINDOW_SIZE - 1));
           //         }
           //     }
           //     catch (SocketTimeoutException e)
           //     {
           //         socket.send(pktsInWindow.get(0));
           //         socket.setSoTimeout(40);
           //     }
           // } while (packets.size() > 0);
      
           // wait for ACK
           // if ACK received for the first of the 8 packets, shift window by 1
           // send new packet that was added to the window
      }
   }


   /**
    * Send method use selective repeat
    */
   public void SendData() throws Exception {
      isQueueBlocked = true; // in transmission, block all traffic
       //numberOfTimeouts = 0; // times of timeouts
       //byte[] packetData = new byte[PACKET_SIZE]; // the packet data
      packetTimer = new Timer(true); // sent timer
      rwndSize = 0; //size of responseWindow
   
      while (true) {
         while (pktsQueue.isEmpty() && rwndSize == 0) {
            isQueueBlocked = false;
         }
         if (rwndSize == 0) { // if it is the first time to send
            isQueueBlocked = true;
            rwndSize = Math.min(pktsQueue.size(), WINDOW_SIZE);
            rwnd = new PacketNode[rwndSize];
         
            for (int i = 0; i < rwndSize; i++) {
               DatagramPacket p = pktsQueue.take();
               pktWindowList.addLast(p);
               SendPacket(p);
            }
         } else {
            isQueueBlocked = true;
            int emptySpace = getWindowShift();
            PacketNode[] newRwnd = new PacketNode[rwndSize];
            int ping = 0; // the variable to set windows
               //adjust list of sending windows
            for (int i = 0; i < emptySpace; i++) {
               pktWindowList.removeFirst();
            }
               // merge to new windows
            for (int i = emptySpace; i < rwndSize; i++) {
               newRwnd[ping] = rwnd[i];
               ping++;
            }
               // send new packet
            while (emptySpace-- != 0 && !pktsQueue.isEmpty()) {
               DatagramPacket p = pktsQueue.poll();
                   // Update packet status
               int seqNum = extractSeqNum(p.getData());
               rwnd[packetIndexOf(seqNum)].setState(2);
               pktWindowList.addLast(p);
               SendPacket(p);
            }
         
               // merge windows
            rwnd = newRwnd;
            rwndSize = pktWindowList.size();
         }
         if (rwndSize != 0) {
            isQueueBlocked = true;
         
            receiveACK();
         
         } else {
            isQueueBlocked = false;
            rwndSize = Math.min(pktsQueue.size(), WINDOW_SIZE);
         }
      }
   }

     



   private void receiveACK() throws UnknownHostException {
      
      try {
         
         byte[] seqNumArray = new byte[5];
         DatagramPacket ackPacket = new DatagramPacket(seqNumArray, seqNumArray.length, InetAddress.getByName("131.204.14.55"), 10003);
         serverSocket.receive(ackPacket);
         int seqNum = ByteBuffer.wrap(seqNumArray).order(ByteOrder.BIG_ENDIAN).getInt();
      
         if (seqNumArray[4] == 1)
         {
            rwnd[packetIndexOf(seqNum)].setState(3); // Set Status ACKd
         
           // pktWindowList   // ACK
           // rwnd[]  // ACK
         }
         System.out.println("\nServer received ACK for packet with Sequence Number " + seqNum + "\n");
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }



   private int extractSeqNum(byte[] packetData) {
      byte[] temp = Arrays.copyOfRange(packetData, 4, 8);
      return ByteBuffer.wrap(temp).order(ByteOrder.BIG_ENDIAN).getInt();
   }


   private ArrayBlockingQueue<DatagramPacket> segmentFile(byte[] fileBuffer, int numPkts, int size, InetAddress clientAddr, int clientPort) throws UnknownHostException {
      ArrayBlockingQueue<DatagramPacket> packets = new ArrayBlockingQueue(numPkts);
       // end is the final index to be copied (exclusive), i is the initial index to be copied (inclusive)
      int end;
      int i = 0; // The index for data copied from the original packet buffer
      int j; // The index for data copied to the outgoing packet buffer
      int packetNum = 0;
      int checksum;
      Byte tempByte; // Boxing conversion from "byte" to "Byte" for byte dereferencing
      int rwndIndex = 0; // The index of the status buffer for filling in default state values
   
   
      try {
         
         while (i < fileBuffer.length)
         {
           // Creating header bytes for checksum value and copying them to the outgoing packet
            byte[] packet_buffer = new byte[512];
            end = i + 504;  // 512 - 8 = 504
            checksum = 0;
            j = 8; // Start index of data of each outgoing packet // change to 8 to fit 2 ints
         
            if (end >= fileBuffer.length)
               end = fileBuffer.length;
         
            while (i < end)
            {
               packet_buffer[j++] = fileBuffer[i];
               tempByte = fileBuffer[i++];
               checksum += tempByte.intValue();
            }
         
            byte[] chkSumBytes = intToBytes(checksum); // Creating checksum
            byte[] sequenceNumber = getSequenceNumber(packetNum);
         
           // For each newly created packet, maintain state information (Seq# & State)
            PacketNode pNode = new PacketNode(packetNum % 24, 1);
            rwnd[rwndIndex++] = pNode; // Add to status buffer
         
         
           // Populating outgoing packet with checksum header and sequence number
            for (int k = 0; k < 8; k++)
            {
               packet_buffer[k] = (k < 4) ? chkSumBytes[k] : sequenceNumber[k];
            }
            packets.put(new DatagramPacket(packet_buffer, packet_buffer.length, InetAddress.getByName("131.204.14.55"), 10003));
            packetNum++;
         }
      
         
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      return packets;
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
   private static int packetsCountFile(File file, int numBytesPerPacket) {
      int length = (int) file.length() / numBytesPerPacket;
      if (file.length() % numBytesPerPacket != 0) {
         length += 3;
      }
      return length;
   }




   private static byte[] getSequenceNumber(int packetNum) {
      int sequenceNumber = packetNum % 24;
      return intToBytes(sequenceNumber);
   }



   /**
    * the timer for packet use to set up the timeout.
    */
   private class PacketTimeout extends TimerTask {
      private DatagramPacket p;
   
      public PacketTimeout(DatagramPacket p) {
         this.p = p;
      }
   
      public void run() {
           //if packet has not been ACKed
                       // numberOfTimeouts++;
         try {
            int seqNumber = extractSeqNum(p.getData());
            if (!(isACK(seqNumber))) {
               SendPacket(p);
            
                   // Retrieve index of status buffer packet to modify
               int index = packetIndexOf(seqNumber);
               if (index != -1) {
                  rwnd[index].setState(7);
               }
               else {
                  System.out.println("ERROR: Unexpected PacketNode[] index (-1). Refer to Sequence Number: " + seqNumber);
               }
            
            }
         }
         catch (Exception e) { }
      }
   }

       /**
    * send a packet to client
    * @param packet the packet
    * @throws Exception the socket exception
    */
   private void SendPacket(DatagramPacket packetToSend) throws Exception {
      serverSocket.send(packetToSend);
       // when sending a packet, set a timer
      packetTimer.schedule(new PacketTimeout(packetToSend), timeOut);
   }



   /**
       Returns true if the DatagramPacket sequence number is in a ready state
       and has been ACKd
   */
   private boolean isACK(int seqNum) {
      for (int i = 0; i < rwnd.length; i++) {
         if (rwnd[i].getSeqNum() == seqNum
               && rwnd[i].getStateValue() == 3) {
            return true;
         }
      }
      return false;
   }

   /**
       Returns index of PacketNode/DatagramPacket given by the specified sequence number
       whose PacketNode is in an unAcked state.
   */
   private int packetIndexOf(int seqNum) {
      for (int i = 0; i < rwnd.length; i++) {
         if (rwnd[i].getSeqNum() == seqNum
               && rwnd[i].getStateValue() != 3) {
            return i;
         }
      }
      return -1;
   }

   /**
    * the method will move the first nak in windows to the first position.
    * @return the number of shifts
    * @throws Exception the exception
    */
   private int getWindowShift() throws Exception {
      int windowShift = 0;
      for (int i = 0; i < rwndSize; i++) {
         if (rwnd[i].getStateString().equals("ACKED")) {
            windowShift++;
         }
         else {
            break;
         }
      }
      return windowShift;
   }




}

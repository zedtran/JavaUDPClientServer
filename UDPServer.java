///////////////////////////////////////////////////////////////////////////////
// File:             UDPServer.java
// Semester:         COMP4320 - SUMMER 2018
//
// Authors:          DON TRAN, THADDEUS HATCHER, EVAN MCCARTHY
// Lecturer's Name:  Dr. ALVIN LIM
// Course Section:   001
//
//////////////////// CREDITS/SOURCES //////////////////////////////////////////
//
// Persons:          
//
// Online sources:   
//
//////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.net.*;
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
 
   public UDPServer(int port) throws SocketException {
        serverSocket = new DatagramSocket(port);
        random = new Random();
   }
   
   public static void main(String[] args) throws Exception {             
      if (args.length < 2) {
            System.out.println("Syntax: UDPServer <file> <port>");
            return;
      }
 
      String ipsumFile = args[0];
      int port = Integer.parseInt(args[1]);
 
      try {
            UDPServer server = new UDPServer(port);
            server.loadIpsumFromFile(ipsumFile);
            server.service();
      } catch (SocketException ex) {
            System.out.println("Socket error: " + ex.getMessage());
      } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
      }
   }
                
   
   private void service() throws IOException {
        while (true) {
            DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1);
            serverSocket.receive(requestPacket);
 
            String LoremIpsumString = loremList.toString();
            byte[] buffer = LoremIpsumString.getBytes();
 
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();
 
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
            serverSocket.send(responsePacket);
        }
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
    private String getRandomLoremListString() {
        int randomIndex = random.nextInt(loremList.size());
        String randomLoremString = loremList.get(randomIndex);
        return randomLoremString;
    }
    */ 
}
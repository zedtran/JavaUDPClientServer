//////////////////////////////////////////////////////////////////////////////
// File:             UDPClient.java
// Semester:         COMP4320 - SUMMER 2018
//
// Authors:          DON TRAN, THADDEUS HATCHER, EVAN MCCARTHY
// Lecturer's Name:  Dr. ALVIN LIM
// Course Section:   001
//
//////////////////////////////////////////////////////////////////////////////

import java.io.*;
import java.util.*;
import java.nio.*;
import java.net.*;
import java.lang.Math;
import java.text.DecimalFormat;
import java.util.Random;
import java.math.BigDecimal;

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
      if (args.length < 2) {
         System.out.println("Syntax: java UDPClient <port> <Probability of Corruption>");
         return;
      }
      // The probability of packet corruption
      final double CORRUPT_PROBABILITY = Double.parseDouble(args[1]);  // Ensure: 0 <= CORRUPT_PROBABILITY < 1
   
      // Ensuring command line arg probability is between 0 and 1
      if (CORRUPT_PROBABILITY < 0 || CORRUPT_PROBABILITY >= 1) {
         System.out.println("\nThe argument for runtime probability of packet corruption must be between 0 (inclusive) and 1 (exclusive).\n");
         System.exit(0);
      }
   
      //String hostname = InetAddress.getLocalHost().getHostName().trim();
      int port = Integer.parseInt(args[0]);
    
      try {
         
         // Create Client Socket
         DatagramSocket clientSocket = new DatagramSocket();
      
         // Translate hostname to IP address using DNS (Server IP)
         InetAddress IPAddress = InetAddress.getByName("131.204.14.65");
         
         // Begin UDP Datagram with data-to-request, length, IP Address, Port
         DatagramPacket requestPacket = new DatagramPacket(new byte[1], 1, IPAddress, port);
         clientSocket.send(requestPacket);
         System.out.println("\nInitial request packet sent to server.\n");
            
         // Read datagram from server //
         // IF using Latin-Lipsum.txt: Byte Count = 29097
         // IF using Latin-Lipsum.html: Byte Count = 30738
      
         // numPackets contains the number of packets that are being sent by the Server
         // containing the file data
         byte[] numPacketsBeingSent = new byte[4];
         int j = 0;
         DatagramPacket firstResponse = new DatagramPacket(numPacketsBeingSent, numPacketsBeingSent.length, InetAddress.getByName("131.204.14.55"), 10003);
         clientSocket.receive(firstResponse);
         int packetCount = ByteBuffer.wrap(numPacketsBeingSent).order(ByteOrder.BIG_ENDIAN).getInt();
         System.out.println("\nClient received number of packets for requested file: " + (packetCount++));
         
         
         while (j <= packetCount) {     
            byte[] buffer = new byte[256];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("131.204.14.55"), 10003);
            clientSocket.receive(responsePacket);
            System.out.println("\nClient received packet # " + (j + 1) + "  of " + packetCount + "\n");  
            clientGremlin(responsePacket, CORRUPT_PROBABILITY);
            System.out.println("Client sent packet # " + (j + 1) + "  to client gremlin.\n");  
            
            // Performs error detection following client gremlin function (true if packet was corrupted, false otherwise)
            if (detectErrors(buffer)) {
               System.out.println("Packet # " + (j + 1) + " of " + packetCount + ": ***CORRUPTED***\n");
            }
            else {
               System.out.println("Packet # " + (j + 1) + " of " + packetCount + ": NOT CORRUPTED\n");
            }
            
            byte[] bufNoChkSum = Arrays.copyOfRange(buffer, 4, buffer.length);   
            System.out.print("MESSAGE(Packet# " + (j + 1) + "): " + new String(bufNoChkSum) + "\n");
            j++; // Increment packet index
         }
      
         clientSocket.close();   
         System.out.println("\n\n***SERVER FINISHED***\n\n");    
      
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
      int sum = 0, checkSum = 0, temp = 0;
      Byte tempByte;
      for (int i = 4; i < packet.length; i++) {
         tempByte = packet[i];
         sum += tempByte.intValue();
      }
      byte[] chkSumBytes = new byte[4];
      chkSumBytes = Arrays.copyOfRange(packet, 0, 4);
      checkSum = ByteBuffer.wrap(chkSumBytes).order(ByteOrder.BIG_ENDIAN).getInt();
      if (sum != checkSum)
      {
         System.out.println("***CHECKSUM ERROR***: ACTUAL checksum: " + sum + ", EXPECTED checksum: " + checkSum + "\n");
         errorsDetected = true;
      }
      else {
         System.out.println("--Checksum values OK--\n");
      }
      return errorsDetected;
   }

   /**
 *  clientGremlin() decides when to damage packets and how many bytes of the
 *  receiving packet to corrupt. If the packet is to be damaged:
 *    >> The probability of changing one byte is 0.5 (i.e. P(X=1) = 0.5),
 *    >> The probability of changing two bytes is 0.3 (i.e. P(X=2) = 0.3),
 *    >> The probability of changing 3 bytes is 0.2 (i.e. P(X=3) = 0.2).
 *  Every byte in the packet is equally likely to be damaged.
 *
 *  Any packet corruption implemented will be controlled such
 *  that we know what bit(s) is/are being modified. Particularly, we will reverse the bits
 *  of a buffer byte if we need to corrupt it. This is so we know how to fix the corrupted packets.
 */
   private static void clientGremlin(DatagramPacket responsePacket, double CORRUPT_PROBABILITY) {
      int numBytesToCorrupt; // We can define a global variable to cross-reference this number and the indeces we modify on the next line so we know how many and which bytes we corrupt/damage
      int corruptIndexOne, corruptIndexTwo, corruptIndexThree;
      //DecimalFormat df = new DecimalFormat("%.1f");
      Double randomProbability = new BigDecimal(Math.random()).setScale(1, BigDecimal.ROUND_HALF_DOWN).doubleValue(); // THIS will always return random num betweeen 0 and 1
      double corruptOneUpperBound = 0.5, corruptTwoUpperBound = 0.8;
      byte[] responseData = responsePacket.getData();
   
    // This conditional check determines if clientGremlin will choose to damage packets.
    // (If the runtime argument for the given probability of damaged packets falls within the appropriate range)
    // P(X=x) if 0 <= CORRUPT_PROBABILITY < X
      if (randomProbability <= CORRUPT_PROBABILITY) {
        /**
        *  Compute new randomProbability for determining # of bytes to corrupt in the responsPacket.
        *      >> If it falls between 0.0 and 0.5 --> Corrupt one bytes
        *      >> If it falls between 0.5 and 0.8 --> Corrupt two bytes
        *      >> If it falls between 0.8 and 1.0 --> Corrupt 3 bytes
        */
         randomProbability = new BigDecimal(Math.random()).setScale(1, BigDecimal.ROUND_HALF_DOWN).doubleValue();
      
        /** This condition will scale to handle cases of variable packet length
         *  s.t. the packet length is between 4 and 256
         */
         if (responseData.length <= 256 && responseData.length > 3) {
            // Conditional check for 50% chance of damaging 1 byte (0.0 <= P(X) < 0.5)
            if (0 <= randomProbability && randomProbability < corruptOneUpperBound) {
               numBytesToCorrupt = 1;
               corruptIndexOne = getRandomIntBetween(4, responseData.length - 1);
               responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
            }
            // Conditional check for 30% chance of damaging 2 bytes (0.5 <= P(X) < 0.8)
            else if (corruptOneUpperBound <= randomProbability && randomProbability < corruptTwoUpperBound) {
               numBytesToCorrupt = 2;
               corruptIndexOne = getRandomIntBetween(4, (int)((responseData.length + 3)/2));
               corruptIndexTwo = getRandomIntBetween((int)((responseData.length + 3)/2), responseData.length - 1);
               responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
               responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
            }
            // Conditional check for 20% chance of damaging 3 bytes (0.8 <= P(X) < 1)
            else if (corruptTwoUpperBound <= randomProbability && randomProbability < 1) {
               numBytesToCorrupt = 3;
               corruptIndexOne = getRandomIntBetween(4, (int)((responseData.length + 3)/3));
               corruptIndexTwo = getRandomIntBetween((int)((responseData.length + 3)/3), (int)(2*(responseData.length + 3)/3));
               corruptIndexThree = getRandomIntBetween((int)(2*(responseData.length + 3)/3), responseData.length - 1);
               responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
               responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
               responseData[corruptIndexThree] = reverseBitsByte(responseData[corruptIndexThree]);
            }
         }
         // Edge cases where packet length data <= 3
         else {
            // If there is only one byte in the packet to consider
            // 4 bytes for checksum + 1 byte data
            if (responseData.length == 5) {
                /**
                 *  Here the probabilistic determination for which bytes get affected doesn't matter, because
                 *  there is only one byte to consider in this packet.
                 */
               numBytesToCorrupt = 1;
               corruptIndexOne = 4;
               responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
            }
            // If there are only two bytes in the packet to consider
            // 4 bytes for checksum + 2 bytes for data
            else if (responseData.length == 6) {
               // 50% chance of damaging 1 byte
               if (0 <= randomProbability && randomProbability < corruptOneUpperBound) {
                  numBytesToCorrupt = 1;
                  corruptIndexOne = getRandomIntBetween(4, responseData.length - 1);
                  responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
               }
               // 20% chance of damaging 3 bytes or 30% chance of damaging 2 bytes
               // Since we only have 2 bytes to consider in this packet, we will damage both bytes. We don't have a
               // third byte to consider.
               else if (randomProbability != 1.0) {
                  numBytesToCorrupt = 2;
                  corruptIndexOne = 4;
                  corruptIndexTwo = 5;
                  responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                  responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
               }
            }
            // If there are only 3 bytes in the packet to consider
            // 4 bytes for checksum + 3 bytes for data
            else if (responseData.length == 7) {
                // 50% chance of damaging 1 byte
               if (0 <= randomProbability && randomProbability < corruptOneUpperBound) {
                  numBytesToCorrupt = 1;
                  corruptIndexOne = getRandomIntBetween(4, responseData.length - 1);
                  responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
               }
                // 30% chance of damaging 2 bytes
               else if (corruptOneUpperBound <= randomProbability && randomProbability < corruptTwoUpperBound) {
                  numBytesToCorrupt = 2;
                  corruptIndexOne = getRandomIntBetween(4, responseData.length - 1);
               
                    // Randomly choose a byte index != to an index already selected
                  do {
                     corruptIndexTwo = getRandomIntBetween(4, responseData.length - 1);
                  } while (corruptIndexTwo == corruptIndexOne);
               
                  responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                  responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
               }
                // 20% chance of damaging 3 bytes
               else if (randomProbability != 1.0) {
                  numBytesToCorrupt = 3;
                  corruptIndexOne = 4;
                  corruptIndexTwo = 5;
                  corruptIndexThree = 6;
                  responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                  responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
                  responseData[corruptIndexThree] = reverseBitsByte(responseData[corruptIndexThree]);
               }
            }
         }
      }
    // Packet is either altered or unaltered based on specified probability -- return
   }

// Simply gets a random int value between two numbers
   private static int getRandomIntBetween(int low, int high) {
      Random rand = new Random();
      return rand.nextInt((high - low) + 1) + low;
   }

// Simply reverses the bits of a byte
   public static byte reverseBitsByte(byte x) {
      int intSize = 8;
      byte y = 0;
      for(int position = intSize - 1; position > 0; position-- ) {
         y += ((x&1) << position);
         x >>= 1;
      }
      return y;
   }
}

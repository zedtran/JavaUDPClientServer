
import java.lang.Math;
import java.text.DecimalFormat;
import java.util.Random;

public final double CORRUPT_PROBABILITY = args[2];  // Ensure: 0 <= CORRUPT_PROBABILITY < 1

if (args[2] >= 1 || args[2] < 0) {
    System.out.println("The argument for runtime probability of packet corruption must be between 0 and 1.");
    exit(0);
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
private DatagramPacket clientGremlin(DatagramPacket responsePacket, CORRUPT_PROBABILITY) {
    int numBytesToCorrupt; // We can define a global variable to cross-reference this number and the indeces we modify on the next line so we know how many and which bytes we corrupt/damage
    int corruptIndexOne, corruptIndexTwo, corruptIndexThree;
    DecimalFormat df = new DecimalFormat("%.1f");
    double randomProbability = df.format(Math.random()); // THIS will always return random num betweeen 0 and 1
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
        randomProbability = df.format(Math.random());

        /** This condition will scale to handle cases of variable packet length
         *  s.t. the packet length is between 4 and 256
         */
        if (responseData.length < 256 && responseData.length > 3) {
            // Conditional check for 50% chance of damaging 1 byte (0.0 <= P(X) < 0.5)
            if (0 <= randomProbability && randomProbability <= corruptOneUpperBound) {
                numBytesToCorrupt = 1;
                corruptIndexOne = getRandomIntBetween(0, responseData.length);
                responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
            }
            // Conditional check for 30% chance of damaging 2 bytes (0.5 <= P(X) < 0.8)
            else if (corruptOneUpperBound <= randomProbability && randomProbability <= corruptTwoUpperBound) {
                numBytesToCorrupt = 2;
                corruptIndexOne = getRandomIntBetween(0, (1/2)*responseData.length);
                corruptIndexTwo = getRandomIntBetween((1/2)*responseData.length, responseData.length);
                responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
            }
            // Conditional check for 20% chance of damaging 3 bytes (0.8 <= P(X) < 1)
            else {
                numBytesToCorrupt = 3;
                corruptIndexOne = getRandomIntBetween(0, (1/3)*responseData.length;
                corruptIndexTwo = getRandomIntBetween((1/3)*responseData.length, (2/3)*responseData.length);
                corruptIndexThree = getRandomIntBetween((2/3)*responseData.length, responseData.length);
                responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
                responseData[corruptIndexThree] = reverseBitsByte(responseData[corruptIndexThree]);
            }
        }
        // Edge cases where packet length <= 3
        else {
            // If there is only one byte in the packet to consider
            if (responseData.length == 1) {
                /**
                 *  Here the probabilistic determination for which bytes get affected doesn't matter, because
                 *  there is only one byte to consider in this packet.
                 */
                 numBytesToCorrupt = 1;
                 corruptIndexOne = 0;
                 responseData[0] = reverseBitsByte(responseData[0]);
            }
            // If there are only two bytes in the packet to consider
            else if (responseData.length == 2) {
                    // 50% chance of damaging 1 byte
                if (0 <= randomProbability && randomProbability <= corruptOneUpperBound) {
                    numBytesToCorrupt = 1;
                    corruptIndexOne = getRandomIntBetween(0, 2);
                    responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                }
                    // 20% chance of damaging 3 bytes or 30% chance of damaging 2 bytes
                    // Since we only have 2 bytes to consider in this packet, we will damage both bytes. We don't have a
                    // third byte to consider.
                else {
                    numBytesToCorrupt = 2;
                    corruptIndexOne = 0;
                    corruptIndexTwo = 1;
                    responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                    responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
                }
            }
            // If there are only 3 bytes in the packet to consider
            else if (responseData.length == 3) {
                // 50% chance of damaging 1 byte
                if (0 <= randomProbability && randomProbability <= corruptOneUpperBound) {
                    numBytesToCorrupt = 1;
                    corruptIndexOne = getRandomIntBetween(0, 3);
                    responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                }
                // 30% chance of damaging 2 bytes
                else if (corruptOneUpperBound <= randomProbability && randomProbability <= corruptTwoUpperBound) {
                    numBytesToCorrupt = 2;
                    corruptIndexOne = getRandomIntBetween(0, 3);

                    // Randomly choose a byte index != to an index already selected
                    do {
                        corruptIndexTwo = getRandomIntBetween(0, 3);
                    } while (corruptIndexTwo == corruptIndexOne)

                    responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                    responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
                }
                // 20% chance of damaging 3 bytes
                else {
                    numBytesToCorrupt = 3;
                    corruptIndexOne = 0;
                    corruptIndexTwo = 1;
                    corruptIndexThree = 2;
                    responseData[corruptIndexOne] = reverseBitsByte(responseData[corruptIndexOne]);
                    responseData[corruptIndexTwo] = reverseBitsByte(responseData[corruptIndexTwo]);
                    responseData[corruptIndexThree] = reverseBitsByte(responseData[corruptIndexThree]);
                }
            }
        }
    }
    // Packet is either altered or unaltered based on specified probability -- return
    return responsePacket;
}

// Simply gets a random int value between two numbers
private int getRandomIntBetween(int low, int high) {
    Random rand = new Random();
    return rand.nextInt(high - low) + low;
}

// Simply reverses the bits of a byte
public byte reverseBitsByte(byte x) {
  int intSize = 8;
  byte y = 0;
  for(int position = intSize - 1; position > 0; position-- ) {
    y += ((x&1) << position);
    x >>= 1;
  }
  return y;
}

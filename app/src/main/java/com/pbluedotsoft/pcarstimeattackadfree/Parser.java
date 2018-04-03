package com.pbluedotsoft.pcarstimeattackadfree;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by daniel on 10/03/18.
 *
 */

public class Parser {

    private static final String TAG = Parser.class.getSimpleName();

    int playerIndex;
    int gameState;
    int raceFlag;
    int lapNum;
    int sectorNum;
    float bestLap;
    float lastLap;
    float lastSec;
    int invalidLap;

    public void parse(byte[] packetBytes) {
        // TODO: check playerIndex in online multi player
        if (playerIndex != 0 && playerIndex != 255 && (packetBytes[4] & 0xFF) == 0) {
            // jump corrupted 1367 that gives 0 as index
//            Log.d(TAG, "corrupted................................................... 1367");
        } else {
            playerIndex = packetBytes[4] & 0xFF;
        }

//        Log.d(TAG, "playerIndex: " + playerIndex);

        gameState = packetBytes[3] & 0xFF;
        raceFlag = packetBytes[10] & 0xFF;
        invalidLap = ((packetBytes[464 + (playerIndex * 16) + 9] & 0xFF) >> 7);
        lapNum = packetBytes[464 + (playerIndex * 16) + 10] & 0xFF;
        sectorNum = packetBytes[464 + (playerIndex * 16) + 11] & 0x07;
        bestLap = ByteBuffer.wrap(packetBytes, 12, 4).order(ByteOrder.LITTLE_ENDIAN)
                .getFloat();
        lastLap = ByteBuffer.wrap(packetBytes, 16, 4).order(ByteOrder.LITTLE_ENDIAN)
                .getFloat();
        lastSec = ByteBuffer.wrap(packetBytes, 464 + (playerIndex * 16) + 12, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

}

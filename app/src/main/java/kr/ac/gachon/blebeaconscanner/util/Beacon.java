/**
 * Radius Networks, Inc.
 * http://www.radiusnetworks.com
 *
 * @author David G. Young
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package kr.ac.gachon.blebeaconscanner.util;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;

/**
 * The <code>Beacon</code> class represents a single hardware Beacon detected by
 * an Android device.
 *
 * <pre>An Beacon is identified by a three part identifier based on the fields
 * proximityUUID - a string UUID typically identifying the owner of a
 *                 number of Beacons
 * major - a 16 bit integer indicating a group of Beacons
 * minor - a 16 bit integer identifying a single Beacon</pre>
 *
 * An Beacon sends a Bluetooth Low Energy (BLE) advertisement that contains these
 * three identifiers, along with the calibrated tx power (in RSSI) of the
 * Beacon's Bluetooth transmitter.
 *
 * This class may only be instantiated from a BLE packet, and an RSSI measurement for
 * the packet.  The class parses out the three part identifier, along with the calibrated
 * tx power.  It then uses the measured RSSI and calibrated tx power to do a rough
 * distance measurement (the accuracy field) and group it into a more reliable buckets of
 * distance (the proximity field.)
 *
 * @author David G. Young
 */
public class Beacon {
    /**
     * Less than half a meter away
     */
    public static final int PROXIMITY_IMMEDIATE = 1;
    /**
     * More than half a meter away, but less than four meters away
     */
    public static final int PROXIMITY_NEAR = 2;
    /**
     * More than four meters away
     */
    public static final int PROXIMITY_FAR = 3;
    /**
     * No distance estimate was possible due to a bad RSSI value or measured TX power
     */
    public static final int PROXIMITY_UNKNOWN = 0;

    final private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String TAG = "Beacon";

    /**
     * A 16 byte UUID that typically represents the company owning a number of Beacons
     * Example: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0 
     */

    //for eddystone , values
    protected int beaconType = 0;
    protected String nameSpace;
    protected String instance;
    protected String url;

    // beaconType , 0 = ibeacon , 1 = eddyStoneUID
    protected String proximityUuid;
    /**
     * A 16 bit integer typically used to represent a group of Beacons
     */
    protected int major;
    /**
     * A 16 bit integer that identifies a specific Beacon within a group
     */
    protected int minor;
    /**
     * An integer with four possible values representing a general idea of how far the Beacon is away
     * @see #PROXIMITY_IMMEDIATE
     * @see #PROXIMITY_NEAR
     * @see #PROXIMITY_FAR
     * @see #PROXIMITY_UNKNOWN
     */
    protected Integer proximity;
    /**
     * A double that is an estimate of how far the Beacon is away in meters.  This name is confusing, but is copied from
     * the iOS7 SDK terminology.   Note that this number fluctuates quite a bit with RSSI, so despite the name, it is not
     * super accurate.   It is recommended to instead use the proximity field, or your own bucketization of this value. 
     */
    protected Double accuracy;
    /**
     * The measured signal strength of the Bluetooth packet that led do this Beacon detection.
     */
    protected int rssi;
    /**
     * The calibrated measured Tx power of the Beacon in RSSI
     * This value is baked into an Beacon when it is manufactured, and
     * it is transmitted with each packet to aid in the distance estimate
     */
    protected int txPower;

    /**
     * If multiple RSSI samples were available, this is the running average
     */
    protected Double runningAverageRssi = null;

    protected Beacon(Beacon otherBeacon) {
        if(otherBeacon.beaconType==0) {
            this.major = otherBeacon.major;
            this.minor = otherBeacon.minor;
            this.accuracy = otherBeacon.accuracy;
            this.proximity = otherBeacon.proximity;
            this.rssi = otherBeacon.rssi;
            this.proximityUuid = otherBeacon.proximityUuid;
            this.txPower = otherBeacon.txPower;
            this.beaconType = otherBeacon.beaconType;
        }else if(otherBeacon.beaconType==1)
        {
            this.beaconType=otherBeacon.beaconType;
            this.nameSpace=otherBeacon.nameSpace;
            this.instance=otherBeacon.instance;
            this.rssi=otherBeacon.rssi;
            this.txPower=otherBeacon.txPower;
        }
    }

    protected Beacon() {

    }


    /**
     * Construct an Beacon from a Bluetooth LE packet collected by Android's Bluetooth APIs
     *
     * @param scanData The actual packet bytes
     * @param rssi The measured signal strength of the packet
     * @return An instance of an <code>Beacon</code>
     */

    public static Beacon fromScanData(byte[] scanData, int rssi) {
        int startByte = 0;

        int scanLength = scanData.length;
        while (startByte<=11)
        {
            // Check that this has the right pattern needed for this to be Eddystone-UID
            if (scanData[startByte + 0] == (byte) 0xaa && scanData[startByte + 1] == (byte) 0xfe &&//eddystone
                    scanData[startByte + 2] == (byte) 0x00) {
                // This is an Eddystone-UID beacon.


                byte[] namespaceIdentifierBytes = Arrays.copyOfRange(scanData, startByte + 4, startByte + 13);
                byte[] instanceIdentifierBytes = Arrays.copyOfRange(scanData, startByte + 14, startByte + 19);
                String nameSpaceString = bytesToHex(namespaceIdentifierBytes);
                String instanceString = bytesToHex(instanceIdentifierBytes);

                Log.d("디버깅","네임스페이스" +nameSpaceString );
                Beacon beacon = new Beacon();
                beacon.nameSpace = nameSpaceString;
                beacon.instance=instanceString;
                beacon.beaconType = 1;
                beacon.rssi=rssi;
                beacon.txPower = (int) scanData[startByte + 3];
                return beacon;

                //    String hexString = bytesToHex(proximityUuidBytes);
                // TODO: do something with the above identifiers here
            }
            else if(scanData[startByte + 0] == (byte) 0xaa && scanData[startByte + 1] == (byte) 0xfe &&//eddystone URL
                    scanData[startByte + 2] == (byte) 0x10)
            {
                String prefix;
                if(scanData[startByte+4]==(byte)0x00)
                    prefix="http://www.";
                else if(scanData[startByte+4]==(byte)0x01)
                    prefix="https://www.";
                else if(scanData[startByte+4]==(byte)0x02)
                    prefix="http://";
                else
                    prefix="https://";
                String encodedURLString=prefix;
                byte[] encodedURL = Arrays.copyOfRange(scanData, startByte + 5, scanData.length-1);
                encodedURLString+=getURL(encodedURL);


                Beacon beacon = new Beacon();
                beacon.beaconType = 2;
                beacon.url = encodedURLString;
                beacon.rssi=rssi;
                beacon.txPower = (int) scanData[startByte + 3];
                return beacon;
            }
            startByte++;
        }



        startByte=0;
        while (startByte <= 5) {

            if (((int) scanData[startByte] & 0xff) == 0x4c &&
                    ((int) scanData[startByte + 1] & 0xff) == 0x00 &&
                    ((int) scanData[startByte + 2] & 0xff) == 0x02 &&
                    ((int) scanData[startByte + 3] & 0xff) == 0x15) {//Beacon

                Beacon beacon = new Beacon();
                beacon.beaconType = 0;
                beacon.major = (scanData[startByte + 20] & 0xff) * 0x100 + (scanData[startByte + 21] & 0xff);
                beacon.minor = (scanData[startByte + 22] & 0xff) * 0x100 + (scanData[startByte + 23] & 0xff);
                beacon.txPower = (int) scanData[startByte + 24]; // this one is signed
                beacon.rssi = rssi;

                byte[] proximityUuidBytes = new byte[16];
                System.arraycopy(scanData, startByte + 4, proximityUuidBytes, 0, 16);
                String hexString = bytesToHex(proximityUuidBytes);
                StringBuilder sb = new StringBuilder();

                sb.append(hexString.substring(0, 8));
                sb.append("-");
                sb.append(hexString.substring(8, 12));
                sb.append("-");
                sb.append(hexString.substring(12, 16));
                sb.append("-");
                sb.append(hexString.substring(16, 20));
                sb.append("-");
                sb.append(hexString.substring(20, 32));

                beacon.proximityUuid = sb.toString();
                return beacon;
                // yes!  This is an Beacon
                //patternFound = true;
            }

            else if (((int)scanData[startByte] & 0xff) == 0x2d &&

                    ((int)scanData[startByte+1] & 0xff) == 0x24 &&

                    ((int)scanData[startByte+2] & 0xff) == 0xbf &&

                    ((int)scanData[startByte+3] & 0xff) == 0x16) {

                // this is an Estimote beacon

                Beacon beacon = new Beacon();

                beacon.major = 0;

                beacon.minor = 0;

                beacon.proximityUuid = "00000000-0000-0000-0000-000000000000";

                beacon.txPower = -55;

                return beacon;

            }

            startByte++;

        }




        // need at least 24 bytes for AltBeacon
        // Check that this has the right pattern needed for this to be AltBeacon
        // Beacon has a slightly different layout.  Do a Google search to find it.

        return null;
    }

    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        Log.d(TAG, "calculating accuracy based on rssi of " + rssi);


        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            Log.d(TAG, " avg rssi: " + rssi + " accuracy: " + accuracy);
            return accuracy;
        }
    }
    protected static String getURL(byte [] bytes)
    {
        String result ="";
        boolean flag=true;
        String []endString = {
                ".com/",".org/",".edu/",".net/",".info/",".biz/"
                ,".gov/",".com",".org",".edu",".net",".info",".biz",".gov"
        };

        for(int i=0;i<bytes.length;i++) {


                char character = (char)bytes[i];
                result+= character;

        }

        return result;
    }
    protected static int calculateProximity(double accuracy) {
        if (accuracy < 0) {
            return PROXIMITY_UNKNOWN;
            // is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
            // accuracy is -1;
        }
        if (accuracy < 0.5) {
            return Beacon.PROXIMITY_IMMEDIATE;
        }
        // forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
        if (accuracy <= 4.0) {
            return Beacon.PROXIMITY_NEAR;
        }
        // if it is > 4.0 meters, call it far
        return Beacon.PROXIMITY_FAR;

    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * @see #accuracy
     * @return accuracy
     */
    public double getAccuracy() {
        if (accuracy == null) {
            accuracy = calculateAccuracy(txPower, runningAverageRssi != null ? runningAverageRssi : rssi);
        }
        return accuracy;
    }

    /**
     * @see #major
     * @return major
     */
    public int getMajor() {
        return major;
    }

    /**
     * @see #minor
     * @return minor
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @see #proximity
     * @return proximity
     */
    public int getProximity() {
        if (proximity == null) {
            proximity = calculateProximity(getAccuracy());
        }
        return proximity;
    }

    /**
     * @see #rssi
     * @return rssi
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * @see #txPower
     * @return txPowwer
     */
    public int getTxPower() {
        return txPower;
    }

    /**
     * @see #proximityUuid
     * @return proximityUuid
     */
    public String getProximityUuid() {
        return proximityUuid;
    }

    @Override
    public int hashCode() {
        return minor;
    }

    /**
     * Two detected Beacons are considered equal if they share the same three identifiers, regardless of their distance or RSSI.
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof Beacon)) {
            return false;
        }
        Beacon thatBeacon = (Beacon) that;
        return (thatBeacon.getMajor() == this.getMajor() && thatBeacon.getMinor() == this.getMinor() && thatBeacon.getProximityUuid().equals(this.getProximityUuid()));
    }

    @SuppressLint("DefaultLocale")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("beaconType").append(beaconType).append("\n");


        if(this.beaconType==0) {
            sb.append("UUID=").append(this.proximityUuid.toUpperCase());
            sb.append(" Major=").append(this.major);
            sb.append(" Minor=").append(this.minor);
            sb.append(" TxPower=").append(this.txPower);
        }
        else if (this.beaconType==1)
        {
            sb.append("Namespace=").append(this.nameSpace);
            sb.append(" Instance=").append(this.instance);
            sb.append(" TxPower=").append(this.txPower);
        }else if(this.beaconType==2)
        {
            sb.append("EddystoneURL=").append(this.url);
            sb.append(" TxPower=").append(this.txPower);
        }

        return sb.toString();
    }

    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.proximityUuid.toUpperCase(Locale.ENGLISH)).append(",");
        sb.append(this.major).append(",");
        sb.append(this.minor).append(",");
        sb.append(this.txPower);

        return sb.toString();
    }
}

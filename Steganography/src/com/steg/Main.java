/*
[March 2020]
Info:
    ENCRYPTION:
        -general: the goal is to hide a text message inside of an image and therefore
                 replace the 2 least significant bits of the rgb color channels with
                 the message bits
        -input: an 24-bit-image (enter path as parameter of prepImage()) and a message (user input)
                    ->can't enter a new line character inside the message due to user input scanner (executes if you press the enter key)
        -output: new image ("SecretImg.png")
    DECRYPTION:
        -general: the goal is to extract the text message back out of the img
        -input: "SecretImg.png"
        -output: message (sometimes the last or last few characters are wrong, haven´t found the fault yet)
 */

package com.steg;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.ColorModel;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;

public class Main{
    public static void main(String[] args) throws IOException {
        //input image path
        String inImgName = "testInput.jpg";
        ColorModel color = ImageIO.read(new File(inImgName)).getColorModel();
        if (color.getPixelSize() != 24) {
            System.out.println("24-bit-color needed, this is: " + color.getPixelSize() + "-bit. Choose another image. Kill program.");
            System.exit(0);
        }
        System.out.println("24-bit-color, ready to continue.\n");
        Encryption.prepImage(inImgName);
        Encryption.MsgToBin();
        Encryption.replaceBits();
        Encryption.secretImage();
        Encryption.getMsgLength();
        Decryption.prepSecretImg();
        Decryption.getBits();
        Decryption.displayMsg();
    }

    static class Encryption {
        //global variables
        static int w, h;
        static ArrayList<String> binR;
        static ArrayList<String> binG;
        static ArrayList<String> binB;
        public static int msgLength;
        static ArrayList<String> lsb1;
        static ArrayList<String> lsb2;
        private static String data;
        static String bit1, bit2, bit3, bit4, bit5, bit6;

        //prepare the input image (load, get rgb)
        public static void prepImage(String imgName) {
            try {
                //import image
                File path = new File(imgName);
                BufferedImage originalImg = ImageIO.read(path);
                h = originalImg.getHeight();
                w = originalImg.getWidth();
                binR = new ArrayList<>();
                binG = new ArrayList<>();
                binB = new ArrayList<>();

                //draw originalImg on to new bImg so I can be sure that I get TYPE_INT_RGB
                BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bImg.createGraphics();
                g2d.drawImage(originalImg, 0, 0, null);
                g2d.dispose();
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int color = bImg.getRGB(x, y);
                        int r = (color & 0x00ff0000) >> 16;
                        int g = (color & 0x0000ff00) >> 8;
                        int b = color & 0x000000ff;
                        StringBuilder StrR = new StringBuilder(Integer.toBinaryString(r));
                        StringBuilder StrG = new StringBuilder(Integer.toBinaryString(g));
                        StringBuilder StrB = new StringBuilder(Integer.toBinaryString(b));
                        while (StrR.length() < 8) {
                            StrR.insert(0, "0");
                        }
                        while (StrG.length() < 8) {
                            StrG.insert(0, "0");
                        }
                        while (StrB.length() < 8) {
                            StrB.insert(0, "0");
                        }
                        binR.add(StrR.toString());
                        binG.add(StrG.toString());
                        binB.add(StrB.toString());
                    }
                }
                System.out.println("ENCRYPTION:\n");
                //check if all the pixels are stored in binR,G,B
                if (binR.size() + binG.size() + binB.size() != (w * h) * 3) {
                    System.out.println("Not all pixels were loaded properly. Kill program.");
                    System.exit(0);
                }
                System.out.println("All pixels transferred to binary RGB arrays.");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //prepare the message (load, transfer text to binary)
        public static void MsgToBin() {
            StringBuilder strB = new StringBuilder();
            try {
                //write user input (message) to inputMsg.txt file
                System.out.print("Enter your text message:\n");
                Scanner scan = new Scanner(System.in);
                String msg = scan.nextLine();
                FileWriter fWriter;
                BufferedWriter writer;
                fWriter = new FileWriter("inputMsg.txt");
                writer = new BufferedWriter(fWriter);
                writer.write(msg);
                writer.close();
                //get message
                InputStream in = new FileInputStream("inputMsg.txt");
                //get the message length and check if inputImg is big enough for the message
                msgLength = in.available();
                if(w*h < Math.floor((float)(msgLength*8)/3)){
                    System.out.println("The message you entered is too long to encrypt in this image.\nExit program.");
                    System.exit(0);
                }
                System.out.println("Message length (characters and line breaks): " + msgLength);
                for (int b; (b = in.read()) != -1; ) {  //if b == -1 -> end of Stream
                    String s = Integer.toBinaryString(b);
                    //make sure all ascii characters are 8bit long
                    for (int i = 1; i < 8; i++) {
                        if (s.length() == i) {
                            strB.append("0".repeat(8 - s.length()));
                        }
                    }
                    strB.append(s).append(' ');
                }
                in.close();

                //binary out
                File binOut = new File("binMsg.dat");
                binOut.createNewFile();
                Path out = Paths.get("binMsg.dat");
                BufferedReader checkIfOutEmpty = new BufferedReader(new FileReader(String.valueOf(out)));
                if (checkIfOutEmpty.readLine() != null) {   //clearing file before writing to it
                    PrintWriter pw = new PrintWriter(String.valueOf(out));
                    pw.close();
                    System.out.println("Output file cleared, ready to write.");
                }
                Files.write(out, Collections.singleton(strB));
                System.out.println("Binary data transferred to output file.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //replace the 7th and 8th bit of rgb with the message bits
        public static void replaceBits() {
            //get bits from binMsg.dat
            try {
                //first get rid of whitespaces
                File outRead = new File("binMsg.dat");
                lsb1 = new ArrayList<>();
                lsb2 = new ArrayList<>();
                Scanner FileScan = new Scanner(outRead);
                while (FileScan.hasNextLine()) {
                    data = FileScan.nextLine();
                    data = data.replace(" ", "");

                    //add data to 2 least significant bit arrays (7th and 8th bit)
                    for (int i = 0; i < data.length(); i++) {
                        char b1 = data.charAt(i);
                        char b2 = data.charAt(i += 1);
                        lsb1.add(Character.toString(b1));
                        lsb2.add(Character.toString(b2));
                    }
                    System.out.println("\nMESSAGE DATA:\nLSB1: " + lsb1 + "\nLSB2: " + lsb2 + "\n");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }

            //replace 7th and 8th bit from "old" binR,G,B with 2 bits at a time from binMsg.dat
            try {
                int x = data.length() / 6;    //number of iterations with 6 bit (one pixel set)
                System.out.println("FULL ITERATIONS TO DO: " + x);
                int y = data.length() % 6;    //how many bits left after 'full' iterations
                System.out.println("PART ITERATIONS TO DO: " + y);
                //full iterations
                ArrayList<StringBuilder> reds = new ArrayList<>();
                ArrayList<StringBuilder> greens = new ArrayList<>();
                ArrayList<StringBuilder> blues = new ArrayList<>();
                for (int j = 0; j < x; j++) {
                    //red
                    int i = j * 3;
                    bit1 = lsb1.get(i);
                    bit2 = lsb2.get(i);
                    StringBuilder r = new StringBuilder(binR.get(j));
                    r.setCharAt(6, bit1.charAt(0));
                    r.setCharAt(7, bit2.charAt(0));
                    reds.add(r);


                    //green
                    bit3 = lsb1.get(i + 1);
                    bit4 = lsb2.get(i + 1);
                    StringBuilder g = new StringBuilder(binG.get(j));
                    g.setCharAt(6, bit3.charAt(0));
                    g.setCharAt(7, bit4.charAt(0));
                    greens.add(g);

                    //blue
                    bit5 = lsb1.get(i + 2);
                    bit6 = lsb2.get(i + 2);
                    StringBuilder b = new StringBuilder(binB.get(j));
                    b.setCharAt(6, bit5.charAt(0));
                    b.setCharAt(7, bit6.charAt(0));
                    blues.add(b);
                }
                System.out.println("Done with full iterations.\n");

                //rest iteration (y has to be 2 or 4 because y is even and y!=6)
                //start bit has to be in lsb1 (lsb1 and 2 have the same length because data.length() is even) and 6 is even too
                int k = x * 3;    //there were (x*3)-1 iterations before
                String b1 = lsb1.get(k - 1);
                String b2 = lsb2.get(k - 1);
                if (y == 2) {
                    //put 2 bits in red
                    StringBuilder r = new StringBuilder(binR.get(x));
                    r.setCharAt(6, b1.charAt(0));
                    r.setCharAt(7, b2.charAt(0));
                    reds.add(r);
                }
                if (y == 4) {
                    //put 2 bits in red
                    StringBuilder r = new StringBuilder(binR.get(x));
                    r.setCharAt(6, b1.charAt(0));
                    r.setCharAt(7, b2.charAt(0));
                    reds.add(r);
                    //put 2 bits in green
                    String b3 = lsb1.get(k + 1);
                    String b4 = lsb2.get(k + 1);
                    StringBuilder g = new StringBuilder(binG.get(x));
                    g.setCharAt(6, b3.charAt(0));
                    g.setCharAt(7, b4.charAt(0));
                    greens.add(g);
                }

                for (int i = 0; i < reds.toString().length() / 10; i++) {
                    binR.set(i, reds.get(i).toString());
                }
                for (int i = 0; i < greens.toString().length() / 10; i++) {
                    binG.set(i, greens.get(i).toString());
                }
                for (int i = 0; i < blues.toString().length() / 10; i++) {
                    binB.set(i, blues.get(i).toString());
                }

                System.out.println("All bits are replaced.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //create a new "SecretImg.png" which includes the message data (binary to int, rotate img, write new img)
        public static void secretImage() throws IOException {
            //TO_INTEGER: get the string part to int
            System.out.println("\nNEW IMAGE:");
            int perColor = w * h;
            int total = (w * h) * 3;
            System.out.println("Size of pixels per color channel: " + perColor + ". Total: " + total);
            int[] pix = new int[total];    //int array with rgb pixel values
            for (int i = 0; i < perColor; i++) {
                int a = i * 3;  //pix: (r0, g0, b0, r1, g1, b1, ...) and pix.indexOf(r1)==a==3==i*3
                String byteR = binR.get(i);
                String byteG = binG.get(i);
                String byteB = binB.get(i);

                int j = Integer.parseInt(byteR, 2);   //base 2 (binary) to int
                pix[a] = j;
                int k = Integer.parseInt(byteG, 2);
                pix[a + 1] = k;
                int l = Integer.parseInt(byteB, 2);
                pix[a + 2] = l;
            }

            //ROTATE img (90° to the right, then flip it vertically)
            BufferedImage notRotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            System.out.println("Pix size: " + pix.length);
            notRotated.getRaster().setPixels(0, 0, w, h, pix);   //or try setDataElements
            h = notRotated.getHeight();
            w = notRotated.getWidth();
            BufferedImage secretImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = secretImg.createGraphics();
            g2d.rotate(Math.toRadians(90), (float) w / 2, (float) h / 2);

            // FLIP img
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -h);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            notRotated = op.filter(notRotated, null);
            g2d.drawImage(notRotated, 0, 0, null);
            g2d.dispose();

            //WRITE new image
            File outFile = new File("SecretImg.png");
            ImageIO.write(secretImg, "png", outFile);
            System.out.println("Image with encrypted data saved at '" + outFile + "'.");
        }

        //so that I can access the msgLength in Decryption class
        public static int getMsgLength() {
            return msgLength;
        }
    }


    static class Decryption {
        //global variables
        static ArrayList<String> binR;
        static ArrayList<String> binG;
        static ArrayList<String> binB;
        static String r7, r8, g7, g8, b7, b8;   //the data bits
        private static ArrayList<String> reds = new ArrayList<>();
        private static ArrayList<String> greens = new ArrayList<>();
        private static ArrayList<String> blues = new ArrayList<>();
        private static int msgLength = Encryption.getMsgLength();

        //get the "SecretImg.png" and get the rgb (binary)
        public static void prepSecretImg() {
            try {
                //import image
                File inputImg = new File("SecretImg.png");
                BufferedImage img = ImageIO.read(inputImg);

                //get rgb arrays
                int h = img.getHeight();
                int w = img.getWidth();
                binR = new ArrayList<>();
                binG = new ArrayList<>();
                binB = new ArrayList<>();
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        int color = img.getRGB(x, y);
                        int r = (color & 0x00ff0000) >> 16;
                        int g = (color & 0x0000ff00) >> 8;
                        int b = color & 0x000000ff;
                        StringBuilder StrR = new StringBuilder(Integer.toBinaryString(r));
                        StringBuilder StrG = new StringBuilder(Integer.toBinaryString(g));
                        StringBuilder StrB = new StringBuilder(Integer.toBinaryString(b));
                        while (StrR.length() < 8) {
                            StrR.insert(0, "0");
                        }
                        while (StrG.length() < 8) {
                            StrG.insert(0, "0");
                        }
                        while (StrB.length() < 8) {
                            StrB.insert(0, "0");
                        }
                        binR.add(StrR.toString());
                        binG.add(StrG.toString());
                        binB.add(StrB.toString());
                    }
                }
                System.out.println("\nDECRYPTION:\n");
                if (binR.size() + binG.size() + binB.size() != (w * h) * 3) {
                    System.out.println("Could not get all or too many rgb values in Decryption. Kill program.");
                    System.exit(0);
                }
                System.out.println("All rgb values transferred to arrays.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //retrieve the last 2 bits from the rgb arrays (only the msgLength bytes)
        public static void getBits() {
            try {
                //get inputMsg.txt length
                int MsgLen = msgLength * 8;

                //get 7th and 8th bit of rgb (storage: reds/greens/blues = [7[0]8[0], 7[1]8[1], ...])
                int f = MsgLen / 6;
                int p = (MsgLen % 6) / 2;   //2 bits per pair
                System.out.println("There are " + f + " full iterations (6 bits each) and " + p + " lsb pairs (2 bits each) left.");
                //full iterations (3*2bit)
                for (int i = 0; i < f; i++) {
                    //red
                    String r = binR.get(i);
                    r7 = String.valueOf(r.charAt(6));
                    r8 = String.valueOf(r.charAt(7));
                    reds.add(r7 + r8);
                    //greens
                    String g = binG.get(i);
                    g7 = String.valueOf(g.charAt(6));
                    g8 = String.valueOf(g.charAt(7));
                    greens.add(g7 + g8);
                    //blues
                    String b = binB.get(i);
                    b7 = String.valueOf(b.charAt(6));
                    b8 = String.valueOf(b.charAt(7));
                    blues.add(b7 + b8);
                }
                //part iterations (either r->p==1 or r&&g->p==2 left)
                if (p == 1) {
                    //red
                    String r = binR.get(f); //there were f iterations before
                    r7 = String.valueOf(r.charAt(6));
                    r8 = String.valueOf(r.charAt(7));
                    reds.add(r7 + r8);
                }
                if (p == 2) {
                    //red
                    String r = binR.get(f);
                    r7 = String.valueOf(r.charAt(6));
                    r8 = String.valueOf(r.charAt(7));
                    reds.add(r7 + r8);
                    //greens
                    String g = binG.get(f);
                    g7 = String.valueOf(g.charAt(6));
                    g8 = String.valueOf(g.charAt(7));
                    greens.add(g7 + g8);
                }

                //check if all lsb were pushed into rgb arrays
                if ((reds.size() + greens.size() + blues.size()) * 2 != MsgLen) {
                    System.out.println("Not all lsb were transferred to the array. Kill program.");
                    System.exit(0);
                }
                System.out.println("All lsb are stored.\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //display the extracted message
        public static void displayMsg() {
            try {
                //put all reds, greens, blues in a String
                ArrayList<String> binMsg = new ArrayList<>();
                //smallest array length -> full iterations
                int minLen = Math.min(reds.size(), Math.min(greens.size(), blues.size()));
                for (int i = 0; i < minLen; i++) {
                    binMsg.add(reds.get(i));
                    binMsg.add(greens.get(i));
                    binMsg.add(blues.get(i));
                }
                //part iterations
                if (reds.size() > minLen && greens.size() == minLen && blues.size() == minLen) {
                    binMsg.add(reds.get(minLen));
                }
                if (reds.size() > minLen && greens.size() > minLen && blues.size() == minLen) {
                    binMsg.add(reds.get(minLen));
                    binMsg.add(greens.get(minLen));
                }
                System.out.println("Done with adding to binMsg.\n\n");

                //displaying the message
                System.out.println("MESSAGE:\n***");
                for (int i = 0; i < binMsg.size() / 4; i++) {
                    int j = i * 4;
                    String newByte = binMsg.get(j) + binMsg.get(j + 1) + binMsg.get(j + 2) + binMsg.get(j + 3);
                    char msgChar = (char) Integer.parseInt(newByte, 2);
                    System.out.print(msgChar);
                }
                System.out.println("\n***\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
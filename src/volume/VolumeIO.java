/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author michel
 */
public class VolumeIO {
    
    public VolumeIO(File file) throws IOException {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
            
            stream.mark(2000);
            byte[] magicCode = new byte[16];
            if (stream.read(magicCode) == 16) {
                String headerText = new String(magicCode);
                if (headerText.compareTo("# AVS field file") != 0) {
                    System.out.println("not a valid file");
                } 
            } 
            stream.reset();
            int headerLength = 1;
            while (stream.read() != '\f') {
                headerLength++;
            }
            // skip also next ^L
            headerLength++;
            stream.reset();
            
            byte[] h = new byte[headerLength];
            if (stream.read(h) == headerLength) {
                String header = new String(h);
                parseHeader(header);
            }

            int byteCount = x_dim*y_dim*z_dim*data_type;
            byte[] d = new byte[byteCount];

            if (stream.read(d) == byteCount) {
                //System.out.println("read " + byteCount + " bytes");
            }
            
            data = new short[x_dim * y_dim * z_dim];
            if (data_type == 1) { //bytes
                for (int i=0; i<byteCount; i++) {
                    data[i] = (short) (d[i] & 0xFF);
                }
            } else if (data_type == 2) { //shorts
                for (int i=0; i<byteCount; i+=2) {
                    short value = (short) ((d[i]&0xFF) + (d[i+1]&0xFF)*256);
                    data[i/2] = value;
                }
            }
            
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    public int getXDim() {
        return x_dim;
    }
    
    public int getYDim() {
        return y_dim;
    }
    
    public int getZDim() {
        return z_dim;
    }

    public short[] getData() {
        return data;
    }
    
    private void parseHeader(String header) {
        
        String[] avs_keys = {"ndim", "dim1", "dim2", "dim3", "nspace", "veclen", 
                      "data", "field","min_ext", "max_ext", "variable", "#", "label", "unit",
                        "min_val", "max_val"};
        
        Scanner scanner = new Scanner(header);
        while (scanner.hasNextLine()) {
            String token = scanner.nextLine();
            if (token.indexOf('=') > 0) {
               //System.out.println("[" + token + "]");
               String[] tokens = token.split("\\s*=\\s*|\\s*#\\s*");
               //for (int i=0; i<tokens.length; i++) {
               //    System.out.print("(" + tokens[i] + ") ");
               //}
               // System.out.println();
               int avs_key_index;
               for (avs_key_index = 0; avs_key_index<avs_keys.length; avs_key_index++) {
                   if (tokens[0].compareTo(avs_keys[avs_key_index]) == 0) {
                       break;
                   }
               }
               
               switch (avs_key_index) {
                   case 0: // ndim 
                       if (Integer.parseInt(tokens[1]) != 3) {
                           System.out.println("Only 3D files supported");
                       } else {
                           //System.out.println("3D file");
                       }
                       break;
                   case 1: // dim1
                       x_dim = Integer.parseInt(tokens[1]);
                       break;
                   case 2: // dim2
                       y_dim = Integer.parseInt(tokens[1]);
                       break;
                   case 3: // dim3
                       z_dim = Integer.parseInt(tokens[1]);
                       break;
                   case 4: // nspace
                       break;
                   case 5: // veclen
                       int veclen = Integer.parseInt(tokens[1]);
                       if (veclen != 1) {
                           System.out.println("Only scalar data are supported");
                       }
                       break;
                   case 6: // data
                       data_type = -1;
                       if (tokens[1].compareTo("byte") == 0) {
                           data_type = 1;
                       }
                       if (tokens[1].compareTo("short") == 0) {
                           data_type = 2;
                       }
                       if (data_type < 0) {
                           System.out.println("Data type not recognized");
                           System.out.println("data type: " + tokens[1]);
                       }
                       break;
                   case 7: // field
                       if (tokens[1].compareTo("uniform") != 0) {
                           System.out.println("Only uniform data are supported");
                       }
                   case 8: // min_ext
                   case 9: // max_ext
                   case 10: // variable
                   case 11: 
                   case 12:
                   case 13:
                   case 14:
                   case 15:
                       break;
                   default:
                       System.out.println("Invalid AVS keyword in file");
               }
               
            }

        }
    }
    
    private int x_dim, y_dim, z_dim;
    private short[] data;
    private int data_type;
    
}

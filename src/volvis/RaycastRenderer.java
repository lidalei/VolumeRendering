/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

/**
 *
 * @author michel
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    private Volume volume = null;
    
    /*
    
    volume attributes
    
    */
    int volumeDimX = 0;
    int volumeDimY = 0;
    int volumeDimZ = 0;
    
    
    private GradientVolume gradients = null;
    // rendering method
    public String Rendering_Method = "Slicer";
    // shading parameter, true or false
    public boolean shading = false;
    
    /* 
    
    shading parameters
    
    */
    
    // ambient reflection coefficient, assuming light source is white
    TFColor SHADING_AMBIENT_COEFF = new TFColor(0.1, 0.1, 0.1, 1.0);
    // diffuse reflection coefficient
    double SHADING_DIFF_COEFF = 0.7;
    // specular reflection coefficient
    double SHADING_SPEC_COEFF = 0.2;
    // exponent used to approximate highligh
    double SHADING_ALPHA = 10;
    
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunctionEditor tfEditor;
    TransferFunction2DEditor tfEditor2D;
    
    public RaycastRenderer() {
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }

    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;
        volumeDimX = volume.getDimX();
        volumeDimY = volume.getDimY();
        volumeDimZ = volume.getDimZ();
        
        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();
        // create a standard TF where lowest intensity maps to black, the highest to white, and opacity increases
        // linearly from 0.0 to 1.0 over the intensity range
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        
        // uncomment this to initialize the TF with good starting values for the orange dataset 
        //tFunc.setTestFunc();
        
        
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        
        tfEditor2D = new TransferFunction2DEditor(volume, gradients);
        tfEditor2D.addTFChangeListener(this);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    public RaycastRendererPanel getPanel() {
        return panel;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2D;
    }
    
    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }
    
    /*
    
    get voxel intensity - using tri-linear interpolation 
    
    */
    

    short getVoxel(double[] coord) {

        
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);
        
        if (coord[0] < 0 || coord[0] >= volumeDimX - 1 || coord[1] < 0 || coord[1] >= volumeDimY - 1
                || coord[2] < 0 || coord[2] >= volumeDimZ - 1) {
            return 0;
        }
        
        // add tri-linear interpolation
        
        int val0 = volume.getVoxel(x, y, z);
        int val1 = volume.getVoxel(x + 1, y, z);
        
        int val2 = volume.getVoxel(x, y + 1, z);
        int val3 = volume.getVoxel(x + 1, y + 1, z);
        
        int val4 = volume.getVoxel(x, y, z + 1);
        int val5 = volume.getVoxel(x + 1, y, z + 1);
        
        int val6 = volume.getVoxel(x, y + 1, z + 1);
        int val7 = volume.getVoxel(x + 1, y + 1, z + 1);
        
        double alpha = coord[0] - x;
        double beta = coord[1] - y;
        double gamma = coord[2] - z;
        // four linear interpolation
        double val01 = alpha * val1 + (1 - alpha) * val0;
        double val23 = alpha * val3 + (1 - alpha) * val2;
        double val45 = alpha * val5 + (1 - alpha) * val4;
        double val67 = alpha * val7 + (1 - alpha) * val6;
        // two bi-linear interpolation
        double val0123 = beta * val23 + (1 - beta) * val01;
        double val4567 = beta * val67 + (1 - beta) * val45;
        // one tri-linear interpolation
        double finalVal = gamma * val4567 + (1 - gamma) * val0123;
        
        return (short)Math.round(finalVal);
    }
    
    /*
    
    get voxel gradient
    
    */

    
    VoxelGradient getVoxelGradient(double[] coord) {
        
        VoxelGradient voxelGradient = new VoxelGradient();
        
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);
        
        if (coord[0] < 0 || coord[0] >= volumeDimX - 1 || coord[1] < 0 || coord[1] >= volumeDimY - 1
                || coord[2] < 0 || coord[2] >= volumeDimZ - 1) {
            return voxelGradient;
        }
        
        // without tri-linear interpolation
        if(true) {
            return gradients.getGradient(x, y, z);
        }
        
        // with tri-linear interpolation        
        VoxelGradient val0 = gradients.getGradient(x, y, z);
        VoxelGradient val1 = gradients.getGradient(x + 1, y, z);
        
        VoxelGradient val2 = gradients.getGradient(x, y + 1, z);
        VoxelGradient val3 = gradients.getGradient(x + 1, y + 1, z);
        
        VoxelGradient val4 = gradients.getGradient(x, y, z + 1);
        VoxelGradient val5 = gradients.getGradient(x + 1, y, z + 1);
        
        VoxelGradient val6 = gradients.getGradient(x, y + 1, z + 1);
        VoxelGradient val7 = gradients.getGradient(x + 1, y + 1, z + 1);
        
        float alpha = (float) (coord[0] - x);
        float beta = (float) (coord[1] - y);
        float gamma = (float) (coord[2] - z);
        
        // four linear interpolation
        VoxelGradient val01 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(alpha, val1), VoxelGradient.scalarMultiplication(1 - alpha, val0));
        VoxelGradient val23 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(alpha, val3), VoxelGradient.scalarMultiplication(1 - alpha, val2));
        VoxelGradient val45 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(alpha, val5), VoxelGradient.scalarMultiplication(1 - alpha, val4));
        VoxelGradient val67 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(alpha, val7), VoxelGradient.scalarMultiplication(1 - alpha, val6));
        
        // two bi-linear interpolation
        VoxelGradient val0123 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(beta, val23), VoxelGradient.scalarMultiplication(1 - beta, val01));
        VoxelGradient val4567 = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(beta, val67), VoxelGradient.scalarMultiplication(1 - beta, val45));
        
        // one tri-linear interpolation
        VoxelGradient finalVal = VoxelGradient.voxelGradientAddition(VoxelGradient.scalarMultiplication(beta, val4567), VoxelGradient.scalarMultiplication(1 - gamma, val0123));
        
        return finalVal;
    }    
    
    /*
    
     get voxel gradient magnitude
    
    */
    
    
    double getVoxelGradientMag(double[] coord) {
        
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);
        
        if (coord[0] < 0 || coord[0] >= volumeDimX - 1 || coord[1] < 0 || coord[1] >= volumeDimY - 1
                || coord[2] < 0 || coord[2] >= volumeDimZ - 1) {
            return 0;
        }
        
        // with tri-linear interpolation
        
        double val0 = gradients.getGradient(x, y, z).mag;
        double val1 = gradients.getGradient(x + 1, y, z).mag;
        
        double val2 = gradients.getGradient(x, y + 1, z).mag;
        double val3 = gradients.getGradient(x + 1, y + 1, z).mag;
        
        double val4 = gradients.getGradient(x, y, z + 1).mag;
        double val5 = gradients.getGradient(x + 1, y, z + 1).mag;
        
        double val6 = gradients.getGradient(x, y + 1, z + 1).mag;
        double val7 = gradients.getGradient(x + 1, y + 1, z + 1).mag;
        
        double alpha = coord[0] - x;
        double beta = coord[1] - y;
        double gamma = coord[2] - z;
        
        // four linear interpolation
        double val01 = alpha * val1 + (1 - alpha) * val0;
        double val23 = alpha * val3 + (1 - alpha) * val2;
        double val45 = alpha * val5 + (1 - alpha) * val4;
        double val67 = alpha * val7 + (1 - alpha) * val6;
        // two bi-linear interpolation
        double val0123 = beta * val23 + (1 - beta) * val01;
        double val4567 = beta * val67 + (1 - beta) * val45;
        // one tri-linear interpolation
        double finalVal = gamma * val4567 + (1 - gamma) * val0123;
        
        return finalVal;
    }
    
    /*
    
    slicer redering
    
    */
    
    
    void slicer(double[] viewMatrix) {

        // clear image
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }

        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() >> 1;

        double[] voxelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX >> 1, volumeDimY >> 1, volumeDimZ >> 1);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();
        
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                voxelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
                voxelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
                voxelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];

                int val = getVoxel(voxelCoord);
                
                // Map the intensity to a grey value by linear scaling
                voxelColor.r = val / max;
                voxelColor.g = voxelColor.r;
                voxelColor.b = voxelColor.r;
                voxelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                // Alternatively, apply the transfer function to obtain a color
                // voxelColor = tFunc.getColor(val);
                
                
                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }
    }

    /*
    
    maximum intensity projection redering
    
    */
    
    void mip(double[] viewMatrix) {
        
        // clear image
        for (int j = 0; j < imageHeight; j++) {
            for (int i = 0; i < imageWidth; i++) {
                image.setRGB(i, j, 0);
            }
        }

        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);
        
        // image is square
        int imageCenter = imageWidth >> 1;

        double[] voxelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX >> 1, volumeDimY >> 1, volumeDimZ >> 1);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        
        // interactive mode
        int step = 1;
        if(interactiveMode == true) {
            step = 3;
        }

        double XStep = viewVec[0] * step;
        double YStep = viewVec[1] * step;
        double ZStep = viewVec[2] * step;
        
        TFColor pixelColor = new TFColor();
        
        for (int j = 0; j < imageHeight; j++) {
            
            double voxelCoordXStart = uVec[0] * (-1 - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
            double voxelCoordYStart = uVec[1] * (-1 - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
            double voxelCoordZStart = uVec[2] * (-1 - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
            
            for (int i = 0; i < imageWidth; i++) {
                
                voxelCoordXStart += uVec[0];
                voxelCoordYStart += uVec[1];
                voxelCoordZStart += uVec[2];
                    
                // compute intersections
                long tXMin = Long.MIN_VALUE;
                long tXMax = Long.MAX_VALUE;
                long tYMin = Long.MIN_VALUE;
                long tYMax = Long.MAX_VALUE;
                long tZMin = Long.MIN_VALUE;
                long tZMax = Long.MAX_VALUE;
                
                if(viewVec[0] != 0) {
                    tXMin = Math.round(-voxelCoordXStart / viewVec[0]);
                    tXMax = Math.round((volumeDimX - voxelCoordXStart) / viewVec[0]);;
                }
                
                if(viewVec[1] != 0) {
                    tYMin = Math.round(-voxelCoordYStart / viewVec[1]);
                    tYMax = Math.round((volumeDimY - voxelCoordYStart) / viewVec[1]);
                }
                
                if(viewVec[2] != 0) {
                    tZMin = Math.round(-voxelCoordZStart / viewVec[2]);
                    tZMax = Math.round((volumeDimZ - voxelCoordZStart) / viewVec[2]);
                }
                
                //swap value if min larger than max
                if(tXMin > tXMax) {
                    tXMin = tXMin + tXMax;
                    tXMax = tXMin - tXMax;
                    tXMin = tXMin - tXMax;          
                }
                //swap value if min larger than max
                if(tYMin > tYMax) {
                    tYMin = tYMin + tYMax;
                    tYMax = tYMin - tYMax;
                    tYMin = tYMin - tYMax;          
                }
                //swap value if min larger than max
                if(tZMin > tZMax) {
                    tZMin = tZMin + tZMax;
                    tZMax = tZMin - tZMax;
                    tZMin = tZMin - tZMax;          
                }
                
                int maxVoxelIntensity = 0;
                
                long start = Math.max(tXMin, Math.max(tYMin, tZMin));
                long end = Math.min(tXMax, Math.min(tYMax, tZMax));
                
                // ray intersects with volume
                if(start < end) {
                    voxelCoord[0] = voxelCoordXStart + (start - step) * viewVec[0];
                    voxelCoord[1] = voxelCoordYStart + (start - step) * viewVec[1];
                    voxelCoord[2] = voxelCoordZStart + (start - step) * viewVec[2];
                
                    for(long u = start; u < end; u += step){
                       voxelCoord[0] += XStep;
                       voxelCoord[1] += YStep;
                       voxelCoord[2] += ZStep;

                       int voxelIntensity = getVoxel(voxelCoord);

                       if(voxelIntensity > maxVoxelIntensity){
                           maxVoxelIntensity = voxelIntensity;
                       }
                    }
                }
                
                // Map the intensity to a grey value by linear scaling
                pixelColor.r = maxVoxelIntensity / max;
                pixelColor.g = pixelColor.r;
                pixelColor.b = pixelColor.r;
                pixelColor.a = maxVoxelIntensity > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                
                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = pixelColor.a <= 1.0 ? (int) Math.floor(pixelColor.a * 255) : 255;
                int c_red = pixelColor.r <= 1.0 ? (int) Math.floor(pixelColor.r * 255) : 255;
                int c_green = pixelColor.g <= 1.0 ? (int) Math.floor(pixelColor.g * 255) : 255;
                int c_blue = pixelColor.b <= 1.0 ? (int) Math.floor(pixelColor.b * 255) : 255;
                int finalPixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, finalPixelColor);
            } // end imageWidth
        } // end imageHeight
    } // end mip

    /*
    
    compositing rendering using 1D function
    
    */
    
    void compositing(double[] viewMatrix) {
        
        // clear image        
        for (int j = 0; j < imageHeight; j++) {
            for (int i = 0; i < imageWidth; i++) {
                image.setRGB(i, j, 0);
            }
        }
        
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = imageWidth >> 1;

        double[] voxelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX >> 1, volumeDimY >> 1, volumeDimZ >> 1);
        
        // interactive mode
        int step = 1;
        if(interactiveMode == true) {
            step = 3;
        }
        
        double XStep = viewVec[0] * step;
        double YStep = viewVec[1] * step;
        double ZStep = viewVec[2] * step;
        
        // reusable
        TFColor voxelColor = new TFColor();
        
        for (int j = 0; j < imageHeight; j ++) {
            
            double voxelCoordXStart = uVec[0] * (-1 - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
            double voxelCoordYStart = uVec[1] * (-1 - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
            double voxelCoordZStart = uVec[2] * (-1 - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
            
            for (int i = 0; i < imageWidth; i++) {
                
                TFColor pixelColor = new TFColor(0, 0, 0, 1);
                
                voxelCoordXStart += uVec[0];
                voxelCoordYStart += uVec[1];
                voxelCoordZStart += uVec[2];
                
                
                // compute intersections
                long tXMin = Long.MIN_VALUE;
                long tXMax = Long.MAX_VALUE;
                long tYMin = Long.MIN_VALUE;
                long tYMax = Long.MAX_VALUE;
                long tZMin = Long.MIN_VALUE;
                long tZMax = Long.MAX_VALUE;
                
                if(viewVec[0] != 0) {
                    tXMin = Math.round(-voxelCoordXStart / viewVec[0]);
                    tXMax = Math.round((volumeDimX - voxelCoordXStart) / viewVec[0]);;
                }
                
                if(viewVec[1] != 0) {
                    tYMin = Math.round(-voxelCoordYStart / viewVec[1]);
                    tYMax = Math.round((volumeDimY - voxelCoordYStart) / viewVec[1]);
                }
                
                if(viewVec[2] != 0) {
                    tZMin = Math.round(-voxelCoordZStart / viewVec[2]);
                    tZMax = Math.round((volumeDimZ - voxelCoordZStart) / viewVec[2]);
                }
                
                //swap value if min larger than max
                if(tXMin > tXMax) {
                    tXMin = tXMin + tXMax;
                    tXMax = tXMin - tXMax;
                    tXMin = tXMin - tXMax;          
                }
                //swap value if min larger than max
                if(tYMin > tYMax) {
                    tYMin = tYMin + tYMax;
                    tYMax = tYMin - tYMax;
                    tYMin = tYMin - tYMax;          
                }
                //swap value if min larger than max
                if(tZMin > tZMax) {
                    tZMin = tZMin + tZMax;
                    tZMax = tZMin - tZMax;
                    tZMin = tZMin - tZMax;          
                }
                
                long start = Math.max(tXMin, Math.max(tYMin, tZMin));
                long end = Math.min(tXMax, Math.min(tYMax, tZMax));
                
                // ray intersects with volume
                if(start < end) {
                    voxelCoord[0] = voxelCoordXStart + (start - step) * viewVec[0];
                    voxelCoord[1] = voxelCoordYStart + (start - step) * viewVec[1];
                    voxelCoord[2] = voxelCoordZStart + (start - step) * viewVec[2];


                    for(long u = start; u < end; u += step){
                        voxelCoord[0] += XStep;
                        voxelCoord[1] += YStep;
                        voxelCoord[2] += ZStep;

                        // use Tansfer function to tansfer an intensity to a color
                        voxelColor = tFunc.getColor(getVoxel(voxelCoord));

                        pixelColor.r = (1 - voxelColor.a) * pixelColor.r + voxelColor.a * voxelColor.r;
                        pixelColor.g = (1 - voxelColor.a) * pixelColor.g + voxelColor.a * voxelColor.g;
                        pixelColor.b = (1 - voxelColor.a) * pixelColor.b + voxelColor.a * voxelColor.b;
                        pixelColor.a = (1 - voxelColor.a) * pixelColor.a;
                    }
                    
                    pixelColor.a = 1 - pixelColor.a;
                    
                    // BufferedImage expects a pixel color packed as ARGB in an int
                    int c_alpha = pixelColor.a <= 1.0 ? (int) Math.floor(pixelColor.a * 255) : 255;
                    int c_red = pixelColor.r <= 1.0 ? (int) Math.floor(pixelColor.r * 255) : 255;
                    int c_green = pixelColor.g <= 1.0 ? (int) Math.floor(pixelColor.g * 255) : 255;
                    int c_blue = pixelColor.b <= 1.0 ? (int) Math.floor(pixelColor.b * 255) : 255;
                    int finalPixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                    image.setRGB(i, j, finalPixelColor);
                } // end start < end
                else {
                    image.setRGB(i, j, 1 << 24);
                }
            } // end imageWidth
        } // end imageWidth
    } // end compositing
    
    /*
    
    2D transfer function rendering
    
    */
    
    void TwoDTransfer(double[] viewMatrix) {
        
        // clear image        
        for (int j = 0; j < imageHeight; j++) {
            for (int i = 0; i < imageWidth; i++) {
                image.setRGB(i, j, 0);
            }
        }
        
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);
        
        // image is square
        int imageCenter = imageWidth >> 1;

        double[] voxelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX >> 1, volumeDimY >> 1, volumeDimZ >> 1);

        // sample on a plane through the origin of the volume data
        TFColor voxelColor = new TFColor();
        short voxelIntensity = 0;
        VoxelGradient voxelGradient = new VoxelGradient();
        
        // interactive mode
        int step = 1;
        if(interactiveMode == true) {
            step = 3;
        }
        
        // get base intensity, radius, and base color from TransferFunction2D
        short baseIntensity = tfEditor2D.triangleWidget.baseIntensity;
        double radius = tfEditor2D.triangleWidget.radius;
        TFColor baseColor = tfEditor2D.triangleWidget.color;
        
        double XStep = viewVec[0] * step;
        double YStep = viewVec[1] * step;
        double ZStep = viewVec[2] * step;
        
        for (int j = 0; j < imageHeight; j ++) {
            
            double voxelCoordXStart = uVec[0] * (-1 - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
            double voxelCoordYStart = uVec[1] * (-1 - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
            double voxelCoordZStart = uVec[2] * (-1 - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
            
            for (int i = 0; i < imageWidth; i++) {
                
                TFColor pixelColor = new TFColor(0, 0, 0, 1);
                
                voxelCoordXStart += uVec[0];
                voxelCoordYStart += uVec[1];
                voxelCoordZStart += uVec[2];
                                
                // compute intersections
                long tXMin = Long.MIN_VALUE;
                long tXMax = Long.MAX_VALUE;
                long tYMin = Long.MIN_VALUE;
                long tYMax = Long.MAX_VALUE;
                long tZMin = Long.MIN_VALUE;
                long tZMax = Long.MAX_VALUE;
                
                if(viewVec[0] != 0) {
                    tXMin = Math.round(-voxelCoordXStart / viewVec[0]);
                    tXMax = Math.round((volumeDimX - voxelCoordXStart) / viewVec[0]);;
                }
                
                if(viewVec[1] != 0) {
                    tYMin = Math.round(-voxelCoordYStart / viewVec[1]);
                    tYMax = Math.round((volumeDimY - voxelCoordYStart) / viewVec[1]);
                }
                
                if(viewVec[2] != 0) {
                    tZMin = Math.round(-voxelCoordZStart / viewVec[2]);
                    tZMax = Math.round((volumeDimZ - voxelCoordZStart) / viewVec[2]);
                }
                
                //swap value if min larger than max
                if(tXMin > tXMax) {
                    tXMin = tXMin + tXMax;
                    tXMax = tXMin - tXMax;
                    tXMin = tXMin - tXMax;          
                }
                //swap value if min larger than max
                if(tYMin > tYMax) {
                    tYMin = tYMin + tYMax;
                    tYMax = tYMin - tYMax;
                    tYMin = tYMin - tYMax;          
                }
                //swap value if min larger than max
                if(tZMin > tZMax) {
                    tZMin = tZMin + tZMax;
                    tZMax = tZMin - tZMax;
                    tZMin = tZMin - tZMax;          
                }
                
                long start = Math.max(tXMin, Math.max(tYMin, tZMin));
                long end = Math.min(tXMax, Math.min(tYMax, tZMax));
                
                // ray intersects with volume
                if(start < end) {
                    voxelCoord[0] = voxelCoordXStart + (start - step) * viewVec[0];
                    voxelCoord[1] = voxelCoordYStart + (start - step) * viewVec[1];
                    voxelCoord[2] = voxelCoordZStart + (start - step) * viewVec[2];
                    
                    for(long u = start; u < end; u += step) {
                        voxelCoord[0] += XStep;
                        voxelCoord[1] += YStep;
                        voxelCoord[2] += ZStep;

                        // get voxel intensity
                        voxelIntensity = getVoxel(voxelCoord);
                        
                        // gradientMag = (float)getVoxelGradientMag(voxelCoord);
                        voxelGradient = getVoxelGradient(voxelCoord);
                        
                        voxelColor.r = baseColor.r;
                        voxelColor.g = baseColor.g;
                        voxelColor.b = baseColor.b;
                        // voxelColor = tFunc.getColor(getVoxel(pixelCoord));

                        // reweight the opacity
                        double absDiffGradRatio = Math.abs(voxelIntensity - baseIntensity) / (voxelGradient.mag + 1e-6);

                        if(voxelGradient.mag <= 1e-6 && voxelIntensity == baseIntensity) {
                            voxelColor.a = baseColor.a;
                        }
                        else if(voxelGradient.mag > 1e-6 && absDiffGradRatio <= radius) {
                            voxelColor.a = baseColor.a * (1.0 - 1.0 / radius * absDiffGradRatio);
                        }
                        else{
                            voxelColor.a = 0;
                        }
                        
                        // shading
                        if(shading) {
                            // surface normal at voxel
                            double [] normalizedNormVec = {voxelGradient.x, voxelGradient.y, voxelGradient.z};
                            double normalizedNVecLength = VectorMath.length(normalizedNormVec);
                            normalizedNormVec[0] /= (normalizedNVecLength + 1e-6);
                            normalizedNormVec[1] /= (normalizedNVecLength + 1e-6);
                            normalizedNormVec[2] /= (normalizedNVecLength + 1e-6);
                            double dotProductNL = Math.max(-VectorMath.dotproduct(viewVec, normalizedNormVec), 0);
                            double dotProductNH = Math.max(-VectorMath.dotproduct(viewVec, normalizedNormVec), 0);
                            double colorIncrement = SHADING_SPEC_COEFF * Math.pow(dotProductNH, SHADING_ALPHA);
                            voxelColor.r = SHADING_AMBIENT_COEFF.r + voxelColor.r * (SHADING_DIFF_COEFF * dotProductNL + colorIncrement);
                            voxelColor.g = SHADING_AMBIENT_COEFF.g + voxelColor.g * (SHADING_DIFF_COEFF * dotProductNL + colorIncrement);
                            voxelColor.b = SHADING_AMBIENT_COEFF.b + voxelColor.b * (SHADING_DIFF_COEFF * dotProductNL + colorIncrement);
                            
                        }

                        // composite voxel colors
                        pixelColor.r = (1 - voxelColor.a) * pixelColor.r + voxelColor.a * voxelColor.r;
                        pixelColor.g = (1 - voxelColor.a) * pixelColor.g + voxelColor.a * voxelColor.g;
                        pixelColor.b = (1 - voxelColor.a) * pixelColor.b + voxelColor.a * voxelColor.b;
                        pixelColor.a = (1 - voxelColor.a) * pixelColor.a;
                    }
                    
                    pixelColor.a = 1 - pixelColor.a;
                                        
                    // BufferedImage expects a pixel color packed as ARGB in an int
                    int c_alpha = pixelColor.a <= 1.0 ? (int) Math.floor(pixelColor.a * 255) : 255;
                    int c_red = pixelColor.r <= 1.0 ? (int) Math.floor(pixelColor.r * 255) : 255;
                    int c_green = pixelColor.g <= 1.0 ? (int) Math.floor(pixelColor.g * 255) : 255;
                    int c_blue = pixelColor.b <= 1.0 ? (int) Math.floor(pixelColor.b * 255) : 255;
                    int finalPixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                    image.setRGB(i, j, finalPixelColor);
                }
                else {
                    // background
                    image.setRGB(i, j, 1 << 24);
                }
            } //end imageWidth
        } // end imageHeight
    } // end TwoDTransfer
    
    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glVertex3d(-volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, volumeDimZ / 2.0);
        gl.glVertex3d(volumeDimX / 2.0, -volumeDimY / 2.0, -volumeDimZ / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }

    @Override
    public void visualize(GL2 gl) {


        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();
        
        if("Slicer".equals(Rendering_Method)){
            slicer(viewMatrix);
        }
        else if("MIP".equals(Rendering_Method)){
            mip(viewMatrix);
        }
        else if("Compositing".equals(Rendering_Method)) {
            compositing(viewMatrix);
        }
        else if("Transfer2D".equals(Rendering_Method)) {
            TwoDTransfer(viewMatrix);
        }
        
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();


        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private double[] viewMatrix = new double[4 * 4];

    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
}

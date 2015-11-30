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
    private GradientVolume gradients = null;
    public String Method_Implemented = "Slicer";
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
     

    short getVoxel(double[] coord) {

        
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);
        
        if (coord[0] < 0 || coord[0] >= volume.getDimX() - 1 || coord[1] < 0 || coord[1] >= volume.getDimY() - 1
                || coord[2] < 0 || coord[2] >= volume.getDimZ() - 1) {
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
        
        double val01 = alpha * val1 + (1 - alpha) * val0;
        double val23 = alpha * val3 + (1 - alpha) * val2;
        double val45 = alpha * val5 + (1 - alpha) * val4;
        double val67 = alpha * val7 + (1 - alpha) * val6;
        
        double val0123 = beta * val23 + (1 - beta) * val01;
        double val4567 = beta * val67 + (1 - beta) * val45;
        
        double finalVal = gamma * val4567 + (1 - gamma) * val0123;
        
        return (short)Math.round(finalVal);
    }
    
//    final private double linearInterpolate(double val0, double val1, double alpha) {
//        
//        return alpha * val1 + (1 - alpha) * val0;
//    }


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
        int imageCenter = image.getWidth() / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();
        //System.out.println(imageCenter);
        
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                        + volumeCenter[0];
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                        + volumeCenter[1];
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                        + volumeCenter[2];

                int val = getVoxel(pixelCoord);
                
                // Map the intensity to a grey value by linear scaling
                voxelColor.r = val/max;
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

    void mip(double[] viewMatrix) {

        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        
        int volumeDimX = volume.getDimX();
        int volumeDimY = volume.getDimY();
        int volumeDimZ = volume.getDimZ();


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
        int imageCenter = imageWidth / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX / 2, volumeDimY / 2, volumeDimZ / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();

        double len = Math.sqrt(Math.pow(viewVec[0] * volumeDimX,2)+Math.pow(viewVec[1] * volumeDimY,2)+Math.pow(viewVec[2] * volumeDimZ,2));
        long range = Math.round(len) >> 1;
        
        int step = 1;
        if(interactiveMode == true) {
            step = 3;
        }
        
        for (int j = 0; j < imageHeight; j++) {
            
            double pixelCoordXStart = uVec[0] * (-1 - imageCenter) + vVec[0] * (j - imageCenter);
            double pixelCoordYStart = uVec[1] * (-1 - imageCenter) + vVec[1] * (j - imageCenter);
            double pixelCoordZStart = uVec[2] * (-1 - imageCenter) + vVec[2] * (j - imageCenter);
            
            for (int i = 0; i < imageWidth; i++) {
                
                int val = 0;
                
                pixelCoordXStart += uVec[0];
                pixelCoordYStart += uVec[1];
                pixelCoordZStart += uVec[2];
                
                pixelCoord[0] = pixelCoordXStart - (range + 1) * viewVec[0] + volumeCenter[0];
                pixelCoord[1] = pixelCoordYStart - (range + 1) * viewVec[1] + volumeCenter[1];
                pixelCoord[2] = pixelCoordZStart - (range + 1) * viewVec[2] + volumeCenter[2];
                
                for(long u = - range; u < range; u+=step){
                    pixelCoord[0] += viewVec[0] * step;
                    pixelCoord[1] += viewVec[1] * step;
                    pixelCoord[2] += viewVec[2] * step;
                    
                    int val2 = getVoxel(pixelCoord);
                    if(val2 > val){
                        val = val2;
                    }
                    
                }
                
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

    
    void compositing(double[] viewMatrix, boolean interactiveMode) {
        
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        
        int volumeDimX = volume.getDimX();
        int volumeDimY = volume.getDimY();
        int volumeDimZ = volume.getDimZ();

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
        int imageCenter = imageWidth / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volumeDimX / 2, volumeDimY / 2, volumeDimZ / 2);

        // sample on a plane through the origin of the volume data
        TFColor voxelColor = new TFColor(0, 0, 0, 1);
        
        double len = Math.sqrt(Math.pow(viewVec[0] * volumeDimX,2)+Math.pow(viewVec[1] * volumeDimY,2)+Math.pow(viewVec[2] * volumeDimZ,2));
        
        long range = Math.round(len) >> 1;
        
        int step = 1;
        if(interactiveMode == true) {
            step = 3;
        }
        
        for (int j = 0; j < imageHeight; j ++) {
            
            double pixelCoordXStart = uVec[0] * (-1 - imageCenter) + vVec[0] * (j - imageCenter);
            double pixelCoordYStart = uVec[1] * (-1 - imageCenter) + vVec[1] * (j - imageCenter);
            double pixelCoordZStart = uVec[2] * (-1 - imageCenter) + vVec[2] * (j - imageCenter);
            
            for (int i = 0; i < imageWidth; i++) {
                
                pixelCoordXStart += uVec[0];
                pixelCoordYStart += uVec[1];
                pixelCoordZStart += uVec[2];
                                
                pixelCoord[0] = pixelCoordXStart - (range + 1) * viewVec[0] + volumeCenter[0];
                pixelCoord[1] = pixelCoordYStart - (range + 1) * viewVec[1] + volumeCenter[1];
                pixelCoord[2] = pixelCoordZStart - (range + 1) * viewVec[2] + volumeCenter[2];
                
                TFColor pixelColor = new TFColor();
                
                
                for(long u = - range; u < range; u += step){
                    
                    pixelCoord[0] += step * viewVec[0];
                    pixelCoord[1] += step * viewVec[1];
                    pixelCoord[2] += step * viewVec[2];
                    
                    // Map the intensity to a color value by linear scaling
                    voxelColor = tFunc.getColor(getVoxel(pixelCoord));
                    
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
        }

    }
    
    private void intersectionRayCastWithVolume(double [] viewMatrix) {
        
        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);
        
        int i = 5;
        int j = 6;
        double []P0 = new double [3];
        P0[0] = i * uVec[0] + j * vVec[0];
        P0[1] = i * uVec[1] + j * vVec[1];
        P0[2] = i * uVec[2] + j * vVec[2];
        
        // intersect with six faces
        // x = 0
        double lambda = (0 - P0[0]) / viewVec[0];
        // y = 0
        lambda = (0 - P0[1]) / viewVec[1];
        // z = 0
        lambda = (0 - P0[2]) / viewVec[2];
        // x = dimX - 1
        lambda = (volume.getDimX() - 1 - P0[0]) / viewVec[0];
        // y = dimY - 1
        lambda = (volume.getDimY() - 1 - P0[1]) / viewVec[1];
        // z = dimZ - 1
        lambda = (volume.getDimZ() - 1 - P0[2]) / viewVec[2];
        
        
        
        
        
    }
    
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
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
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
        
        if("Slicer".equals(Method_Implemented)){
            slicer(viewMatrix);
        }
        else if("MIP".equals(Method_Implemented)){
            mip(viewMatrix);
        }
        else if("Compositing".equals(Method_Implemented)) {
            compositing(viewMatrix, interactiveMode);
        }
        else if("Transfer2D".equals(Method_Implemented)) {
            
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
    private double[] viewMatrix = new double[4 * 4];

    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
}

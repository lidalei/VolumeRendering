/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

/**
 *
 * @author michel
 */
public class VoxelGradient {

    public float x, y, z;
    public float mag;
    
    public VoxelGradient() {
        x = y = z = mag = 0.0f;
    }
    
    public VoxelGradient(float gx, float gy, float gz) {
        x = gx;
        y = gy;
        z = gz;
        mag = (float) Math.sqrt(x*x + y*y + z*z);
    }
    
    // scalar multiplication a gradient
    public static VoxelGradient scalarMultiplication(float lambda, VoxelGradient g) {
        VoxelGradient g2 = new VoxelGradient();
        g2.x = lambda * g.x;
        g2.y = lambda * g.y;
        g2.z = lambda * g.z;
        g2.mag = lambda * g.mag;
        return g2;
        
    }
    
    
    // add two voxel graident
    public static VoxelGradient voxelGradientAddition(VoxelGradient g1, VoxelGradient g2) {
        VoxelGradient g3 = new VoxelGradient();
        g3.x = g1.x + g2.x;
        g3.y = g1.y + g2.y;
        g3.z = g1.z + g2.z;
        g3.mag = (float)Math.sqrt(g1.mag * g1.mag + g2.mag * g2.mag + 2 * (g1.x * g2.x + g1.y * g2.y + g1.z * g2.z));
        return g3;
    }
    
}

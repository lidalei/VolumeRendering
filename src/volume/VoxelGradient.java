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
        return new VoxelGradient(lambda * g.x, lambda * g.y, lambda * g.z);
    }
    
    
    // add two voxel graident
    public static VoxelGradient voxelGradientAddition(VoxelGradient g1, VoxelGradient g2) {
        return new VoxelGradient(g1.x + g2.x, g1.y + g2.y, g1.z + g2.z);
    }
    
}

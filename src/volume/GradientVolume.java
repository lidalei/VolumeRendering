/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Nov. 23, 2015
 * @author Dalei Li
 * non-normalized gradient vector
 */
package volume;

/**
 *
 * @author michel
 */
public class GradientVolume {

    public GradientVolume(Volume vol) {
        volume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        data = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

    public VoxelGradient getGradient(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }

    
    public void setGradient(int x, int y, int z, VoxelGradient value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, VoxelGradient value) {
        data[i] = value;
    }

    public VoxelGradient getVoxel(int i) {
        return data[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }

    private void compute() {
        
        // non-normalized gradient vector
        
        for (int x = 0; x < dimX; ++x) {
            
            for (int y = 0; y < dimY; ++y) {
                
                for (int z = 0; z < dimZ; ++z) {
                    
                    float gx = 0;
                    float gy = 0;
                    float gz = 0;

                    boolean xISBounded = false;
                    boolean yISBounded = false;
                    boolean zISBounded = false;

                    if (x == dimX - 1) {
                        gx = volume.getVoxel(x, y, z) - volume.getVoxel(x - 1, y, z);
                        xISBounded = true;
                    }

                    if (y == dimY - 1) {
                        gy = volume.getVoxel(x, y, z) - volume.getVoxel(x, y - 1, z);
                        yISBounded = true;
                    }

                    if (z == dimZ - 1) {
                        gz = volume.getVoxel(x, y, z) - volume.getVoxel(x, y, z - 1);
                        zISBounded = true;
                    }

                    if (x == 0) {
                        gx = volume.getVoxel(x + 1, y, z) - volume.getVoxel(x, y, z);
                        xISBounded = true;
                    }

                    if (y == 0) {
                        gy = volume.getVoxel(x, y + 1, z) - volume.getVoxel(x, y, z);
                        yISBounded = true;
                    }

                    if (z == 0) {
                        gz = volume.getVoxel(x, y, z + 1) - volume.getVoxel(x, y, z);
                        zISBounded = true;
                    }

                    if (xISBounded == false) {
                        gx = (volume.getVoxel(x + 1, y, z) - volume.getVoxel(x - 1, y, z)) >> 1;
                    }

                    if (yISBounded == false) {
                        gy = (volume.getVoxel(x, y + 1, z) - volume.getVoxel(x, y - 1, z)) >> 1;
                    }

                    if (zISBounded == false) {
                        gz = (volume.getVoxel(x, y, z + 1) - volume.getVoxel(x, y, z - 1)) >> 1;
                    }

                    data[x + dimX*(y + dimY * z)] = new VoxelGradient(gx, gy, gz);
                }
                
                
            }
            
            
            
        }
        
        
        
        
        // for (int i = 0; i < data.length; i++) {            
            
            // this just initializes all gradients to the vector (0,0,0)
            // data[i] = zero;
        // }
                
    }
    
    public double getMaxGradientMagnitude() {
        if (maxmag >= 0) {
            return maxmag;
        } else {
            double magnitude = data[0].mag;
            for (int i=0; i<data.length; i++) {
                magnitude = data[i].mag > magnitude ? data[i].mag : magnitude;
            }   
            maxmag = magnitude;
            return magnitude;
        }
    }
    
    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] data;
    Volume volume;
    double maxmag;
}

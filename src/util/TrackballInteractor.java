/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author michel
 */
public class TrackballInteractor {

    private int lmx = 0, lmy = 0;	//remembers last mouse location
    private double[] trackballXform = new double[16];
    private double[] lastPos = new double[3];
    private double[] axis = new double[3];
    private double angle;
    private int width, height;
    private boolean rotating = false;

    public TrackballInteractor(int width, int height) {
        this.width = width;
        this.height = height;
        trackballXform[0] = 1.0;
        trackballXform[5] = 1.0;
        trackballXform[10] = 1.0;
        trackballXform[15] = 1.0;
    }

    public void setDimensions(int w, int h) {
        width = w;
        height = h;
    }

    public double[] getTransformationMatrix() {
        return trackballXform;
    }

    public void setMousePos(int x, int y) {
        lmx = x;
        lmy = y;
        trackball_ptov(lmx, lmy, width, height, lastPos);
    }

    public boolean isRotating() {
        return rotating;
    }

    public void setRotating(boolean flag) {
        rotating = flag;
    }

    private void trackball_ptov(int x, int y, int width, int height, double v[]) {
        double d, a;

        // project x,y onto a hemi-sphere centered within width, height
        double radius = Math.min(width, height) - 20;
        v[0] = (2.0 * x - width) / radius;
        v[1] = (height - 2.0 * y) / radius;

        d = Math.sqrt(v[0] * v[0] + v[1] * v[1]);
        v[2] = Math.cos((Math.PI / 2.0) * ((d < 1.0) ? d : 1.0));
        a = 1.0 / Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] *= a;
        v[1] *= a;
        v[2] *= a;

    }

    public void drag(int mx, int my) {

        double[] curPos = new double[3];

        trackball_ptov(mx, my, width, height, curPos);

        double dx = curPos[0] - lastPos[0];
        double dy = curPos[1] - lastPos[1];
        double dz = curPos[2] - lastPos[2];

        if ((dx != 0) || (dy != 0) || (dz != 0)) {
            angle = 90.0 * Math.sqrt(dx * dx + dy * dy + dz * dz);
            axis[0] = lastPos[1] * curPos[2] - lastPos[2] * curPos[1];
            axis[1] = lastPos[2] * curPos[0] - lastPos[0] * curPos[2];
            axis[2] = lastPos[0] * curPos[1] - lastPos[1] * curPos[0];

            lastPos[0] = curPos[0];
            lastPos[1] = curPos[1];
            lastPos[2] = curPos[2];
        }
    }

    public void updateTransform(GL2 gl) {
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glRotated(angle, axis[0], axis[1], axis[2]);
        gl.glMultMatrixd(trackballXform, 0);
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, trackballXform, 0);
        gl.glPopMatrix();
        setRotating(false);
    }
    
}

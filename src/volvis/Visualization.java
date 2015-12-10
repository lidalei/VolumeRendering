/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import util.TFChangeListener;
import util.TrackballInteractor;

/**
 *
 * @author michel
 */
public class Visualization implements GLEventListener, TFChangeListener {

  GLU glu = new GLU();
    ArrayList<Renderer> renderers;
    GLAutoDrawable canvas;
    int winWidth, winHeight;
    double fov = 20.0;
    TrackballInteractor trackball;
        
    public Visualization(GLAutoDrawable canvas) {
        this.renderers = new ArrayList<Renderer>();
        this.canvas = canvas;
        
        if (canvas instanceof Component) {
            Component comp = (Component) canvas;
            comp.addMouseMotionListener(new MouseMotionListener()); // listens to drag events
            comp.addMouseListener(new MousePressListener());
            comp.addMouseWheelListener(new MouseWheelHandler());
        }
        
        trackball = new TrackballInteractor(winWidth, winHeight);
    }

    // Add a new renderer (i.e. visualization method) to the visualization
    public void addRenderer(Renderer vis) {
        renderers.add(vis);
    }

    public void update() {
        canvas.display();
    }
    
  @Override
    public void changed() {
        canvas.display();
    }



    


   @Override
    public void init(GLAutoDrawable drawable) {
       
    }

   @Override
    public void display(GLAutoDrawable drawable) {

        // get the OpenGL rendering context
        GL2 gl = drawable.getGL().getGL2();

        // set up the projection transform
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(fov, ((float) winWidth/((float) winHeight)), 0.1, 5000);
        gl.glTranslated(0, 0, -1000);

        
        // clear screen and set the view transform to the identity matrix
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);

       if (trackball.isRotating()) {
           // when dragging the mouse, keep updating the virtual trackball
           trackball.updateTransform(gl);
       }
       
       // multiply the current view transform (identity) with trackball transform
       gl.glMultMatrixd(trackball.getTransformationMatrix(), 0);
       
       
        // call the visualize() methods of all subscribed renderers
        for (int i = 0; i < renderers.size(); i++) {
            renderers.get(i).visualize(gl);
            // blocking call ensures drawing of renderer is completed before
            // next one starts
            gl.glFlush();
        }
    }

    // reshape handles window resize
   @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
        for (int i = 0; i < renderers.size(); i++) {
            renderers.get(i).setWinWidth(width);
            renderers.get(i).setWinHeight(height);
        }
        winWidth = width;
        winHeight = height;
        trackball.setDimensions(width, height);
    }


    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }

   
   class MousePressListener extends MouseAdapter {

       @Override
       public void mousePressed(MouseEvent e) {
           trackball.setMousePos(e.getX(), e.getY());
           
           for (int i = 0; i < renderers.size(); i++) {
               renderers.get(i).setInteractiveMode(true);
           }
       }
       
       @Override
       public void mouseReleased(MouseEvent e) {
           for (int i = 0; i < renderers.size(); i++) {
               renderers.get(i).setInteractiveMode(false);
           }
           update();
       }
   }
   
    class MouseMotionListener extends MouseMotionAdapter {
        
        
      @Override
        public void mouseDragged(MouseEvent e) {
             trackball.drag(e.getX(), e.getY());
             trackball.setRotating(true);
             update();
          }
          
        }
    
    
    class MouseWheelHandler implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0) { // up
                fov--;
                if (fov < 2) {
                    fov = 2;
                }
            } else { // down
                fov++;
            }
            update();
        }
        
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import volvis.TFColor;
import volvis.TransferFunction;

/**
 *
 * @author michel
 */
public class TransferFunctionView extends javax.swing.JPanel {

    private TransferFunction tfunc;
    private final int DOTSIZE = 8;
    private int selected;
    private Point dragStart;
    private TransferFunctionEditor editor;
    private int[] histogram;

    /**
     * Creates new form TransferFunctionView
     */
    public TransferFunctionView(TransferFunction tfunc, int[] histogram, TransferFunctionEditor ed) {
        initComponents();
        this.tfunc = tfunc;
        this.editor = ed;
        this.histogram = histogram;
        addMouseMotionListener(new ControlPointHandler());
        addMouseListener(new SelectionHandler());
    }

    @Override
    public void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        int w = this.getWidth();
        int h = this.getHeight() - 30;
        int range = tfunc.getMaximum() - tfunc.getMinimum();
        int min = tfunc.getMinimum();


        g2.setColor(Color.white);
        g2.fillRect(0, 0, w, h);
        
        //draw histogram
        int nrBins = histogram.length;
        int maxBinHeigth = 0;
        for (int i = 0; i < nrBins; i++) {
            maxBinHeigth = histogram[i] > maxBinHeigth ? histogram[i] : maxBinHeigth;
        }
        double binWidth = (double) w / (double) nrBins;
        g2.setColor(Color.lightGray);
        double scalingFactor = (double) h / (double) maxBinHeigth;
        for (int i = 0; i < nrBins; i++) {
            g2.fill(new Rectangle2D.Double(i*binWidth, h-scalingFactor*histogram[i], binWidth, scalingFactor*histogram[i]));
        }
        

        ArrayList<TransferFunction.ControlPoint> controlPoints = tfunc.getControlPoints();
        int xprev = -1;
        int yprev = -1;
        for (int i = 0; i < controlPoints.size(); i++) {
            TransferFunction.ControlPoint cp = controlPoints.get(i);
            int s = cp.value;
            //System.out.println("s = " + s);
            TFColor color = cp.color;
            double t = (double) (s - min) / (double) range;
            //System.out.println("t = " + t);
            int xpos = (int) (t * w);
            int ypos = h - (int) (color.a * h);
            //System.out.println("x = " + xpos + "; y = " + ypos);
            g2.setColor(Color.black);
            g2.fillOval(xpos - DOTSIZE / 2, ypos - DOTSIZE / 2, DOTSIZE, DOTSIZE);
            if (xprev > -1) {
                g2.drawLine(xpos, ypos, xprev, yprev);
            }
            xprev = xpos;
            yprev = ypos;
        }


        for (int i = 0; i < w; i++) {
            double t = (1.0 * i) / (w - 1);
            int s = (int) (t * range + min);
            TFColor c = tfunc.getColor(s);
            g2.setColor(new Color((float) c.r, (float) c.g, (float) c.b));
            g2.fillRect(i, h + 5, 1, h + 30);
        }
    }

    private Ellipse2D getControlPointArea(TransferFunction.ControlPoint cp) {
        int w = this.getWidth();
        int h = this.getHeight() - 30;
        int range = tfunc.getMaximum() - tfunc.getMinimum();
        int min = tfunc.getMinimum();

        int s = cp.value;
        TFColor color = cp.color;
        double t = (double) (s - min) / (double) range;
        int xpos = (int) (t * w);
        int ypos = h - (int) (color.a * h);
        Ellipse2D bounds = new Ellipse2D.Double(xpos - DOTSIZE / 2, ypos - DOTSIZE / 2, DOTSIZE, DOTSIZE);
        return bounds;
    }

    private class ControlPointHandler extends MouseMotionAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            ArrayList<TransferFunction.ControlPoint> controlPoints = tfunc.getControlPoints();
            boolean inside = false;
            for (int i = 0; i < controlPoints.size(); i++) {
                inside = inside || getControlPointArea(controlPoints.get(i)).contains(e.getPoint());
            }
            if (inside) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selected < 0) {
                return;
            }

            Point dragEnd = e.getPoint();

            ArrayList<TransferFunction.ControlPoint> controlPoints = tfunc.getControlPoints();

            if (selected == 0 || selected == controlPoints.size() - 1) {
                // constrain first and last point to vertical motion
                Ellipse2D cp = getControlPointArea(controlPoints.get(selected));
                dragEnd.setLocation(cp.getCenterX(), dragEnd.y);
                if (dragEnd.y < 0) {
                    dragEnd.y = 0;
                }
                if (dragEnd.y > getHeight() - 30) {
                    dragEnd.y = getHeight() - 30;
                }
            } else {
                Ellipse2D leftPoint = getControlPointArea(controlPoints.get(selected - 1));
                Ellipse2D rightPoint = getControlPointArea(controlPoints.get(selected + 1));

                if (dragEnd.getX() <= leftPoint.getCenterX() + 1) {
                    dragEnd.setLocation(leftPoint.getCenterX() + 2, dragEnd.y);
                }
                if (dragEnd.getX() >= rightPoint.getCenterX() - 1) {
                    dragEnd.setLocation(rightPoint.getCenterX() - 2, dragEnd.y);
                }
                if (dragEnd.y < 0) {
                    dragEnd.y = 0;
                }
                if (dragEnd.y > getHeight() - 30) {
                    dragEnd.y = getHeight() - 30;
                }

            }

            double w = getWidth();
            double h = getHeight() - 30;
            int range = tfunc.getMaximum() - tfunc.getMinimum();
            int min = tfunc.getMinimum();
            double t = dragEnd.x / w;
            int s = (int) ((t * range) + min);
            //System.out.println("s = " + s);
            double a = (h - dragEnd.y) / h;
            //System.out.println("a = " + a);

            tfunc.updateControlPointScalar(selected, s);
            tfunc.updateControlPointAlpha(selected, a);
            editor.setSelectedInfo(selected, s, a, controlPoints.get(selected).color);
            repaint();

        }
    }


    private class SelectionHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            ArrayList<TransferFunction.ControlPoint> controlPoints = tfunc.getControlPoints();
            boolean inside = false;
            int idx = 0;
            while (!inside && idx < controlPoints.size()) {
                inside = inside || getControlPointArea(controlPoints.get(idx)).contains(e.getPoint());
                if (inside) {
                    break;
                } else {
                    idx++;
                }
            }
            if (inside) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    selected = idx;
                    //System.out.println("selected: " + controlPoints.get(selected).toString());
                    TransferFunction.ControlPoint cp = controlPoints.get(selected);
                    editor.setSelectedInfo(selected, cp.value, cp.color.a, cp.color);
                    dragStart = e.getPoint();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    if (idx > 0 && idx < controlPoints.size() - 1) {
                        tfunc.removeControlPoint(idx);
                        selected = idx - 1;
                        TransferFunction.ControlPoint cp = controlPoints.get(selected);
                        editor.setSelectedInfo(selected, cp.value, cp.color.a, cp.color);
                        dragStart = e.getPoint();
                    }
                }
            } else {
                Point pos = e.getPoint();
                if (pos.x >= 0 && pos.x < getWidth() && pos.y >= 0 && pos.y < (getHeight() - 30)) {
                    double w = getWidth();
                    double h = getHeight() - 30;
                    int range = tfunc.getMaximum() - tfunc.getMinimum();
                    int min = tfunc.getMinimum();
                    double t = pos.x / w;
                    int s = (int) ((t * range) + min);
                    //System.out.println("s = " + s);
                    double a = (h - pos.y) / h;
                    //System.out.println("a = " + a);
                    selected = tfunc.addControlPoint(s, 0.0, 0.0, 0.0, a);
                    TransferFunction.ControlPoint cp = controlPoints.get(selected);
                    editor.setSelectedInfo(selected, cp.value, cp.color.a, cp.color);
                    dragStart = e.getPoint();
                    repaint();
                }
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            repaint();
            tfunc.changed();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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

/**
 *
 * @author michel
 */
public class TransferFunction2DView extends javax.swing.JPanel {

    TransferFunction2DEditor ed;
    private final int DOTSIZE = 6;
    public Ellipse2D.Double baseControlPoint, radiusControlPoint;    
    boolean selectedBaseControlPoint, selectedRadiusControlPoint;
    
    // extend widget
    public Ellipse2D.Double lowControlPoint, upControlPoint;
    boolean selectedLowControlPoint, selectedUpControlPoint;
    
    /**
     * Creates new form TransferFunction2DView
     * @param ed
     */
    public TransferFunction2DView(TransferFunction2DEditor ed) {
        initComponents();
        
        this.ed = ed;
        selectedBaseControlPoint = false;
        selectedRadiusControlPoint = false;
        
        // extend widget
        selectedLowControlPoint = false;
        selectedUpControlPoint = false;
        
        addMouseMotionListener(new TriangleWidgetHandler());
        addMouseListener(new SelectionHandler());
    }
    
    @Override
    public void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        int w = this.getWidth();
        int h = this.getHeight();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, w, h);
        
        double maxHistoMagnitude = ed.histogram[0];
        for (int i = 0; i < ed.histogram.length; i++) {
            if(ed.histogram[i] > maxHistoMagnitude) {
                maxHistoMagnitude = ed.histogram[i];
            }
        }
        
        double binWidth = (double) w / (double) ed.xbins;
        double binHeight = (double) h / (double) ed.ybins;
        maxHistoMagnitude = Math.log(maxHistoMagnitude);
        
        for (int y = 0; y < ed.ybins; y++) {
            for (int x = 0; x < ed.xbins; x++) {
                if (ed.histogram[y * ed.xbins + x] > 0) {
                    int intensity = (int) Math.floor(255 * (1.0 - Math.log(ed.histogram[y * ed.xbins + x]) / maxHistoMagnitude));
                    g2.setColor(new Color(intensity, intensity, intensity));
                    g2.fill(new Rectangle2D.Double(x * binWidth, h - (y * binHeight), binWidth, binHeight));
                }
            }
        }
        
        // draw triangle widget
        int ypos = h;
        int xpos = (int) (ed.triangleWidget.baseIntensity * binWidth);
        g2.setColor(Color.black);
        baseControlPoint = new Ellipse2D.Double(xpos - DOTSIZE / 2, ypos - DOTSIZE, DOTSIZE, DOTSIZE);
        g2.fill(baseControlPoint);
        g2.drawLine(xpos, ypos, xpos - (int) (ed.triangleWidget.radius * binWidth * ed.maxGradientMagnitude), 0);
        g2.drawLine(xpos, ypos, xpos + (int) (ed.triangleWidget.radius * binWidth * ed.maxGradientMagnitude), 0);
        radiusControlPoint = new Ellipse2D.Double(xpos + (ed.triangleWidget.radius * binWidth * ed.maxGradientMagnitude) - DOTSIZE / 2,  0, DOTSIZE, DOTSIZE);
        g2.fill(radiusControlPoint);
        
        // extend widget
        int lowYPos = h - (int) (ed.triangleWidget.lowGradientMagnitude / ed.maxGradientMagnitude * ed.ybins * binHeight);
        int upYPos = h - (int) (ed.triangleWidget.upGradientMagnitude / ed.maxGradientMagnitude * ed.ybins * binHeight);
        g2.setColor(Color.green);
        lowControlPoint = new Ellipse2D.Double(lowYPos >= h - DOTSIZE ? xpos - 10 - DOTSIZE / 2 : xpos - DOTSIZE / 2, lowYPos - DOTSIZE / 2, DOTSIZE, DOTSIZE);
        upControlPoint = new Ellipse2D.Double(xpos - DOTSIZE / 2, upYPos - DOTSIZE / 2, DOTSIZE, DOTSIZE);
        g2.fill(lowControlPoint);
        g2.fill(upControlPoint);
        // lowControlPoint
        int lowLeftXPos = xpos - (int) (ed.triangleWidget.radius * binWidth * ed.triangleWidget.lowGradientMagnitude);
        int lowRightXPos = xpos + (int) (ed.triangleWidget.radius * binWidth * ed.triangleWidget.lowGradientMagnitude);
        g2.drawLine(lowLeftXPos, lowYPos, lowRightXPos, lowYPos);
        // upControlPoint
        int upLeftXPos = xpos - (int) (ed.triangleWidget.radius * binWidth * ed.triangleWidget.upGradientMagnitude);
        int upRightXPos = xpos + (int) (ed.triangleWidget.radius * binWidth * ed.triangleWidget.upGradientMagnitude);
        g2.drawLine(upLeftXPos, upYPos, upRightXPos, upYPos);
    }
    
    
    private class TriangleWidgetHandler extends MouseMotionAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {
            // extend widget
            if (baseControlPoint.contains(e.getPoint()) || radiusControlPoint.contains(e.getPoint()) || lowControlPoint.contains(e.getPoint()) || upControlPoint.contains(e.getPoint())) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
            
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            
            double w = getWidth();
            double h = getHeight();
            double binWidth = (double) w / (double) ed.xbins;

            // extend widget
            double binHeight = (double) h / ed.ybins;
                
            // extend widget
            
            if (selectedBaseControlPoint || selectedRadiusControlPoint || selectedLowControlPoint || selectedUpControlPoint) {
                Point dragEnd = e.getPoint();
                
                if (selectedBaseControlPoint) {
                    // restrain to horizontal movement
                    dragEnd.setLocation(dragEnd.x, baseControlPoint.getCenterY());
                } else if (selectedRadiusControlPoint) {
                    // restrain to horizontal movement and avoid radius getting 0
                    dragEnd.setLocation(dragEnd.x, radiusControlPoint.getCenterY());
                    if (dragEnd.x - baseControlPoint.getCenterX() <= 0) {
                        dragEnd.x = (int) (baseControlPoint.getCenterX() + 1);
                    }
                }
                
                // extend widget
                else if(selectedLowControlPoint) {
                    // restrain to vertical movement and avoid out of range
                    if(dragEnd.y > h) {
                         dragEnd.y = (int)h;
                    }
                    if(dragEnd.y < 0) {
                         dragEnd.y = 0;
                    }
                    dragEnd.setLocation(lowControlPoint.getCenterX(), dragEnd.y);
                }
                else if(selectedUpControlPoint) {
                    // restrain to vertical movement and avoid out of range
                    if(dragEnd.y > h) {
                         dragEnd.y = (int)h;
                    }
                    if(dragEnd.y < 0) {
                         dragEnd.y = 0;
                    }
                    dragEnd.setLocation(upControlPoint.getCenterX(), dragEnd.y);
                }
                
                if (dragEnd.x < 0) {
                    dragEnd.x = 0;
                }
                if (dragEnd.x >= getWidth()) {
                    dragEnd.x = getWidth() - 1;
                }
                
                if (selectedBaseControlPoint) {
                    ed.triangleWidget.baseIntensity = (short) (dragEnd.x / binWidth);
                } else if (selectedRadiusControlPoint) {
                    ed.triangleWidget.radius = (dragEnd.x - (ed.triangleWidget.baseIntensity * binWidth))/(binWidth * ed.maxGradientMagnitude);
                }
                                
                // extend widget
                else if(selectedLowControlPoint) {
                    double newLowGradientMagnitude = ed.maxGradientMagnitude * (h - dragEnd.y) / h;
                    if(newLowGradientMagnitude > ed.triangleWidget.upGradientMagnitude) {
                        ed.triangleWidget.lowGradientMagnitude = ed.triangleWidget.upGradientMagnitude;
                        ed.triangleWidget.upGradientMagnitude = newLowGradientMagnitude;
                    }
                    else {
                        ed.triangleWidget.lowGradientMagnitude = newLowGradientMagnitude;
                    }
                    
//                    System.out.println(ed.triangleWidget.lowGradientMagnitude);
                }
                else if(selectedUpControlPoint) {
                    double newUpGradientMagnitude = ed.maxGradientMagnitude * (h - dragEnd.y) / h;
                    if(newUpGradientMagnitude < ed.triangleWidget.lowGradientMagnitude) {
                        ed.triangleWidget.upGradientMagnitude = ed.triangleWidget.lowGradientMagnitude;
                        ed.triangleWidget.lowGradientMagnitude = newUpGradientMagnitude;
                    }
                    else {
                        ed.triangleWidget.upGradientMagnitude = newUpGradientMagnitude;
                    }
//                    System.out.println(ed.triangleWidget.upGradientMagnitude);
                }
                
                ed.setSelectedInfo();
                
                repaint();
            } 
        }

    }
    
    
    private class SelectionHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (baseControlPoint.contains(e.getPoint())) {
                selectedBaseControlPoint = true;
            } else if (radiusControlPoint.contains(e.getPoint())) {
                selectedRadiusControlPoint = true;
            } else {
                selectedRadiusControlPoint = false;
                selectedBaseControlPoint = false;
            }
            
            // extend widget
            if (lowControlPoint.contains(e.getPoint())) {
                selectedLowControlPoint = true;
            } else if (upControlPoint.contains(e.getPoint())) {
                selectedUpControlPoint = true;
            } else {
                selectedLowControlPoint = false;
                selectedUpControlPoint = false;
            }            
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            selectedRadiusControlPoint = false;
            selectedBaseControlPoint = false;
            
            // extend widget
            selectedLowControlPoint = false;
            selectedUpControlPoint = false;
            
            ed.changed();
            repaint();
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import java.awt.Color;
import java.util.ArrayList;
import util.TFChangeListener;

/**
 *
 * @author michel
 */
public class TransferFunction {

    private ArrayList<TFChangeListener> listeners = new ArrayList<TFChangeListener>();
    
    public TransferFunction(short min, short max) {
        sMin = min;
        sMax = max;
        sRange = sMax - sMin;
        controlPoints = new ArrayList<ControlPoint>();

        controlPoints.add(new ControlPoint(min, new TFColor(0.0, 0.0, 0.0, 0.0)));
        controlPoints.add(new ControlPoint(max, new TFColor(1.0, 1.0, 1.0, 1.0)));

        LUTsize = sRange;
        LUT = new TFColor[LUTsize];

        buildLUT();

    }
    
    
    public void setTestFunc() {
        // control points for orange data set
        addControlPoint(0, 0.0, 0.0, 0.0, 0.0);
        addControlPoint(40, 0.0, 0.0, 0.0, 0.0);
        addControlPoint(75, 1.0, 0.666, 0.0, 1.0);
        addControlPoint(103, 0.0, 0.0, 0.0, 0.5);
        addControlPoint(205, 0.0, 0.0, 0.0, 0.0);
    }

    public int getMinimum() {
        return sMin;
    }

    public int getMaximum() {
        return sMax;
    }

    public void addTFChangeListener(TFChangeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }
    
    public ArrayList<ControlPoint> getControlPoints() {
        return controlPoints;
    }

    public TFColor getColor(int value) {
        return LUT[computeLUTindex(value)];
    }

    
    public int addControlPoint(int value, double r, double g, double b, double a) {
        if (value < sMin || value > sMax) {
            return -1;
        }
        a = Math.floor(a*100)/100.0;
        
        ControlPoint cp = new ControlPoint(value, new TFColor(r, g, b, a));
        int idx = 0;
        while (idx < controlPoints.size() && controlPoints.get(idx).compareTo(cp) < 0) {
                idx++;  
        }
        
        
        if (controlPoints.get(idx).compareTo(cp) == 0) {
            controlPoints.set(idx, cp);
        } else {
            controlPoints.add(idx, cp);
        }

        buildLUT();
        return idx;
    }

    public void removeControlPoint(int idx) {
        controlPoints.remove(idx);
        buildLUT();
    }
    
    public void updateControlPointScalar(int index, int s) {
        controlPoints.get(index).value = s;
        buildLUT();
    }
    
    public void updateControlPointAlpha(int index, double alpha) {
        alpha = Math.floor(alpha*100)/100.0;
        controlPoints.get(index).color.a = alpha;
        buildLUT();
    }
    
    public void updateControlPointColor(int idx, Color c) {
        ControlPoint cp = controlPoints.get(idx);
        cp.color.r = c.getRed()/255.0;
        cp.color.g = c.getGreen()/255.0;
        cp.color.b = c.getBlue()/255.0;
        buildLUT();
    }
    
    public void changed() {
        for (int i=0; i<listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
    
    private int computeLUTindex(int value) {
        int idx = ((LUTsize - 1) * (value - sMin)) / sRange;
        return idx;
    }

    private void buildLUT() {

        for (int i = 1; i < controlPoints.size(); i++) {
            ControlPoint prev = controlPoints.get(i - 1);
            ControlPoint next = controlPoints.get(i);
            //System.out.println(prev.value + " " + prev.color + " -- " + next.value + " " + next.color);
            double range = next.value - prev.value;
            for (int k = prev.value; k <= next.value; k++) {
                double frac = (k - prev.value) / range;
                TFColor newcolor = new TFColor();
                newcolor.r = prev.color.r + frac * (next.color.r - prev.color.r);
                newcolor.g = prev.color.g + frac * (next.color.g - prev.color.g);
                newcolor.b = prev.color.b + frac * (next.color.b - prev.color.b);
                newcolor.a = prev.color.a + frac * (next.color.a - prev.color.a);
                LUT[computeLUTindex(k)] = newcolor;
            }

        }


    }

    public class ControlPoint implements Comparable<ControlPoint> {

        public int value;
        public TFColor color;

        public ControlPoint(int v, TFColor c) {
            value = v;
            color = c;
        }

        @Override
        public int compareTo(ControlPoint t) {
            return (value < t.value ? -1 : (value == t.value ? 0 : 1));
        }
        
        @Override
        public String toString() {
            return new String("(" + value + ") -> " + color.toString());
        }
        
    }
    private short sMin, sMax;
    private int sRange;
    private TFColor[] LUT;
    private int LUTsize = 4095;
    private ArrayList<ControlPoint> controlPoints;
}

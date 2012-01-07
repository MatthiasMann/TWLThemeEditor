/*
 * Copyright (c) 2008-2012, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.model.AbstractFloatModel;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.SimpleListModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import java.util.ArrayList;
import java.util.Arrays;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

/**
 *
 * @author Matthias Mann
 */
public class GradientStopModel extends SimpleListModel<GradientStopModel.Stop> {

    public static final float DELTA     = 1e-2f;
    public static final float MAX_DELTA = 1e+2f;

    private final Element gradientElement;
    private final Runnable domChangedCB;
    private final ArrayList<Stop> stops;
    private final String stopIndentation;

    GradientStopModel(Element gradientElement, ThemeTreeNode node, Runnable domChangedCB) {
        this.gradientElement = gradientElement;
        this.domChangedCB = domChangedCB;
        this.stops = new ArrayList<Stop>();
        
        for(int i=0,n=gradientElement.getContentSize() ; i<n ; i++) {
            Content content = gradientElement.getContent(i);
            if(content instanceof Element) {
                Element stopElement = (Element)content;
                if("stop".equals(stopElement.getName())) {
                    try {
                        Stop stop = new Stop(stopElement);
                        stop.nr = stops.size();
                        stop.pos = Float.parseFloat(stopElement.getAttributeValue("pos"));
                        stop.color = ColorProperty.parseColor(stopElement.getAttributeValue("color"), node);
                        stops.add(stop);
                    } catch(IllegalArgumentException ex) {
                    }
                }
            }
        }
        
        Stop endStop = new Stop(null);
        endStop.nr = stops.size();
        stops.add(endStop);
        
        int indentation = 0;
        for(Element e=gradientElement ; e!=null ; e=e.getParentElement()) {
            indentation += INDENTATION_SIZE;
        }
        
        char[] buf = new char[indentation + 1];
        Arrays.fill(buf, ' ');
        buf[0] = '\n';
        stopIndentation = new String(buf);
    }
    
    public Stop getEntry(int index) {
        return stops.get(index);
    }

    public int getNumEntries() {
        return stops.size();
    }
    
    Stop getStop(int nr) {
        if(nr >= 0 & nr < stops.size()) {
            return stops.get(nr);
        }
        return null;
    }
    
    Float computeInsertPos(Stop stop) {
        Stop prev = getStop(stop.nr-1);
        float prevPos = (prev != null) ? prev.pos : 0.0f;
        if(stop.isSpecial()) {
            return prevPos + 10.0f;
        }
        if(prev != null) {
            float delta = stop.pos - prevPos;
            if(delta < 2*DELTA) {
                return null;
            }
            return prevPos + delta*0.5f;
        } else {
            float newPos = stop.pos * 0.5f;
            if(newPos < DELTA) {
                return null;
            }
            return newPos;
        }
    }
    
    Color computeInsertColor(Stop stop) {
        final Stop prev = getStop(stop.nr-1);
        Color prevColor = (prev != null) ? prev.color : null;
        if(stop.isSpecial()) {
            return (prevColor != null) ? prevColor : Color.WHITE;
        }
        Color thisColor = stop.color;
        if(prevColor != null) {
            return new Color(
                    (byte)((prevColor.getRed()   + thisColor.getRed())   / 2),
                    (byte)((prevColor.getGreen() + thisColor.getGreen()) / 2),
                    (byte)((prevColor.getBlue()  + thisColor.getBlue())  / 2),
                    (byte)((prevColor.getAlpha() + thisColor.getAlpha()) / 2));
        } else {
            return thisColor;
        }
    }
        
    void updateRow(int row) {
        if(row >= 0 && row < stops.size()) {
            fireEntriesChanged(row, row);
        }
    }

    void posUpdated(int row) {
        updateRow(row-1);
        updateRow(row+1);
    }
    
    boolean insertBefore(Stop stop) {
        final Float newPos = computeInsertPos(stop);
        if(newPos == null) {
            return false;
        }
        final int rowNr = stop.nr;
        
        Stop newStop = new Stop(new Element("stop"));
        newStop.nr = rowNr;
        newStop.pos = newPos;
        newStop.color = computeInsertColor(stop);
        newStop.setPos();
        newStop.setColor();
        
        int elementPos = stop.isSpecial()
                ? gradientElement.getContentSize()
                : gradientElement.indexOf(stop.stopElement);
        if(elementPos > 0 && gradientElement.getContent(elementPos-1) instanceof Text) {
            elementPos--;
        }
        
        gradientElement.addContent(elementPos  , new Text(stopIndentation));
        gradientElement.addContent(elementPos+1, newStop.stopElement);
        
        for(int i=rowNr ; i<stops.size() ; i++) {
            stops.get(i).nr++;
        }
        stops.add(rowNr, newStop);
        fireEntriesInserted(rowNr, rowNr);
        posUpdated(rowNr);
        domChanged();
        return true;
    }

    boolean canRemove(Stop stop) {
        return stops.size() > 1 && !stop.isSpecial();
    }
    
    boolean remove(Stop stop) {
        final int rowNr = stop.nr;
        if(!canRemove(stop)) {
            return false;
        }

        int elementPos = gradientElement.indexOf(stop.stopElement);
        gradientElement.removeContent(elementPos);
        if(elementPos > 0) {
            Content prevContent = gradientElement.getContent(elementPos-1);
            if(prevContent instanceof Text) {
                gradientElement.removeContent(elementPos-1);
            }
        }
        
        stops.remove(rowNr);
        for(int i=rowNr ; i<stops.size() ; i++) {
            stops.get(i).nr--;
        }
        fireEntriesDeleted(rowNr, rowNr);
        updateRow(rowNr-1);
        updateRow(rowNr);
        domChanged();
        return true;
    }
    
    void domChanged() {
        domChangedCB.run();
    }
    
    private static final int INDENTATION_SIZE = 4;
    
    public class Stop {
        final Element stopElement;
        final FloatModel posModel;
        final ColorModel colorModel;
        int nr;
        float pos;
        Color color;

        Stop(Element stopElement) {
            this.stopElement = stopElement;
            if(stopElement == null) {
                this.posModel = null;
                this.colorModel = null;
            } else {
                this.posModel = new PM();
                this.colorModel = new CM();
            }
        }

        public FloatModel getPosModel() {
            return posModel;
        }

        public ColorModel getColorModel() {
            return colorModel;
        }

        public boolean isSpecial() {
            return stopElement == null;
        }
        
        public boolean canInsertBefore() {
            return GradientStopModel.this.computeInsertPos(this) != null;
        }
        
        public boolean insertBefore() {
            return GradientStopModel.this.insertBefore(this);
        }
        
        public boolean canRemove() {
            return GradientStopModel.this.canRemove(this);
        }
        
        public boolean remove() {
            return GradientStopModel.this.remove(this);
        }
        
        void setPos() {
            stopElement.setAttribute("pos", Float.toString(pos));
        }
        
        void setColor() {
            stopElement.setAttribute("color", color.toString());
        }
        
        class PM extends AbstractFloatModel {
            public float getMinValue() {
                Stop prev = getStop(nr-1);
                return (prev != null)
                        ? Math.min(pos, prev.pos + DELTA)
                        : 0.0f;
            }
            public float getMaxValue() {
                Stop next = getStop(nr+1);
                return (next != null && !next.isSpecial())
                        ? Math.max(pos, next.pos - DELTA)
                        : pos + MAX_DELTA;
            }
            public float getValue() {
                return pos;
            }
            public void setValue(float value) {
                if(Math.abs(value - pos) > 1e-5) {
                    pos = value;
                    doCallback();
                    setPos();
                    posUpdated(nr);
                    domChanged();
                }
            }
        };
        class CM extends HasCallback implements ColorModel {
            public Color getValue() {
                return color;
            }
            public void setValue(Color value) {
                if(!color.equals(value)) {
                    color = value;
                    doCallback();
                    setColor();
                    domChanged();
                }
            }
        }
    }
}

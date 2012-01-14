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
import de.matthiasmann.twlthemeeditor.dom.Attribute;
import de.matthiasmann.twlthemeeditor.dom.AttributeList.AttributeListListener;
import de.matthiasmann.twlthemeeditor.dom.Content;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.Parent;
import de.matthiasmann.twlthemeeditor.dom.Parent.ContentListener;
import de.matthiasmann.twlthemeeditor.dom.Text;
import de.matthiasmann.twlthemeeditor.dom.Undo;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Matthias Mann
 */
public final class GradientStopModel extends SimpleListModel<GradientStopModel.Stop> {

    public static final float DELTA     = 1e-2f;
    public static final float MAX_DELTA = 1e+2f;

    private final Element gradientElement;
    final ThemeTreeNode node;
    private final ArrayList<Stop> stops;
    private final String stopIndentation;

    GradientStopModel(Element gradientElement, ThemeTreeNode node) {
        this.gradientElement = gradientElement;
        gradientElement.addContentListener(new ContentListener() {
            public void contentAdded(Parent parent, Content child, int index) {
                if(isStop(child)) {
                    stopAdded((Element)child, index);
                }
            }
            public void contentRemoved(Parent parent, Content child, int index) {
                if(isStop(child)) {
                    stopRemoved(child);
                }
            }
            public void contentMoved(Parent parent, Content child, int oldIndex, int newIndex) {
                throw new UnsupportedOperationException("Not supported");
            }
        });
        this.node = node;
        this.stops = new ArrayList<Stop>();
        
        for(int i=0,n=gradientElement.getContentSize() ; i<n ; i++) {
            Content content = gradientElement.getContent(i);
            if(isStop(content)) {
                stopAdded((Element)content, i);
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
    
    static boolean isStop(Content child) {
        return (child instanceof Element) && "stop".equals(((Element)child).getName());
    }
    
    public Stop getEntry(int index) {
        return stops.get(index);
    }

    public int getNumEntries() {
        return stops.size();
    }
    
    void stopAdded(Element stopElement, int index) {
        int stopIdx = 0;
        for(int n=stops.size() ; stopIdx<n ; stopIdx++) {
            Stop stop = stops.get(stopIdx);
            if(stop.stopElement == null || index < stop.stopElement.getIndex()) {
                break;
            }
        }
        
        Stop stop = new Stop(stopElement);
        stops.add(stopIdx, stop);
        renumber(stopIdx);
        fireEntriesInserted(stopIdx, stopIdx);
        posUpdated(stopIdx);
    }
    
    void stopRemoved(Content child) {
        for(int i=0,n=stops.size()-1 ; i<n ; i++) {
            if(child == stops.get(i).stopElement) {
                stops.remove(i);
                renumber(i);
                fireEntriesDeleted(i, i);
                updateRow(i-1);
                updateRow(i);
                break;
            }
        }
    }
    
    void renumber(int idx) {
        for(int n=stops.size() ; idx<n ; idx++) {
            stops.get(idx).nr = idx;
        }
    }
    
    Stop getStop(int nr) {
        if(nr >= 0 & nr < stops.size()) {
            return stops.get(nr);
        }
        return null;
    }
    
    Float computeInsertPos(Stop stop) {
        Stop prev = getStop(stop.nr-1);
        float prevPos = (prev != null) ? prev.getPos() : 0.0f;
        if(stop.isSpecial()) {
            return prevPos + 10.0f;
        }
        if(prev != null) {
            float delta = stop.getPos() - prevPos;
            if(delta < 2*DELTA) {
                return null;
            }
            return prevPos + delta*0.5f;
        } else {
            float newPos = stop.getPos() * 0.5f;
            if(newPos < DELTA) {
                return null;
            }
            return newPos;
        }
    }
    
    Color computeInsertColor(Stop stop) {
        final Stop prev = getStop(stop.nr-1);
        Color prevColor = (prev != null) ? prev.getColor() : null;
        if(stop.isSpecial()) {
            return (prevColor != null) ? prevColor : Color.WHITE;
        }
        Color thisColor = stop.getColor();
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
            Stop stop = stops.get(row);
            if(stop.posModel != null) {
                stop.posModel.doCallbackDirect();
            }
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
        
        Element stopElement = new Element("stop");
        stopElement.setAttribute("pos", Float.toString(newPos));
        stopElement.setAttribute("color", computeInsertColor(stop).toString());
        
        int elementPos = stop.isSpecial()
                ? gradientElement.getContentSize()
                : gradientElement.indexOf(stop.stopElement);
        if(elementPos > 0 && gradientElement.getContent(elementPos-1) instanceof Text) {
            elementPos--;
        }
        
        Undo.startComplexOperation();
        try {
            gradientElement.addContent(elementPos  , new Text(stopIndentation));
            gradientElement.addContent(elementPos+1, stopElement);
        } finally {
            Undo.endComplexOperation();
        }
        return true;
    }

    boolean canRemove(Stop stop) {
        return stops.size() > 1 && !stop.isSpecial();
    }
    
    boolean remove(Stop stop) {
        if(!canRemove(stop)) {
            return false;
        }

        Undo.startComplexOperation();
        try {
            int elementPos = gradientElement.indexOf(stop.stopElement);
            gradientElement.removeContent(elementPos);
            if(elementPos > 0) {
                Content prevContent = gradientElement.getContent(elementPos-1);
                if(prevContent instanceof Text) {
                    gradientElement.removeContent(elementPos-1);
                }
            }
        } finally {
            Undo.endComplexOperation();
        }
        return true;
    }
    
    private static final int INDENTATION_SIZE = 4;
    
    public class Stop {
        final Element stopElement;
        final PM posModel;
        final CM colorModel;
        int nr;

        Stop(Element stopElement) {
            this.stopElement = stopElement;
            if(stopElement == null) {
                this.posModel = null;
                this.colorModel = null;
            } else {
                this.posModel = new PM();
                this.colorModel = new CM();
                
                stopElement.getAttributes().addListener(new AttributeListListener() {
                    public void attributeAdded(Element element, Attribute attribute, int index) {
                    }
                    public void attributeRemoved(Element element, Attribute attribute, int index) {
                    }
                    public void attributeChanged(Element element, Attribute attribute, String oldValue, String newValue) {
                        checkAttribute(attribute.getName());
                    }
                });
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
        
        public float getPos() {
            return Float.parseFloat(stopElement.getAttributeValue("pos"));
        }
        
        public Color getColor() {
            return ColorProperty.parseColor(stopElement.getAttributeValue("color"), node);
        }
        
        void checkAttribute(String attributeName) {
            if("pos".equals(attributeName)) {
                posModel.doCallback();
            } else if("color".equals(attributeName)) {
                colorModel.doCallback();
            }
        }
        
        class PM extends AbstractFloatModel {
            public float getMinValue() {
                Stop prev = getStop(nr-1);
                return (prev != null)
                        ? Math.min(getPos(), prev.getPos() + DELTA)
                        : 0.0f;
            }
            public float getMaxValue() {
                Stop next = getStop(nr+1);
                return (next != null && !next.isSpecial())
                        ? Math.max(getPos(), next.getPos() - DELTA)
                        : getValue() + MAX_DELTA;
            }
            public float getValue() {
                return getPos();
            }
            public void setValue(float value) {
                stopElement.setAttribute("pos", Float.toString(value));
            }
            protected void doCallbackDirect() {
                super.doCallback();
            }
            @Override
            protected void doCallback() {
                posUpdated(nr);
                super.doCallback();
            }
        };
        class CM extends HasCallback implements ColorModel {
            public Color getValue() {
                return getColor();
            }
            public void setValue(Color value) {
                stopElement.setAttribute("color", value.toString());
            }
            @Override
            protected void doCallback() {
                super.doCallback();
            }
        }
    }
}

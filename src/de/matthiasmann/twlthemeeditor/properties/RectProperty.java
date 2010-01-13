/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.Property;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class RectProperty implements Property<Rect> {

    private final IntegerProperty baseX;
    private final IntegerProperty baseY;
    private final IntegerProperty baseW;
    private final IntegerProperty baseH;
    private final String name;

    public RectProperty(Element element, String name) {
        this.baseX = new IntegerProperty(new AttributeProperty(element, "x"), 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getX() - 1;
            }
        };
        this.baseY = new IntegerProperty(new AttributeProperty(element, "y"), 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getY() - 1;
            }
        };
        this.baseW = new IntegerProperty(new AttributeProperty(element, "width"), 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getX();
            }
        };
        this.baseH = new IntegerProperty(new AttributeProperty(element, "height"), 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getY();
            }
        };
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<Rect> getType() {
        return Rect.class;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public Rect getPropertyValue() {
        return new Rect(
                baseX.getValue(),
                baseY.getValue(),
                baseW.getValue(),
                baseH.getValue());
    }

    public void setPropertyValue(Rect value) throws IllegalArgumentException {
        baseX.setValue(value.getX());
        baseY.setValue(value.getY());
        baseW.setValue(value.getWidth());
        baseH.setValue(value.getHeight());
    }

    public void addValueChangedCallback(Runnable cb) {
        baseX.addValueChangedCallback(cb);
        baseY.addValueChangedCallback(cb);
        baseW.addValueChangedCallback(cb);
        baseH.addValueChangedCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        baseX.removeValueChangedCallback(cb);
        baseY.removeValueChangedCallback(cb);
        baseW.removeValueChangedCallback(cb);
        baseH.removeValueChangedCallback(cb);
    }

    public abstract Dimension getLimit();

}

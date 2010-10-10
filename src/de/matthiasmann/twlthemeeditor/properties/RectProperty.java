/*
 * Copyright (c) 2008-2010, Matthias Mann
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

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public abstract class RectProperty implements Property<Rect> {

    private final BooleanModel wholeArea;
    private final IntegerModel baseX;
    private final IntegerModel baseY;
    private final IntegerModel baseW;
    private final IntegerModel baseH;
    private final String name;

    public RectProperty(Property<String> xywh, String name) {
        this.wholeArea = new WholeAreaModel(xywh);
        this.baseX = new RectPartProperty(xywh, 0) {
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    super.setValue(value);
                    baseW.setValue(Math.min(getLimit().getX() - value, baseW.getValue()));
                }
            }
        };
        this.baseY = new RectPartProperty(xywh, 1) {
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    super.setValue(value);
                    baseH.setValue(Math.min(getLimit().getY() - value, baseH.getValue()));
                }
            }
        };
        this.baseW = new RectPartProperty(xywh, 2) {
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    super.setValue(value);
                    baseX.setValue(Math.min(getLimit().getX() - value, baseX.getValue()));
                }
            }
        };
        this.baseH = new RectPartProperty(xywh, 3) {
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    super.setValue(value);
                    baseY.setValue(Math.min(getLimit().getY() - value, baseY.getValue()));
                }
            }
        };
        this.name = name;
    }
    
    public RectProperty(IntegerModel baseX, IntegerModel baseY, IntegerModel baseW, IntegerModel baseH, String name) {
        this.wholeArea = null;
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseW = baseW;
        this.baseH = baseH;
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
        baseX.addCallback(cb);
        baseY.addCallback(cb);
        baseW.addCallback(cb);
        baseH.addCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        baseX.removeCallback(cb);
        baseY.removeCallback(cb);
        baseW.removeCallback(cb);
        baseH.removeCallback(cb);
    }

    public BooleanModel getWholeAreaModel() {
        return wholeArea;
    }

    public AbstractAction[] getActions() {
        return NO_ACTIONS;
    }

    public IntegerModel getXProperty() {
        return baseX;
    }

    public IntegerModel getYProperty() {
        return baseY;
    }

    public IntegerModel getWidthProperty() {
        return baseW;
    }

    public IntegerModel getHeightProperty() {
        return baseH;
    }

    public abstract Dimension getLimit();

    int[] parseXYWH(String xywh) {
        if("*".equals(xywh)) {
            Dimension limit = getLimit();
            return new int[] { 0, 0, limit.getX(), limit.getY() };
        } else {
            return Utils.parseInts(xywh);
        }
    }
    
    class WholeAreaModel extends DerivedProperty<Boolean> implements BooleanModel {
        public WholeAreaModel(Property<String> base) {
            super(base, Boolean.class);
        }
        public boolean getValue() {
            return "*".equals(base.getPropertyValue());
        }
        public void setValue(boolean value) {
            if(value) {
                base.setPropertyValue("*");
            } else {
                Rect rect = RectProperty.this.getPropertyValue();
                base.setPropertyValue(rect.getX()+","+rect.getY()+","+rect.getWidth()+","+rect.getHeight());
            }
        }
        public Boolean getPropertyValue() {
            return getValue();
        }
        public void setPropertyValue(Boolean value) throws IllegalArgumentException {
            setValue(value);
        }
    }

    abstract class RectPartProperty extends DerivedProperty<Integer> implements IntegerModel {
        private final int part;

        public RectPartProperty(Property<String> base, int part) {
            super(base, Integer.class);
            this.part = part;
        }

        public Integer getPropertyValue() {
            return getValue();
        }

        public void setPropertyValue(Integer value) throws IllegalArgumentException {
            setValue(value);
        }

        public int getMinValue() {
            return 0;
        }

        public int getMaxValue() {
            switch (part) {
                case 0:
                case 2: return getLimit().getX();
                case 1:
                case 3: return getLimit().getY();
                default:
                    throw new AssertionError();
            }
        }

        public void setValue(int value) {
            String baseValue = base.getPropertyValue();
            try {
                int[] parts = parseXYWH(baseValue);
                parts[part] = value;
                base.setPropertyValue(Utils.toString(parts));
            } catch (Throwable ex) {
                Logger.getLogger(IntegerProperty.class.getName()).log(Level.SEVERE,
                        "Can't parse value of propterty '" + getName() + "': " + baseValue, ex);
            }
        }

        public int getValue() {
            String baseValue = base.getPropertyValue();
            try {
                return parseXYWH(baseValue)[part];
            } catch (Throwable ex) {
                Logger.getLogger(IntegerProperty.class.getName()).log(Level.SEVERE,
                        "Can't parse value of propterty '" + getName() + "': " + baseValue, ex);
                return 0;
            }
        }
    }

    protected static final AbstractAction[] NO_ACTIONS = new AbstractAction[0];
    
    public abstract class AbstractAction implements Runnable {
        private final String name;
        private final String tooltip;

        public AbstractAction(String name) {
            this.name = name;
            this.tooltip = null;
        }

        public AbstractAction(String name, String tooltip) {
            this.name = name;
            this.tooltip = tooltip;
        }

        public String getName() {
            return name;
        }

        public String getTooltip() {
            return tooltip;
        }
    }
}

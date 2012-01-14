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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.ClassUtils;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.AbstractAction;
import de.matthiasmann.twlthemeeditor.properties.BoundProperty;
import de.matthiasmann.twlthemeeditor.properties.WidgetThemeProperty;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class WidgetPropertyEditor extends ScrollPane {

    private Context ctx;
    private Timer timer;
    private ArrayList<Property<?>> properties;

    public WidgetPropertyEditor() {
        setFixed(ScrollPane.Fixed.HORIZONTAL);
    }

    public void setContext(Context ctx) {
        this.ctx = ctx;
        setContent(null);
    }

    public void setWidget(final Widget testWidget) {
        if(testWidget == null || ctx == null) {
            setContent(null);
            properties = null;
            return;
        }
        
        properties = new ArrayList<Property<?>>();
        properties.add(new WidgetRectProperty(testWidget));
        properties.add(new PolledBoundProperty<Integer>(testWidget, "Preferred Width", "preferredWidth", Integer.class));
        properties.add(new PolledBoundProperty<Integer>(testWidget, "Preferred Height", "preferredHeight", Integer.class));
        properties.add(new WidgetThemeProperty(testWidget, ctx));
        properties.add(new AbstractProperty<AnimationState>() {
            public boolean canBeNull() {
                return false;
            }
            public String getName() {
                return "Animation state";
            }
            public AnimationState getPropertyValue() {
                return testWidget.getAnimationState();
            }
            public Class<AnimationState> getType() {
                return AnimationState.class;
            }
            public boolean isReadOnly() {
                return true;
            }
            public void setPropertyValue(AnimationState value) throws IllegalArgumentException {
                throw new UnsupportedOperationException("Not supported");
            }
        });
        properties.add(new BoundProperty<Boolean>(testWidget, "locallyEnabled",
                BoundProperty.getReadMethod(testWidget, "locallyEnabled", Boolean.class),
                BoundProperty.getWriteMethod(testWidget, "enabled", boolean.class),
                Boolean.class) {

            @Override
            public String getName() {
                return "Enabled";
            }
        });
        addBeanProperties(testWidget, properties);

        PropertyPanel panel = new PropertyPanel(ctx, properties);
        setContent(panel);
    }

    @SuppressWarnings("unchecked")
    private void addBeanProperties(final Widget testWidget, ArrayList<Property<?>> properties) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(testWidget.getClass());
            for(PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if(pd.getReadMethod() != null && pd.getWriteMethod() != null) {
                    if(pd.getReadMethod().getDeclaringClass() != Widget.class) {
                        properties.add(new BoundProperty(testWidget, pd,
                                ClassUtils.mapPrimitiveToWrapper(pd.getPropertyType())));
                    }
                }
            }
        } catch(Throwable ex) {
            Logger.getLogger(WidgetPropertyEditor.class.getName()).log(Level.SEVERE, "can't collect bean properties", ex);
        }
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        timer = gui.createTimer();
        timer.setContinuous(true);
        timer.setDelay(500);
        timer.setCallback(new Runnable() {
            public void run() {
                pollProperties();
            }
        });
        timer.start();
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        timer.stop();
        timer = null;
        super.beforeRemoveFromGUI(gui);
    }

    void pollProperties() {
        if(properties != null) {
            for(int i=0,n=properties.size() ; i<n ; i++) {
                Property<?> p = properties.get(i);
                if(p instanceof PolledBoundProperty<?>) {
                    ((PolledBoundProperty<?>)p).poll();
                }
            }
        }
    }

    static abstract class BoundIntegerProperty extends BoundProperty<Integer> implements IntegerModel {
        public BoundIntegerProperty(Object bean, String name) {
            super(bean, name, Integer.class);
        }
        public void addCallback(Runnable callback) {
            addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            removeValueChangedCallback(callback);
        }
        public int getMinValue() {
            return 0;
        }
        public int getValue() {
            return getPropertyValue();
        }
        @Override
        public boolean isReadOnly() {
            return false;
        }
    }

    static class WidgetRectProperty implements Property<ExtRect>, ExtRect.ExtRectProperty {
        final Widget widget;
        final BoundIntegerProperty propX;
        final BoundIntegerProperty propY;
        final BoundIntegerProperty propW;
        final BoundIntegerProperty propH;
        public WidgetRectProperty(final Widget widget) {
            propX = new BoundIntegerProperty(widget, "x") {
                public int getMaxValue() {
                    return widget.getParent().getInnerWidth()-1;
                }
                public void setValue(int value) {
                    widget.setPosition(value, widget.getY());
                }
            };
            propY = new BoundIntegerProperty(widget, "y") {
                public int getMaxValue() {
                    return widget.getParent().getInnerHeight()-1;
                }
                public void setValue(int value) {
                    widget.setPosition(widget.getX(), value);
                }
            };
            propW = new BoundIntegerProperty(widget, "width") {
                public int getMaxValue() {
                    return widget.getParent().getInnerWidth();
                }
                public void setValue(int value) {
                    widget.setSize(value, widget.getHeight());
                }
            };
            propH = new BoundIntegerProperty(widget, "height") {
                public int getMaxValue() {
                    return widget.getParent().getInnerHeight();
                }
                public void setValue(int value) {
                    widget.setSize(widget.getWidth(), value);
                }
            };
            this.widget = widget;
        }
        public Class<ExtRect> getType() {
            return ExtRect.class;
        }
        public void addValueChangedCallback(Runnable cb) {
            propX.addValueChangedCallback(cb);
            propY.addValueChangedCallback(cb);
            propW.addValueChangedCallback(cb);
            propH.addValueChangedCallback(cb);
        }
        public void removeValueChangedCallback(Runnable cb) {
            propX.removeValueChangedCallback(cb);
            propY.removeValueChangedCallback(cb);
            propW.removeValueChangedCallback(cb);
            propH.removeValueChangedCallback(cb);
        }
        public boolean canBeNull() {
            return false;
        }
        public String getName() {
            return "Widget position & size";
        }
        public boolean isReadOnly() {
            return false;
        }
        public ExtRect getPropertyValue() {
            return new ExtRect(propX.getValue(), propY.getValue(), propW.getValue(), propH.getValue());
        }
        public void setPropertyValue(ExtRect value) throws IllegalArgumentException {
            widget.setPosition(value.x, value.y);
            widget.setSize(value.width, value.height);
        }
        public Dimension getLimit() {
            Widget parent = widget.getParent();
            return new Dimension(parent.getInnerWidth(), parent.getInnerHeight());
        }
        public AbstractAction[] getActions() {
            if(widget.getParent() instanceof TestWidgetContainer) {
                return new AbstractAction[] {
                    new AbstractAction("Adjust size", "Calls the adjustSize() method") {
                        public void run() {
                            try {
                                widget.adjustSize();
                            } catch(Throwable ex) {
                                Logger.getLogger(WidgetPropertyEditor.class.getName()).log(Level.SEVERE, "adjusting widget size", ex);
                            }
                        }
                    },
                    new AbstractAction("Full size") {
                        public void run() {
                            try {
                                Widget parent = widget.getParent();
                                widget.setPosition(0, 0);
                                widget.setSize(parent.getInnerWidth(), parent.getInnerHeight());
                            } catch(Throwable ex) {
                                Logger.getLogger(WidgetPropertyEditor.class.getName()).log(Level.SEVERE, "changing widget size", ex);
                            }
                        }
                    }
                };
            }
            return null;
        }
        public boolean supportsFlipping() {
            return false;
        }
        public boolean supportsWholeArea() {
            return false;
        }
    }

    static class PolledBoundProperty<T> extends BoundProperty<T> {
        private T lastValue;

        public PolledBoundProperty(Object bean, String name, Class<T> type) {
            super(bean, name, type);
        }

        public PolledBoundProperty(Object bean, String displayName, String propertyName, Class<T> type) {
            super(bean, displayName, propertyName, type);
        }

        @Override
        public T getPropertyValue() {
            updateValue();
            return lastValue;
        }

        @Override
        protected void propertyChanged() {
            updateValue();
            super.propertyChanged();
        }

        public void poll() {
            T value = super.getPropertyValue();
            if(value != lastValue) {
                boolean changed = value == null || (value != null && !value.equals(lastValue));
                lastValue = value;
                if(changed) {
                    fireValueChangedCallback();
                }
            }
        }

        private void updateValue() {
            lastValue = super.getPropertyValue();
        }
    }
}

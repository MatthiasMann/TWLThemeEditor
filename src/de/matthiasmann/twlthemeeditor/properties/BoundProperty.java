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

import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twl.utils.ClassUtils;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class BoundProperty<T> extends AbstractProperty<T> {

    private final Object bean;
    private final String name;
    private final Method readMethod;
    private final Method writeMethod;
    private final Class<T> type;

    private boolean gotPropertyChangeEvent;

    public BoundProperty(Object bean, String name, Method readMethod, Method writeMethod, Class<T> type) {
        this.bean = bean;
        this.name = name;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.type = type;

        if(type.isPrimitive()) {
            throw new IllegalArgumentException("Call with wrapper class");
        }
        if(readMethod == null) {
            throw new NullPointerException("readMethod");
        }
        if(readMethod.getParameterTypes().length > 0) {
            throw new IllegalArgumentException("readMethod has parameters");
        }
        if(!type.isAssignableFrom(ClassUtils.mapPrimitiveToWrapper(readMethod.getReturnType()))) {
            throw new IllegalArgumentException("Incompatible return type of readMethod");
        }
        if(writeMethod != null) {
            Class<?>[] parameterTypes = writeMethod.getParameterTypes();
            if(parameterTypes.length != 1 ||
                    !ClassUtils.mapPrimitiveToWrapper(parameterTypes[0]).isAssignableFrom(type)) {
                throw new IllegalArgumentException("Incompatible parameter type of writeMethod");
            }
        }
        
        try {
            Method addPropertyChangeListener = bean.getClass().getMethod("addPropertyChangeListener", String.class, PropertyChangeListener.class);
            addPropertyChangeListener.invoke(bean, name, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    propertyChanged();
                }
            });
        } catch(Exception ex) {
            Logger.getLogger(BoundProperty.class.getName()).log(Level.SEVERE,
                    "Can't bind property: " + name, ex);
        }
    }

    public BoundProperty(Object bean, String name, Class<T> type) {
        this(bean, name, getReadMethod(bean, name, type), getWriteMethod(bean, name, type), type);
    }

    public BoundProperty(Object bean, String displayName, String propertyName, Class<T> type) {
        this(bean, displayName, getReadMethod(bean, propertyName, type), getWriteMethod(bean, propertyName, type), type);
    }

    public BoundProperty(Object bean, PropertyDescriptor pd, Class<T> type) {
        this(bean, pd.getName(), pd.getReadMethod(), pd.getWriteMethod(), type);
    }

    public boolean canBeNull() {
        return false;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public boolean isReadOnly() {
        return writeMethod == null;
    }

    public T getPropertyValue() {
        try {
            return type.cast(readMethod.invoke(bean));
        } catch(Exception ex) {
            throw new RuntimeException("Can't invoke readMethod", ex);
        }
    }

    public void setPropertyValue(T value) throws IllegalArgumentException {
        if(writeMethod == null) {
            throw new UnsupportedOperationException("read only");
        }
        try {
            gotPropertyChangeEvent = false;
            writeMethod.invoke(bean, value);
            if(!gotPropertyChangeEvent) {
                propertyChanged();
            }
        } catch(Exception ex) {
            Logger.getLogger(BoundProperty.class.getName()).log(Level.SEVERE,
                    "Can't invoke writeMethod", ex);
        }
    }

    protected void propertyChanged() {
        gotPropertyChangeEvent = true;
        fireValueChangedCallback();
    }

    public static Method getReadMethod(Object bean, String name, Class<?> type) {
        try {
            String prefix = (type == Boolean.class) ? "is" : "get";
            return bean.getClass().getMethod(prefix.concat(Utils.capitalize(name)));
        } catch(NoSuchMethodException unused) {
            return null;
        } catch(SecurityException unused) {
            return null;
        }
    }
    
    public static Method getWriteMethod(Object bean, String name, Class<?> type) {
        try {
            return bean.getClass().getMethod("set".concat(Utils.capitalize(name)), type);
        } catch(NoSuchMethodException unused) {
            return null;
        } catch(SecurityException unused) {
            return null;
        }
    }
}

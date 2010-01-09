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

import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.utils.ClassUtils;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class PropertyAccessor<T> {

    private final Object obj;
    private final PropertyDescriptor pd;
    private final BooleanModel activeModel;

    private T value;
    private Widget[] widgetsToEnable;
    private Method getLimitMethod;
    private boolean triedLimitMethod;

    @SuppressWarnings("unchecked")
    public PropertyAccessor(Object obj, PropertyDescriptor pd, BooleanModel activeModel) {
        this.obj = obj;
        this.pd = pd;
        this.activeModel = activeModel;

        try {
            value = (T) pd.getReadMethod().invoke(obj);
        } catch (Exception ex) {
            Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(activeModel != null) {
            activeModel.setValue(value != null);
            activeModel.addCallback(new Runnable() {
                public void run() {
                    setProperty();
                    syncWithActive();
                }
            });
        }
    }

    public void setWidgetsToEnable(Widget ... widgetsToEnable) {
        this.widgetsToEnable = widgetsToEnable;
        syncWithActive();
    }

    public boolean isActive() {
        return (activeModel == null) || activeModel.getValue();
    }

    public void addActiveCallback(Runnable cb) {
        if(activeModel != null) {
            activeModel.addCallback(cb);
        }
    }
    
    public T getValue(T defaultValue) {
        return (value != null) ? value : defaultValue;
    }
    
    public void setValue(T value) {
        this.value = value;
        setProperty();
    }

    public Object getObject() {
        return obj;
    }

    public String getDisplayName() {
        return pd.getDisplayName();
    }

    public<A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return pd.getReadMethod().getAnnotation(annotationClass);
    }

    public<LT> LT getLimit(Class<LT> type, LT defaultLimit) {
        if(type.isPrimitive()) {
            throw new IllegalArgumentException("type is a primitive, call with wrapper type");
        }
        if(getLimitMethod == null && !triedLimitMethod) {
            getLimitMethod = initLimitMethod(type);
            triedLimitMethod = true;
        }
        if(getLimitMethod != null) {
            try {
                return type.cast(getLimitMethod.invoke(obj));
            } catch (Exception ex) {
                Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE,
                        "can't invoke limit method: " + getLimitMethod, ex);
            }
        }
        return defaultLimit;
    }

    private Method initLimitMethod(Class<?> type) {
        String methodName = pd.getReadMethod().getName().concat("Limit");
        Class<?> clazz = obj.getClass();
        try {
            Method method = clazz.getMethod(methodName);
            if(!Modifier.isPublic(method.getModifiers())) {
                Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE,
                        "can't access non public limit method: " + methodName +
                        " on class " + clazz.getName());
                return null;
            }
            Class<?> returnType = ClassUtils.mapPrimitiveToWrapper(method.getReturnType());
            if(!type.isAssignableFrom(returnType)) {
                Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE,
                        "return type of limit method " + methodName + " on class " +
                        clazz.getName() + " is incompatible: got " + returnType + " need " + type);
                return null;
            }
            return method;
        } catch (Exception ex) {
            Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE,
                    "can't get limit method: " + methodName + " on class " + clazz.getName(), ex);
            return null;
        }
    }
    
    void setProperty() {
        try {
            pd.getWriteMethod().invoke(obj, isActive() ? value : null);
        } catch (Exception ex) {
            Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void syncWithActive() {
        if(widgetsToEnable != null) {
            boolean isActive = isActive();
            for (Widget w : widgetsToEnable) {
                w.setEnabled(isActive);
            }
        }
    }
}

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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class PropertyAccessor<T> {

    private final Object obj;
    private final PropertyDescriptor pd;
    private final ToggleButton btnActive;

    private T value;
    private Widget[] widgetsToEnable;

    public PropertyAccessor(Object obj, PropertyDescriptor pd, ToggleButton btnActive) {
        this.obj = obj;
        this.pd = pd;
        this.btnActive = btnActive;

        try {
            value = (T) pd.getReadMethod().invoke(obj);
        } catch (Exception ex) {
            Logger.getLogger(PropertyAccessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(btnActive != null) {
            btnActive.setActive(value != null);
            btnActive.addCallback(new Runnable() {
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
        return (btnActive == null) || btnActive.isActive();
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

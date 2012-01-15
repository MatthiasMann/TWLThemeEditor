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

import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.WithRunnableCallback;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @param <T> data type
 * @author Matthias Mann
 */
public abstract class DerivedProperty<T> implements OptionalProperty<T>, HasTooltip, WithRunnableCallback {

    protected final Property<String> base;
    protected final Class<T> type;
    protected final CallbackHandler callbackHandler;
    protected final T defaultValue;
    protected String tooltip;
    
    protected T prevValue;
    protected boolean isPresent;
    protected boolean inSet;

    protected DerivedProperty(Property<String> base, Class<T> type, T defaultValue) {
        if(base == null) {
            throw new NullPointerException("base");
        }
        if(type == null) {
            throw new NullPointerException("type");
        }
        
        this.base = base;
        this.type = type;
        this.callbackHandler = new CallbackHandler();
        this.defaultValue = defaultValue;
        this.prevValue = defaultValue;
    }

    public void removeValueChangedCallback(Runnable cb) {
        callbackHandler.removeCallback(cb);
    }

    public void addValueChangedCallback(Runnable cb) {
        callbackHandler.addCallback(cb);
    }

    public void addCallback(Runnable callback) {
        callbackHandler.addCallback(callback);
    }

    public void removeCallback(Runnable callback) {
        callbackHandler.removeCallback(callback);
    }

    public String getName() {
        return base.getName();
    }

    public boolean canBeNull() {
        return false;
    }

    public Class<T> getType() {
        return type;
    }

    public boolean isOptional() {
        return base.canBeNull();
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getPropertyValue() {
        maybeRefresh();
        return prevValue;
    }

    public void setPropertyValue(T value) throws IllegalArgumentException {
        if(prevValue != value) {
            String str = toString(value);
            isPresent = true;
            inSet = true;
            prevValue = value;
            try {
                base.setPropertyValue(str);
            } finally {
                inSet = false;
                doCallback();
            }
        }
    }

    public boolean isPresent() {
        if(isOptional()) {
            maybeRefresh();
            return isPresent;
        }
        return true;
    }

    public void setPresent(boolean present) {
        if(isOptional()) {
            if(present != isPresent()) {
                isPresent = present;
                inSet = true;
                try {
                    if(present) {
                        base.setPropertyValue(toString(prevValue));
                    } else {
                        base.setPropertyValue(null);
                    }
                } finally {
                    inSet = false;
                    doCallback();
                }
            }
        } else if(!present) {
            throw new UnsupportedOperationException();
        }
    }

    public boolean isReadOnly() {
        return false;
    }

    public String getTooltip() {
        if(tooltip != null) {
            return tooltip;
        }
        if(base instanceof HasTooltip) {
            return ((HasTooltip)base).getTooltip();
        }
        return null;
    }

    public DerivedProperty<T> withTooltip(String tooltip) {
        assert this.tooltip == null : "Tooltip already set";
        this.tooltip = tooltip;
        return this;
    }

    protected void callbackHandlerRegistered() {
        refresh();
    }
    
    protected void callbackHandlerUnregistered() {
    }
    
    protected void baseChanged() {
        if(!inSet) {
            refresh();
            doCallback();
        }
    }
    
    protected void maybeRefresh() {
        if(!hasCallbacks()) {
            refresh();
        }
    }
    
    protected void refresh() {
        String value = base.getPropertyValue();
        if(isOptional() && value == null) {
            isPresent = false;
        } else {
            isPresent = true;
            try {
                prevValue = parse(value);
            } catch(IllegalArgumentException ex) {
                Logger.getLogger(DerivedProperty.class.getName()).log(Level.SEVERE,
                        "Can't parse value of property '" + getName() + "': " + value, ex);
            }
        }
    }
    
    protected abstract T parse(String value) throws IllegalArgumentException;
    protected abstract String toString(T value) throws IllegalArgumentException;
    
    protected boolean hasCallbacks() {
        return callbackHandler.hasCallbacks();
    }

    protected void doCallback() {
        callbackHandler.doCallback();
    }
    
    protected class CallbackHandler extends HasCallback implements Runnable {
        CallbackHandler() {
        }
        @Override
        public void addCallback(Runnable cb) {
            boolean hadCallbacks = super.hasCallbacks();
            super.addCallback(cb);
            if(!hadCallbacks) {
                base.addValueChangedCallback(this);
                callbackHandlerRegistered();
            }
        }
        @Override
        public void removeCallback(Runnable cb) {
            super.removeCallback(cb);
            if(!hasCallbacks()) {
                base.removeValueChangedCallback(cb);
                callbackHandlerUnregistered();
            }
        }
        public void run() {
            baseChanged();
        }
        @Override
        protected void doCallback() {
            super.doCallback();
        }
    }
}

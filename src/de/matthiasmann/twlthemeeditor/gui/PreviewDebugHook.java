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

import de.matthiasmann.twl.DebugHook;
import de.matthiasmann.twl.ParameterMap;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.Widget;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 *
 * @author Matthias Mann
 */
public class PreviewDebugHook extends DebugHook {

    public static class Entry {
        public final Widget widget;
        final ArrayList<String> errorMsg;
        final ArrayList<String> warningMsg;

        public Entry(Widget widget) {
            this.widget = widget;
            this.errorMsg = new ArrayList<String>();
            this.warningMsg = new ArrayList<String>();
        }
    }
    
    private final IdentityHashMap<Widget, Entry> messages;

    private Widget applyThemeWidget;
    private DebugHook previous;

    public PreviewDebugHook() {
        this.messages = new IdentityHashMap<Widget, Entry>();
    }

    @Override
    public void beforeApplyTheme(Widget widget) {
        applyThemeWidget = widget;
        messages.remove(applyThemeWidget);
    }

    @Override
    public void afterApplyTheme(Widget widget) {
        applyThemeWidget = null;
    }

    @Override
    public void missingTheme(String themePath) {
        if(applyThemeWidget != null) {
            getEntryOrCreate(applyThemeWidget).errorMsg.add("Missing theme: " + themePath);
        }
    }

    @Override
    public void missingChildTheme(ThemeInfo parent, String theme) {
        if(applyThemeWidget != null) {
            getEntryOrCreate(applyThemeWidget).errorMsg.add("Missing theme: " + theme);
        }
    }

    @Override
    public void missingParameter(ParameterMap map, String paramName, String parentDescription, Class<?> dataType) {
        if(applyThemeWidget != null) {
            StringBuilder sb = new StringBuilder("Missing ");
            if(dataType != null) {
                if(dataType == Integer.class) {
                    sb.append("int");
                } else if(dataType == Float.class) {
                    sb.append("float");
                } else if(dataType == Boolean.class) {
                    sb.append("boolean");
                } else if(dataType != null) {
                    sb.append(dataType.getSimpleName());
                }
                sb.append(' ');
            }
            sb.append("parameter: ").append(paramName).append(parentDescription);
            getEntryOrCreate(applyThemeWidget).warningMsg.add(sb.toString());
        }
    }

    @Override
    public void wrongParameterType(ParameterMap map, String paramName, Class<?> expectedType, Class<?> foundType, String parentDescription) {
        if(applyThemeWidget != null) {
            getEntryOrCreate(applyThemeWidget).warningMsg.add("Parameter \"" + paramName + "\" is a " +
                    foundType.getSimpleName() + " expected a " +
                    expectedType.getSimpleName() + parentDescription);
        }
    }

    private Entry getEntryOrCreate(Widget widget) {
        Entry e = messages.get(widget);
        if(e == null) {
            e = new Entry(widget);
            messages.put(widget, e);
        }
        return e;
    }

    public Entry getEntry(Widget widget) {
        return messages.get(widget);
    }

    public void clear() {
        messages.clear();
    }
    
    public void install() {
        if(previous != null) {
            throw new IllegalStateException("Already installed");
        }
        previous = DebugHook.installHook(this);
    }
    
    public void uninstall() {
        if(previous == null) {
            throw new IllegalStateException("Not installed");
        }
        DebugHook.installHook(previous);
        previous = null;
        applyThemeWidget = null;
    }
}

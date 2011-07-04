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
import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Matthias Mann
 */
public class WidgetThemeProperty extends AbstractProperty<String> {

    private final Widget widget;
    private final Context ctx;

    public WidgetThemeProperty(Widget widget, Context ctx) {
        this.widget = widget;
        this.ctx = ctx;
    }

    public boolean canBeNull() {
        return false;
    }

    public String getName() {
        return "Theme name";
    }

    public String getPropertyValue() {
        return widget.getTheme();
    }

    public Class<String> getType() {
        return String.class;
    }

    public boolean isReadOnly() {
        return false;
    }

    private static final MessageLog.Category CAT_APPLY_THEME_ERROR =
            new MessageLog.Category("apply theme", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    
    public void setPropertyValue(String value) throws IllegalArgumentException {
        ctx.installDebugHook();
        try {
            widget.setTheme(value);
            ctx.clearWidgetMessages(widget);
            widget.reapplyTheme();
        } catch(Throwable ex) {
            ctx.logMessage(new MessageLog.Entry(CAT_APPLY_THEME_ERROR,
                    "Could not apply theme: " + value, null, ex));
        } finally {
            ctx.uninstallDebugHook();
        }
    }

    public String getThemePath() {
        return widget.getThemePath();
    }

    public String[] getThemePathElements(int skip) {
        ArrayList<String> tmp = new ArrayList<String>();
        Widget w = widget;
        while(skip > 0 && w != null) {
            w = w.getParent();
            --skip;
        }
        for(; w!=null ; w=w.getParent()) {
            String theme = w.getTheme();
            if(!theme.isEmpty()) {
                if(Widget.isAbsoluteTheme(theme)) {
                    tmp.add(theme.substring(1));
                    break;
                } else {
                    tmp.add(theme);
                }
            }
        }
        Collections.reverse(tmp);
        return tmp.toArray(new String[tmp.size()]);
    }
}

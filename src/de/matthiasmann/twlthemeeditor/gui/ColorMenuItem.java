/*
 * Copyright (c) 2008-2011, Matthias Mann
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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.MenuElement;
import de.matthiasmann.twl.MenuManager;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.model.ColorSpaceHSL;

/**
 *
 * @author Matthias Mann
 */
public class ColorMenuItem extends MenuElement {

    private final ColorModel model;

    public ColorMenuItem(ColorModel model, String name) {
        super(name);
        this.model = model;
    }

    @Override
    protected Widget createMenuWidget(MenuManager mm, int level) {
        final ColorButton btn = new ColorButton();
        btn.setText(getName());
        btn.setColorModel(model);
        btn.addCallback(new Runnable() {
            public void run() {
                final ColorSelector cs = new ColorSelector(new ColorSpaceHSL());
                cs.setUseLabels(false);
                cs.setShowPreview(true);
                cs.setShowHexEditField(true);
                cs.setModel(model);
                PopupWindow popup = new PopupWindow(btn);
                popup.setTheme("colorEditorPopup");
                popup.add(cs);
                if(popup.openPopup()) {
                    popup.adjustSize();
                    popup.setPosition(
                            computePos(btn.getX(), popup.getWidth(), popup.getParent().getInnerRight()),
                            computePos(btn.getY(), popup.getHeight(), popup.getParent().getInnerBottom()));
                }
            }
        });
        setWidgetTheme(btn, "colorbtn");
        return btn;
    }
    
    static int computePos(int pos, int required, int avail) {
        return Math.min(pos, avail - required);
    }
}

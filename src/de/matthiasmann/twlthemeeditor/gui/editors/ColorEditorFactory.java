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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.ColorSpaceHSL;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;

/**
 *
 * @author Matthias Mann
 */
public class ColorEditorFactory implements PropertyEditorFactory<Color, ColorProperty> {

    public Widget create(final PropertyAccessor<Color, ColorProperty> pa) {
        final ColorButton button = new ColorButton();
        ColorProperty property = pa.getProperty();
        button.setColor(pa.getValue(Color.WHITE), property.getColorName());
        final Runnable cb = new Runnable() {
            public void run() {
                final ColorSelector cs = new ColorSelector(new ColorSpaceHSL());
                cs.setUseLabels(false);
                cs.setShowPreview(true);
                cs.setShowHexEditField(true);
                cs.setColor(pa.getValue(Color.WHITE));
                cs.addCallback(new Runnable() {
                    public void run() {
                        Color color = cs.getColor();
                        pa.setValue(color);
                        pa.setActive(true);
                        button.setColor(color, null);
                    }
                });
                PopupWindow popup = new PopupWindow(button);
                popup.setTheme("colorEditorPopup");
                popup.add(cs);
                if(popup.openPopup()) {
                    popup.adjustSize();
                    popup.setPosition(
                            computePos(button.getX(), popup.getWidth(), popup.getParent().getInnerRight()),
                            computePos(button.getY(), popup.getHeight(), popup.getParent().getInnerBottom()));
                }
            }
        };
        button.addCallback(cb);
        if(!pa.hasValue()) {
            pa.addActiveCallback(new Runnable() {
                public void run() {
                    if(pa.isActive() && !button.hasOpenPopups() && !pa.hasValue()) {
                        cb.run();
                    }
                }
            });
        }
        
        return button;
    }
    
    static int computePos(int pos, int required, int avail) {
        return Math.min(pos, avail - required);
    }
    
    static class ColorButton extends Button {
        private Image white;
        private Image colored;
        private Color color = Color.WHITE;
        private String colorName;

        public void setColor(Color color, String colorName) {
            this.color = color;
            this.colorName = colorName;
            updateColored();
        }

        @Override
        protected void applyTheme(ThemeInfo themeInfo) {
            super.applyTheme(themeInfo);
            white = themeInfo.getImage("white");
            updateColored();
        }

        @Override
        protected void paintWidget(GUI gui) {
            if(colored != null) {
                colored.draw(getAnimationState(), getX(), getY(), getWidth(), getHeight());
            }
            super.paintWidget(gui);
        }

        private void updateColored() {
            if(white != null) {
                colored = white.createTintedVersion(color);
            }
            String text = color.toString();
            if(colorName != null) {
                text = colorName + " (" + text + ")";
            }
            setText(text);
        }
    }
}

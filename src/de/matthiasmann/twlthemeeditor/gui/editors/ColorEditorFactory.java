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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.model.ColorSpaceHSL;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.gui.ColorButton;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class ColorEditorFactory implements PropertyEditorFactory<Color> {

    public Widget create(final Property<Color> property, ExternalFetaures ef) {
        final ColorModel cm = (property instanceof ColorModel)
                ? (ColorModel)property : new CM(property);
        final ColorButton button = new ColorButton();
        button.setColorModel(cm);
        final Runnable cb = new Runnable() {
            public void run() {
                final ColorSelector cs = new ColorSelector(new ColorSpaceHSL());
                cs.setUseLabels(false);
                cs.setShowPreview(true);
                cs.setShowHexEditField(true);
                cs.setModel(cm);
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
        ef.setPresentAction(new Runnable() {
            public void run() {
                if(!button.hasOpenPopups()) {
                    cb.run();
                }
            }
        });
        return button;
    }
    
    static int computePos(int pos, int required, int avail) {
        return Math.min(pos, avail - required);
    }
        
    static class CM implements ColorModel {
        final Property<Color> property;

        CM(Property<Color> property) {
            this.property = property;
        }

        public Color getValue() {
            return property.getPropertyValue();
        }

        public void setValue(Color value) {
            property.setPropertyValue(value);
        }

        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }

        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
}

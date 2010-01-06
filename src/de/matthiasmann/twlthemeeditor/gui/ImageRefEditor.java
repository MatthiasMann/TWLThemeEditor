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

import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twlthemeeditor.datamodel.ImageReference;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class ImageRefEditor implements PropertyEditorFactory {

    private final Context ctx;

    public ImageRefEditor(Context ctx) {
        this.ctx = ctx;
    }

    public void create(PropertyPanel panel, Group vert, final Object obj, final PropertyDescriptor pd, ToggleButton btnActive) {
        final ComboBox cb = new ComboBox(ctx.getImages());

        Label label = new Label(pd.getDisplayName());
        label.setLabelFor(cb);

        panel.horzColumns[0].addWidget(label);
        panel.horzColumns[1].addWidget(cb);
        vert.addWidget(label).addWidget(cb);

        try {
            ImageReference ref = (ImageReference) pd.getReadMethod().invoke(obj);
            cb.setSelected(ctx.findImage(ref));
        } catch (Exception ex) {
            Logger.getLogger(ImageRefEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        cb.addCallback(new Runnable() {
            public void run() {
                int selected = cb.getSelected();
                if(selected >= 0) {
                    try {
                        pd.getWriteMethod().invoke(obj, ctx.getImages().getEntry(selected).makeReference());
                    } catch (Exception ex) {
                        Logger.getLogger(ImageRefEditor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

}

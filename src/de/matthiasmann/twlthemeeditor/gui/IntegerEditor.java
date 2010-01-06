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

import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.MinValueI;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class IntegerEditor implements PropertyEditorFactory {

    public IntegerEditor() {
    }

    public void create(PropertyPanel panel, Group vert, final Object obj, final PropertyDescriptor pd, final ToggleButton btnActive) {
        Integer value = null;
        try {
            value = (Integer)pd.getReadMethod().invoke(obj);
        } catch (Exception ex) {
            Logger.getLogger(IntegerEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(btnActive != null) {
            btnActive.setActive(value != null);
        }

        MinValueI minValueI = pd.getReadMethod().getAnnotation(MinValueI.class);

        final SimpleIntegerModel model = new SimpleIntegerModel(
                (minValueI != null) ? minValueI.value() : Short.MIN_VALUE,
                Short.MAX_VALUE, (value == null) ? 0 : value.intValue());

        Runnable updateProperty = new Runnable() {
            public void run() {
                try {
                    boolean isActive = (btnActive == null) || btnActive.isActive();
                    pd.getWriteMethod().invoke(obj, isActive ? model.getValue() : null);
                } catch (Exception ex) {
                    Logger.getLogger(IntegerEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        model.addCallback(updateProperty);

        final ValueAdjusterInt va = new ValueAdjusterInt(model);
        final Label label = new Label(pd.getDisplayName());
        label.setLabelFor(va);

        ActiveSynchronizer.sync(btnActive, updateProperty, va, label);

        panel.horzColumns[0].addWidget(label);
        panel.horzColumns[1].addWidget(va);
        vert.addWidget(label).addWidget(va);
    }

}

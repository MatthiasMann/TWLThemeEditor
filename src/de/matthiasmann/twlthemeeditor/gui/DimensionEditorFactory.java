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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.properties.DimensionProperty;

/**
 *
 * @author Matthias Mann
 */
public class DimensionEditorFactory implements PropertyEditorFactory<Dimension, DimensionProperty> {

    public Widget create(PropertyAccessor<Dimension, DimensionProperty> pa) {
        return new DimensionEditor(pa);
    }

    static class DimensionEditor extends DialogLayout implements Runnable {
        private static final Dimension DEFAULT_DIM = new Dimension(0, 0);

        private final PropertyAccessor<Dimension, DimensionProperty> pa;
        private final SimpleIntegerModel modelX;
        private final SimpleIntegerModel modelY;

        public DimensionEditor(PropertyAccessor<Dimension, DimensionProperty> pa) {
            this.pa = pa;
            
            
            Dimension dim = pa.getValue(DEFAULT_DIM);

            this.modelX = new SimpleIntegerModel(0, Short.MAX_VALUE, dim.getX());
            this.modelY = new SimpleIntegerModel(0, Short.MAX_VALUE, dim.getY());

            modelX.addCallback(this);
            modelY.addCallback(this);

            ValueAdjusterInt adjusters[] = new ValueAdjusterInt[] {
                new ValueAdjusterInt(modelX),
                new ValueAdjusterInt(modelY)
            };

            adjusters[0].setDisplayPrefix("X: ");
            adjusters[1].setDisplayPrefix("Y: ");

            setHorizontalGroup(createParallelGroup(adjusters));
            setVerticalGroup(createSequentialGroup().addWidgetsWithGap("adjuster", adjusters));
        }

        public void run() {
            Dimension dim = new Dimension(modelX.getValue(), modelY.getValue());
            pa.setValue(dim);
        }
    }
}

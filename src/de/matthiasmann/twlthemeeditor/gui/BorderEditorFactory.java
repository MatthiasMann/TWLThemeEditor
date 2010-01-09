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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.properties.MinValueI;
import de.matthiasmann.twlthemeeditor.properties.PropertyAccessor;

/**
 *
 * @author Matthias Mann
 */
public class BorderEditorFactory implements PropertyEditorFactory<Border> {

    public Widget create(PropertyAccessor<Border> pa) {
        return new BorderEditor(pa);
    }

    static class BorderEditor extends DialogLayout implements Runnable {
        private final PropertyAccessor<Border> pa;
        private final SimpleIntegerModel modelTop;
        private final SimpleIntegerModel modelLeft;
        private final SimpleIntegerModel modelBottom;
        private final SimpleIntegerModel modelRight;

        private static final int MAX_BORDER_SIZE = 1000;

        public BorderEditor(PropertyAccessor<Border> pa) {
            this.pa = pa;

            Border border = pa.getValue(Border.ZERO);

            MinValueI minValueI = pa.getAnnotation(MinValueI.class);
            int minValue = (minValueI != null) ? minValueI.value() : -MAX_BORDER_SIZE;

            modelTop = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderTop());
            modelLeft = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderLeft());
            modelBottom = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderBottom());
            modelRight = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderRight());

            ValueAdjusterInt adjuster[] = {
                new ValueAdjusterInt(modelTop),
                new ValueAdjusterInt(modelLeft),
                new ValueAdjusterInt(modelBottom),
                new ValueAdjusterInt(modelRight)
            };

            adjuster[0].setDisplayPrefix("T: ");
            adjuster[1].setDisplayPrefix("L: ");
            adjuster[2].setDisplayPrefix("B: ");
            adjuster[3].setDisplayPrefix("R: ");

            modelTop.addCallback(this);
            modelLeft.addCallback(this);
            modelBottom.addCallback(this);
            modelRight.addCallback(this);

            setHorizontalGroup(createParallelGroup(adjuster));
            setVerticalGroup(createSequentialGroup().addWidgetsWithGap("adjuster", adjuster));
        }

        public void run() {
            pa.setValue(new Border(
                    modelTop.getValue(),
                    modelLeft.getValue(),
                    modelBottom.getValue(),
                    modelRight.getValue()));
        }
    }
}

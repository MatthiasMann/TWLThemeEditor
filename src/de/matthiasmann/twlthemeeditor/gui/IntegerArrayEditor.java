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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.InfoWindow;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import java.util.Arrays;

/**
 *
 * @author Matthias Mann
 */
public abstract class IntegerArrayEditor extends DialogLayout {

    private final InfoWindow errorInfoWindow;
    private final Label errorInfoLabel;
    private final AddButton addButton;

    private ValueAdjusterInt[] adjusters;
    private RemoveButton[] removeButtons;

    protected String errorMessage;
    protected int[] array;

    protected IntegerArrayEditor() {
        addButton = new AddButton();

        errorInfoLabel = new Label();
        errorInfoWindow = new InfoWindow(this);
        errorInfoWindow.setTheme("editfield-errorinfowindow");
        errorInfoWindow.add(errorInfoLabel);
    }

    protected void init(int[] array) {
        this.array = array;
        buildControlls();
    }

    protected void buildControlls() {
        if (adjusters != null) {
            removeWidgets(adjusters);
            removeWidgets(removeButtons);
        }

        Group horzAdjusters = createParallelGroup();
        Group horzRemoveButtons = createParallelGroup();
        Group vertControlls = createSequentialGroup();

        adjusters = new ValueAdjusterInt[array.length];
        removeButtons = new RemoveButton[array.length];

        for (int i = 0; i < array.length; i++) {
            adjusters[i] = new ValueAdjusterInt(new ArrayElementModel(i));
            adjusters[i].getAnimationState().setAnimationState("adjusterNotFirst", i > 0);
            adjusters[i].getAnimationState().setAnimationState("adjusterNotLast", i < array.length - 1);
            removeButtons[i] = new RemoveButton(i);

            horzAdjusters.addWidget(adjusters[i]);
            horzRemoveButtons.addWidget(removeButtons[i]);
            if(i > 0) {
                vertControlls.addGap("adjuster");
            }
            vertControlls.addGroup(createParallelGroup().addWidget(adjusters[i]).addWidget(removeButtons[i]));
        }

        setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup().addGroup(horzAdjusters).addGroup(horzRemoveButtons))
                .addGroup(createSequentialGroup().addWidget(addButton).addGap()));
        setVerticalGroup(createSequentialGroup()
                .addGroup(vertControlls)
                .addWidget(addButton));

        updateButtonEnabled();
        invalidateLayoutTree();
    }

    private void removeWidgets(Widget[] widgets) {
        for (Widget w : widgets) {
            removeChild(w);
        }
    }

    protected boolean isValid(int[] array) {
        return array.length > 0;
    }

    protected int getMinValue(int idx) {
        return 0;
    }

    protected int getMaxValue(int idx) {
        return Short.MAX_VALUE;
    }

    protected void setValue(int idx, int value) {
        array[idx] = value;
        updatePropertyCheck();
    }

    protected void setValue(int[] array) {
        this.array = array;
        buildControlls();
        updatePropertyCheck();
    }

    protected void updateButtonEnabled() {
        if(removeButtons != null) {
            for(RemoveButton b : removeButtons) {
                b.updateButtonEnabled();
            }
        }
        addButton.updateButtonEnabled();
    }

    protected void updatePropertyCheck() {
        updateButtonEnabled();
        if(isValid(array)) {
            errorInfoWindow.closeInfo();
            updateProperty();
        } else {
            openErrorInfoWindow();
        }
    }

    @Override
    public void layout() {
        super.layout();
        layoutErrorInfoWindow();
    }

    @Override
    protected void positionChanged() {
        super.positionChanged();
        layoutErrorInfoWindow();
    }

    @Override
    protected void keyboardFocusGained() {
        super.keyboardFocusGained();
        openErrorInfoWindow();
    }

    @Override
    protected void keyboardFocusLost() {
        super.keyboardFocusLost();
        errorInfoWindow.closeInfo();
    }

    protected void openErrorInfoWindow() {
        if(errorMessage != null && hasKeyboardFocus()) {
            errorInfoLabel.setText(errorMessage);
            errorInfoWindow.openInfo();
            layoutErrorInfoWindow();
        }
    }

    protected void layoutErrorInfoWindow() {
        errorInfoWindow.setSize(getWidth(), errorInfoWindow.getPreferredHeight());
        errorInfoWindow.setPosition(getX(), getBottom());
    }

    protected abstract void updateProperty();

    abstract class ModifyArrayLength extends Button implements Runnable {
        public ModifyArrayLength() {
            addCallback(this);
        }

        protected abstract int[] createNewArray();

        public void run() {
            int[] newArray = createNewArray();
            if(isValid(newArray)) {
                setValue(newArray);
            }
        }

        public void updateButtonEnabled() {
            setEnabled(isValid(createNewArray()));
        }
    }

    class AddButton extends ModifyArrayLength {
        @Override
        protected int[] createNewArray() {
            return Arrays.copyOf(array, array.length + 1);
        }
    }
    
    class RemoveButton extends ModifyArrayLength {
        private final int idx;

        public RemoveButton(int idx) {
            this.idx = idx;
        }

        @Override
        protected int[] createNewArray() {
            int[] newArray = new int[array.length - 1];
            System.arraycopy(array, 0, newArray, 0, idx);
            System.arraycopy(array, idx + 1, newArray, idx, newArray.length - idx);
            return newArray;
        }
    }

    class ArrayElementModel extends AbstractIntegerModel {
        private final int idx;

        public ArrayElementModel(int idx) {
            this.idx = idx;
        }

        public int getMaxValue() {
            return IntegerArrayEditor.this.getMaxValue(idx);
        }

        public int getMinValue() {
            return IntegerArrayEditor.this.getMinValue(idx);
        }

        public int getValue() {
            return array[idx];
        }

        public void setValue(int value) {
            IntegerArrayEditor.this.setValue(idx, value);
        }
    }
}

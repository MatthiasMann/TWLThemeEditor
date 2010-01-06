/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twl.model.IntegerModel;
import java.beans.PropertyDescriptor;

/**
 *
 * @author MannMat
 */
public class IntegerEditor implements PropertyEditorFactory {

    public IntegerEditor() {
    }

    public void create(PropertyPanel panel, Group vert, final Object obj, final PropertyDescriptor pd, ToggleButton btnActive) {
        IntegerModel model = new AbstractIntegerModel() {
            public int getValue() {
                try {
                    Integer value = (Integer)pd.getReadMethod().invoke(obj);
                    return (value == null) ? 0 : value.intValue();
                } catch (Exception ex) {
                    throw new RuntimeException("Can't read property: " + pd.getName(), ex);
                }
            }

            public int getMinValue() {
                return Short.MIN_VALUE;
            }

            public int getMaxValue() {
                return Short.MAX_VALUE;
            }

            public void setValue(int value) {
                try {
                    pd.getWriteMethod().invoke(obj, value);
                } catch (Exception ex) {
                    throw new RuntimeException("Can't write property: " + pd.getName(), ex);
                }
            }
        };

        ValueAdjusterInt va = new ValueAdjusterInt(model);
        Label label = new Label(pd.getDisplayName());
        label.setLabelFor(va);

        panel.horzColumns[0].addWidget(label);
        panel.horzColumns[1].addWidget(va);
        vert.addWidget(label).addWidget(va);
    }

}

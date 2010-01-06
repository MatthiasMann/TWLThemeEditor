/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ToggleButton;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MannMat
 */
public class BooleanEditor implements PropertyEditorFactory {

    public void create(PropertyPanel panel, Group vert, final Object obj, final PropertyDescriptor pd, ToggleButton btnActive) {
        final ToggleButton btn = new ToggleButton();
        btn.setTheme("boolean");

        try {
            btn.setActive((Boolean) pd.getReadMethod().invoke(obj));
        } catch (Exception ex) {
            Logger.getLogger(BooleanEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        Label label = new Label(pd.getDisplayName());
        label.setLabelFor(btn);

        panel.horzColumns[0].addWidget(label);
        panel.horzColumns[1].addWidget(btn);
        vert.addWidget(label).addWidget(btn);

        btn.addCallback(new Runnable() {
            public void run() {
                try {
                    pd.getWriteMethod().invoke(obj, btn.isActive());
                } catch (Exception ex) {
                    Logger.getLogger(BooleanEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

}

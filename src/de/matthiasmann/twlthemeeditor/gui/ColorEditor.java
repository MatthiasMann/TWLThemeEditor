/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.model.ColorSpaceHSL;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MannMat
 */
public class ColorEditor implements PropertyEditorFactory {

    public void create(PropertyPanel panel, Group vert, final Object obj, final PropertyDescriptor pd, final ToggleButton btnActive) {
        try {
            final ColorSelector cs = new ColorSelector(new ColorSpaceHSL());
            Color color = (Color) pd.getReadMethod().invoke(obj);
            if(color != null) {
                cs.setColor(color);
                btnActive.setActive(true);
            } else {
                cs.setColor(Color.WHITE);
                cs.setEnabled(false);
                btnActive.setActive(false);
            }

            Label label = new Label(pd.getDisplayName());
            label.setLabelFor(cs);

            final Runnable updateProperty = new Runnable() {
                public void run() {
                    try {
                        boolean isActive = btnActive == null || btnActive.isActive();
                        pd.getWriteMethod().invoke(obj, isActive ? cs.getColor() : null);
                    } catch (Exception ex) {
                        Logger.getLogger(ColorEditor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            
            if(btnActive != null) {
                btnActive.addCallback(new Runnable() {
                    public void run() {
                        cs.setEnabled(btnActive.isActive());
                        updateProperty.run();
                    }
                });
            }
            
            panel.horzComplete.addWidget(label).addWidget(cs);
            vert.addGroup(panel.createSequentialGroup(label, cs));

            cs.addCallback(updateProperty);
        } catch (Exception ex) {
            Logger.getLogger(ColorEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

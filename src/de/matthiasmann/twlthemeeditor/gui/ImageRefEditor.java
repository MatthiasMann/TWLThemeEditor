/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author MannMat
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

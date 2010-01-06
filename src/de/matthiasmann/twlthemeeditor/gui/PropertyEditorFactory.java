/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.ToggleButton;
import java.beans.PropertyDescriptor;

/**
 *
 * @author MannMat
 */
public interface PropertyEditorFactory {

    public void create(PropertyPanel panel, DialogLayout.Group vert, Object obj, PropertyDescriptor pd, ToggleButton btnActive);
    
}

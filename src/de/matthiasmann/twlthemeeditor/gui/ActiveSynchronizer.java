/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;

/**
 *
 * @author Matthias Mann
 */
public class ActiveSynchronizer implements Runnable {

    private final ToggleButton btnActive;
    private final Runnable updateProperty;
    private final Widget[] widgets;

    ActiveSynchronizer(ToggleButton btnActive, Runnable updateProperty, Widget ... widgets) {
        this.btnActive = btnActive;
        this.widgets = widgets;
        this.updateProperty = updateProperty;
    }

    public void run() {
        setEnabled();
        if(updateProperty != null) {
            updateProperty.run();
        }
    }

    void setEnabled() {
        for (Widget w : widgets) {
            w.setEnabled(btnActive.isActive());
        }
    }

    public static void sync(ToggleButton btnActive, Runnable updateProperty, Widget ... widgets) {
        if(btnActive != null) {
            ActiveSynchronizer as = new ActiveSynchronizer(btnActive, updateProperty, widgets);
            btnActive.addCallback(as);
            as.setEnabled();
        }
    }
}

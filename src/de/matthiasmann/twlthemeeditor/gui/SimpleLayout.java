/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Widget;

/**
 *
 * @author Matthias Mann
 */
public class SimpleLayout extends DialogLayout {

    public SimpleLayout(String theme, Widget ... widgets) {
        setTheme(theme);

        Group horzAll = createParallelGroup();
        Group horzLabel = createParallelGroup();
        Group horzControls = createParallelGroup();
        Group vert = createSequentialGroup();

        for(Widget w : widgets) {
            if(w.getParent() == null) {
                if(w instanceof Label) {
                    Label label = (Label)w;
                    Widget control = label.getLabelFor();

                    horzLabel.addWidget(label);
                    horzControls.addWidget(control);
                    vert.addGroup(createParallelGroup().addWidget(label).addWidget(control));
                } else {
                    horzAll.addWidget(w);
                    vert.addWidget(w);
                }
            }
        }
        
        setHorizontalGroup(createParallelGroup(horzAll,
                createSequentialGroup(horzLabel, horzControls)));
        setVerticalGroup(vert);
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Scrollbar;
import de.matthiasmann.twl.ToggleButton;

/**
 *
 * @author MannMat
 */
public class PreviewWidgets extends DialogLayout {

    public PreviewWidgets() {
        Label label = new Label("Label");
        Button button = new Button("Button");
        ToggleButton toggleButton = new ToggleButton("ToggleButton");
        ToggleButton checkBox = new ToggleButton("CheckBox");
        checkBox.setTheme("checkbox");
        Scrollbar scrollbarH = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
        Scrollbar scrollbarV = new Scrollbar(Scrollbar.Orientation.VERTICAL);

        Group horzWidgets = createSequentialGroup()
                .addGroup(createParallelGroup(label, button, toggleButton, checkBox))
                .addGap();
        Group vertWidgets = createSequentialGroup(label, button, toggleButton, checkBox).addGap();

        setHorizontalGroup(createSequentialGroup()
                .addGroup(createParallelGroup().addGroup(horzWidgets).addWidget(scrollbarH))
                .addWidget(scrollbarV));
        setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup().addGroup(vertWidgets).addWidget(scrollbarV))
                .addWidget(scrollbarH));
    }

}

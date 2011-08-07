/*
 * Copyright (c) 2008-2011, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ValueAdjuster;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import de.matthiasmann.twlthemeeditor.gui.CollapsiblePanel;
import de.matthiasmann.twlthemeeditor.gui.PropertyFactories;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Matthias Mann
 */
public class EffectsPanel extends DialogLayout {

    private final DialogLayout.Group hLabels;
    private final DialogLayout.Group hControls;
    private final DialogLayout.Group vRows;
    
    private final PropertyFactories factories;
    private final ArrayList<EffectPropertyPanel> effectPanels;
    private final ArrayList<CollapsiblePanel> effectCollapsiblePanels;

    private Runnable[] callbacks;

    public EffectsPanel() {
        hLabels = createParallelGroup();
        hControls = createParallelGroup();
        vRows = createSequentialGroup();

        factories = new PropertyFactories();
        effectPanels = new ArrayList<EffectPropertyPanel>();
        effectCollapsiblePanels = new ArrayList<CollapsiblePanel>();

        setIncludeInvisibleWidgets(false);
        setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup(hLabels, hControls)));
        setVerticalGroup(vRows);
    }

    public void addEffect(String name, Effect effect) {
        EffectPropertyPanel epp = new EffectPropertyPanel(factories, name, effect);
        epp.addCallback(new Runnable() {
            public void run() {
                fireCallback();
            }
        });

        effectPanels.add(epp);
        effectCollapsiblePanels.add(addCollapsible(name, epp, epp.getEffectActive()));
    }

    public CollapsiblePanel addCollapsible(String name, Widget content, BooleanModel active) {
        CollapsiblePanel panel = new CollapsiblePanel(CollapsiblePanel.Direction.VERTICAL, name, content, active);
        addControl(panel);
        return panel;
    }

    public CollapsiblePanel addCollapsible(String name, final ValueAdjuster[] adjuster, final BooleanModel active) {
        DialogLayout l = new DialogLayout();
        l.setHorizontalGroup(l.createParallelGroup(adjuster));
        l.setVerticalGroup(l.createSequentialGroup().addWidgetsWithGap("adjuster", adjuster));

        if(active != null) {
            Runnable cb = new Runnable() {
                public void run() {
                    boolean enabled = active.getValue();
                    for(ValueAdjuster va : adjuster) {
                        va.setEnabled(enabled);
                    }
                }
            };
            active.addCallback(cb);
            cb.run();
        }

        return addCollapsible(name, l, active);
    }

    public void addControl(String labelText, Widget control) {
        Label label = new Label(labelText);
        label.setLabelFor(control);

        hLabels.addWidget(label);
        hControls.addWidget(control);
        vRows.addGroup(createParallelGroup().addWidget(label).addWidget(control));
    }

    public void addControl(String labelText, Widget control, Button btn) {
        Label label = new Label(labelText);
        label.setLabelFor(control);

        hLabels.addWidget(label);
        hControls.addGroup(createSequentialGroup().addWidget(control).addWidget(btn));
        vRows.addGroup(createParallelGroup().addWidget(label).addWidget(control).addWidget(btn));
    }

    public void addControl(Widget widget) {
        getHorizontalGroup().addWidget(widget);
        getVerticalGroup().addWidget(widget);
    }
    
    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    public void enableEffectsPanels(GeneratorMethod generatorMethod) {
        for(CollapsiblePanel cp : effectCollapsiblePanels) {
            Widget cpContent = cp.getContent();
            if(cpContent instanceof EffectPropertyPanel) {
                cp.setVisible(((EffectPropertyPanel)cpContent).getEffect().supports(generatorMethod));
            }
        }
    }

    void fireCallback() {
        CallbackSupport.fireCallbacks(callbacks);
    }

    public void save(Properties prop) {
        for(EffectPropertyPanel epp : effectPanels) {
            epp.save(prop);
        }
    }

    public void load(Properties prop) {
        for(EffectPropertyPanel epp : effectPanels) {
            epp.load(prop);
        }
    }

    public Effect[] getActiveEffects() {
        ArrayList<Effect> result = new ArrayList<Effect>();
        for(EffectPropertyPanel epp : effectPanels) {
            if(epp.getEffectActive().getValue()) {
                result.add(epp.getEffect());
            }
        }
        return result.toArray(new Effect[result.size()]);
    }
}

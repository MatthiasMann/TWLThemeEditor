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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.PropertyFactories;
import java.util.Properties;

/**
 *
 * @author Matthias Mann
 */
public class EffectPropertyPanel extends DialogLayout {

    private final SimpleBooleanModel effectActive;
    private final String effectName;
    private final Effect effect;
    private final Property<?>[] properties;
    
    public EffectPropertyPanel(PropertyFactories factories, String effectName, Effect effect) {
        this.effectActive = new SimpleBooleanModel();
        this.effectName = effectName;
        this.effect = effect;
        this.properties = effect.getProperties();
        
        Group hLabels = createParallelGroup();
        Group hControls = createParallelGroup();
        Group vRows = createSequentialGroup();

        for(Property<?> p : properties) {
            PropertyEditorFactory<?, ?> factory = factories.getFactory(p);
            if(factory != null) {
                @SuppressWarnings("unchecked")
                Widget control = factory.create(new PropertyAccessor(p, null));

                Label label = new Label(p.getName());
                label.setLabelFor(control);

                hLabels.addWidget(label);
                hControls.addWidget(control);
                vRows.addGroup(createParallelGroup().addWidget(label).addWidget(control));
            } else {
                System.out.println("No factory for property " + p.getName() +
                        " type " + p.getClass() + "<" + p.getType() + ">");
            }
        }

        setHorizontalGroup(createSequentialGroup().addGroup(hLabels).addGroup(hControls));
        setVerticalGroup(vRows);
    }

    public void addCallback(Runnable cb) {
        effectActive.addCallback(cb);
        for(Property<?> p : properties) {
            p.addValueChangedCallback(cb);
        }
    }

    public void removeCallback(Runnable cb) {
        effectActive.removeCallback(cb);
        for(Property<?> p : properties) {
            p.removeValueChangedCallback(cb);
        }
    }
    
    public Effect getEffect() {
        return effect;
    }

    public SimpleBooleanModel getEffectActive() {
        return effectActive;
    }

    public void save(Properties prop) {
        prop.setProperty(getActiveKey(), Boolean.toString(effectActive.getValue()));
        for(Property<?> p : properties) {
            prop.setProperty(getPropertyKey(p), String.valueOf(p.getPropertyValue()));
        }
    }

    @SuppressWarnings("unchecked")
    public void load(Properties prop) {
        effectActive.setValue(Boolean.valueOf(prop.getProperty(getActiveKey())));
        for(Property p : properties) {
            String str = prop.getProperty(getPropertyKey(p));
            if(str != null) {
                Class<?> type = p.getType();
                Object value;
                if(Color.class == type) {
                    value = Color.parserColor(str);
                } else if(Integer.class == type) {
                    value = Integer.valueOf(str);
                } else if(Float.class == type) {
                    value = Float.valueOf(str);
                } else {
                    throw new UnsupportedOperationException("Not implemented: " + type);
                }
                p.setPropertyValue(value);
            }
        }
    }

    private String getActiveKey() {
        return effectName.concat(".active");
    }

    private String getPropertyKey(Property<?> p) {
        return effectName + "." + p.getName().replace(' ', '_');
    }
}

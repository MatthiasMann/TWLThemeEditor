/*
 * Copyright (c) 2008-2012, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.ElementText;

/**
 *
 * @author Matthias Mann
 */
public class ElementTextProperty implements Property<String> {

    private final ElementText elementText;
    private final String name;
    private final PropertyChangeHandler pch;

    public ElementTextProperty(Element element, String name) {
        this.elementText = element.getElementText();
        this.name = name;
        this.pch = new PropertyChangeHandler("text");
    }

    public Class<String> getType() {
        return String.class;
    }

    public String getName() {
        return name;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public String getPropertyValue() {
        return elementText.getText();
    }

    public void setPropertyValue(String value) throws IllegalArgumentException {
        elementText.setText(value);
    }

    public void addValueChangedCallback(Runnable cb) {
        if(pch.addCallback(cb)) {
            elementText.addPropertyChangeListener(pch);
        }
    }

    public void removeValueChangedCallback(Runnable cb) {
        if(pch.removeCallback(cb)) {
            elementText.removePropertyChangeListener(pch);
        }
    }
    
}

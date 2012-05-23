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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.StringModel;
import de.matthiasmann.twl.utils.TextUtil;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class StringEditorFactory implements PropertyEditorFactory<String> {

    public Widget create(Property<String> property, ExternalFetaures externalFetaures) {
        final EditField ef = new EditField();
        ef.setModel(wrap(property));
        if(externalFetaures != null) {
            externalFetaures.disableOnNotPresent(ef);
        }
        return ef;
    }
    
    static StringModel wrap(Property<String> property) {
        return (property instanceof StringModel)
                ? (StringModel)property : new SM(property);
    }
    
    static class SM implements StringModel {
        private final Property<String> property;
        SM(Property<String> property) {
            this.property = property;
        }
        public String getValue() {
            return TextUtil.notNull(property.getPropertyValue());
        }
        public void setValue(String value) {
            property.setPropertyValue(value);
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
}

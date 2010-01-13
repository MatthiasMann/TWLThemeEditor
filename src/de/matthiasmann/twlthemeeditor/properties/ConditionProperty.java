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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.Condition;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class ConditionProperty implements Property<Condition> {

    private final AttributeProperty baseIf;
    private final AttributeProperty baseUnless;
    private final String name;

    public ConditionProperty(Element element, String name) {
        this.baseIf = new AttributeProperty(element, "if", name, true);
        this.baseUnless = new AttributeProperty(element, "unless", name, true);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<Condition> getType() {
        return Condition.class;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public Condition getPropertyValue() {
        String cond = baseIf.getPropertyValue();
        if(cond != null) {
            return new Condition(Condition.Type.IF, cond);
        }
        cond = baseUnless.getPropertyValue();
        if(cond != null) {
            return new Condition(Condition.Type.UNLESS, cond);
        }
        return Condition.NONE;
    }

    public void setPropertyValue(Condition value) throws IllegalArgumentException {
        baseIf.setPropertyValue((value.getType() == Condition.Type.IF) ? value.getCondition() : null);
        baseUnless.setPropertyValue((value.getType() == Condition.Type.UNLESS) ? value.getCondition() : null);
    }

    public void addValueChangedCallback(Runnable cb) {
        baseIf.addValueChangedCallback(cb);
        baseUnless.addValueChangedCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        baseIf.removeValueChangedCallback(cb);
        baseUnless.removeValueChangedCallback(cb);
    }
}

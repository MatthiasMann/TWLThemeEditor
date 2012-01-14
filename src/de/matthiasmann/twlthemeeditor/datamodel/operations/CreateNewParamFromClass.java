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
package de.matthiasmann.twlthemeeditor.datamodel.operations;

import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.themeparams.ThemeParamInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

/**
 *
 * @author Matthias Mann
 */
public class CreateNewParamFromClass extends CreateChildOperation {

    public CreateNewParamFromClass(ThemeTreeNode parent, Element element) {
        super("opNewParamFromClass", parent, element);
    }

    @Override
    public ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException {
        assert parameter.length == 1;
        ThemeParamInfo paramInfo = (ThemeParamInfo)parameter[0];
        if(paramInfo == null) {
            return null;
        }
        
        Element e = new Element("param");
        e.setAttribute("name", paramInfo.getName());

        Element v = new Element(paramInfo.getType());
        if(paramInfo.isEnum()) {
            String enumType = paramInfo.getEnumType();
            
            try {
                Field enumsField = ThemeManager.class.getDeclaredField("enums");
                enumsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Class<?>> enums = (Map<String, Class<?>>)enumsField.get(null);
                for(Map.Entry<String, Class<?>> entry : enums.entrySet()) {
                    if(enumType.equals(entry.getValue().getName())) {
                        enumType = entry.getKey();
                        break;
                    }
                }
            } catch(Exception ex) {
            }
            
            v.setAttribute("type", enumType);
        }
        if(paramInfo.getDefaultValue() != null) {
            v.setText(paramInfo.getDefaultValue());
        }
        e.addContent(v);
        
        return addChild(e, pos);
    }

    @Override
    public Parameter[] getParameter() {
        return new Parameter[] {
            new Parameter("", Parameter.Type.THEME_PARAMETER_INFO)
        };
    }
}

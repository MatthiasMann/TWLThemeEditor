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
package de.matthiasmann.twlthemeeditor.themeparams;

import java.io.Serializable;
import org.objectweb.asm.Type;

/**
 *
 * @author Matthias Mann
 */
public class ThemeParamInfo implements Serializable {
    
    private static final long serialVersionUID = 1;
    
    private final String name;
    private final String type;
    private boolean isEnum;
    private boolean isBoolean;
    private String enumType;
    private String defaultValue;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isBoolean() {
        return isBoolean;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public String getEnumType() {
        return enumType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
    
    private ThemeParamInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }

    private ThemeParamInfo(String name, Type returnType) {
        this.name = name;
        
        switch(returnType.getSort()) {
            case Type.BYTE:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
                type = "int";
                break;
            case Type.LONG:
                type = "int";   // NOTE: Theme doesn't support long
                break;
            case Type.FLOAT:
                type = "float";
                break;
            case Type.DOUBLE:
                type = "float"; // NOTE: Theme doesn't support double
                break;
            case Type.BOOLEAN:
                isBoolean = true;
                type = "bool";
                break;
            case Type.OBJECT:
                String internalName = returnType.getInternalName();
                if("java/lang/String".equals(internalName)) {
                    type = "String";
                } else if("java/lang/Enum".equals(internalName)) {
                    type = "enum";
                    isEnum = true;
                } else if("de/matthiasmann/twl/Border".equals(internalName)) {
                    type = "border";
                } else if("de/matthiasmann/twl/InputMap".equals(internalName)) {
                    type = "inputMap";
                } else if("de/matthiasmann/twl/Dimension".equals(internalName)) {
                    type = "dimension";
                } else if("de/matthiasmann/twl/Color".equals(internalName)) {
                    type = "color";
                } else if("de/matthiasmann/twl/renderer/Image".equals(internalName)) {
                    type = "image";
                    defaultValue = "none";
                } else {
                    type = returnType.getClassName();
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    private void setDefaultValue(Object value) {
        if(value instanceof StaticFieldAccess) {
            StaticFieldAccess field = (StaticFieldAccess)value;
            if(isEnum) {
                enumType = field.className.replace('/', '.');
            }
            defaultValue = field.fieldName;
        } else if(isBoolean && value instanceof Integer) {
            defaultValue = (((Integer)value).intValue() != 0) ? "true" : "false";
        } else if(value != null) {
            defaultValue = value.toString();
        }
    }
    
    public static ThemeParamInfo create(String themeInfoGetter, Type returnType, Object[] args) {
        if(!(args[0] instanceof String)) {
            return null;
        }
        String name = (String)args[0];
        if("getImage".equals(themeInfoGetter)) {
            return new ThemeParamInfo(name, "image");
        }
        if("getMouseCursor".equals(themeInfoGetter)) {
            return new ThemeParamInfo(name, "cursor");
        }
        if("getFont".equals(themeInfoGetter)) {
            return new ThemeParamInfo(name, "cursor");
        }
        if("getParameterMap".equals(themeInfoGetter)) {
            return new ThemeParamInfo(name, "map");
        }
        if("getParameterList".equals(themeInfoGetter)) {
            return new ThemeParamInfo(name, "list");
        }
        if("getParameterValue".equals(themeInfoGetter)) {
            if(args.length >= 3 && args[2] instanceof Type) {
                ThemeParamInfo param = new ThemeParamInfo(name, (Type)args[2]);
                if(args.length >= 4 && args[3] != null) {
                    param.setDefaultValue(args[3]);
                }
                return param;
            } else if(args.length == 2) {
                // default to string
                return new ThemeParamInfo(name, "string");
            }
        }
        if("getParameter".equals(themeInfoGetter)) {
            ThemeParamInfo param = new ThemeParamInfo(name, returnType);
            param.setDefaultValue(args[1]);
            return param;
        }
        
        System.err.println("Unknown themeInfo accessor: " + themeInfoGetter + " with " + args.length + " parameters");
        return null;
    }
}

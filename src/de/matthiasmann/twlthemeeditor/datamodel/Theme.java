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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParam;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewWildcardTheme;
import de.matthiasmann.twlthemeeditor.datamodel.operations.DeleteNodeOperation;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.DerivedNodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Theme extends ThemeTreeNode implements HasProperties {

    protected final NameProperty nameProperty;
    protected final BooleanProperty allowWildcardProperty;
    protected final BooleanProperty mergeProperty;
    protected final DerivedNodeReferenceProperty refProperty;

    public Theme(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(themeFile, parent, element);

        final boolean isTopLevel = element.getParentElement().isRootElement();

        boolean isWildcard = !isTopLevel &&
                "".equals(element.getAttributeValue("name")) &&
                "*".equals(element.getAttributeValue("ref"));

        if(isWildcard) {
            this.nameProperty = null;
            this.allowWildcardProperty = null;
            this.mergeProperty = null;
            this.refProperty = null;
        } else {
            this.nameProperty = new NameProperty(new AttributeProperty(element, "name"), getThemeTreeModel(), Kind.THEME, isTopLevel) {
                @Override
                public void validateName(String name) throws IllegalArgumentException {
                    if(name == null || name.length() == 0) {
                        throw new IllegalArgumentException("empty name not allowed");
                    }
                    if(isTopLevel && getThemeTreeModel().findTopLevelNodes(Theme.class, name, Theme.this) != null) {
                        throw new IllegalArgumentException("Name \"" + name + "\" already in use");
                    }
                }
            };
            addProperty(nameProperty);

            if(isTopLevel) {
                allowWildcardProperty = new BooleanProperty(
                        new AttributeProperty(element, "allowWildcard", "Allow Wildcard", true), false);
                addProperty(allowWildcardProperty);
                mergeProperty = null;
            } else {
                allowWildcardProperty = null;
                mergeProperty = new BooleanProperty(
                        new AttributeProperty(element, "merge", "Merge", true), false);
                addProperty(mergeProperty);
            }

            addProperty(refProperty = new DerivedNodeReferenceProperty(
                    new AttributeProperty(element, "ref", "Base theme reference", true),
                    this, Kind.THEME));
        }

        addProperty(new NodeRefParamProperty("background", Kind.IMAGE, true));
        addProperty(new NodeRefParamProperty("overlay", Kind.IMAGE, true));
        addProperty(new NodeRefParamProperty("font", Kind.FONT, false));
        addProperty(new NodeRefParamProperty("mouseCursor", Kind.CURSOR, false));
        addProperty(new IntegerParamProperty("minWidth", 0, Short.MAX_VALUE));
        addProperty(new IntegerParamProperty("maxWidth", 0, Short.MAX_VALUE));
        addProperty(new IntegerParamProperty("minHeight", 0, Short.MAX_VALUE));
        addProperty(new IntegerParamProperty("maxHeight", 0, Short.MAX_VALUE));
    }

    public String getName() {
        if(nameProperty == null) {
            return "WILDCARD";
        }
        return nameProperty.getPropertyValue();
    }

    public boolean isWildcard() {
        return (nameProperty == null);
    }

    public boolean isAllowWildcard() {
        return allowWildcardProperty != null && allowWildcardProperty.getValue();
    }

    public boolean isMerge() {
        return mergeProperty != null && mergeProperty.getValue();
    }

    public boolean matchName(String name) {
        return nameProperty != null && nameProperty.getPropertyValue().equals(name);
    }

    public NodeReference getRef() {
        return refProperty.getPropertyValue();
    }
    
    public Kind getKind() {
        return Kind.THEME;
    }

    public final Theme getLimit() {
        Theme node = this;
        while(node.getParent() instanceof Theme) {
            node = (Theme)node.getParent();
        }
        return node;
    }
    
    public void addChildren() throws IOException {
        addChildren(themeFile, element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();
                if("theme".equals(tagName)) {
                    return new Theme(themeFile, parent, element);
                }
                if("param".equals(tagName)) {
                    return new Param(Theme.this, parent, element);
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        ThemeFile.addCreateThemeOperation(operations, this, element);
        operations.add(new CreateNewWildcardTheme(this, element));
        Param.addCreateParam(operations, this, element);
        return operations;
    }

    class ParamProperty<T> extends AbstractProperty<T> {
        final String paramName;
        final Class<T> type;

        public ParamProperty(String paramName, Class<T> type) {
            this.paramName = paramName;
            this.type = type;
        }

        public boolean canBeNull() {
            return true;
        }

        public String getName() {
            return paramName;
        }

        public Class<T> getType() {
            return type;
        }

        public boolean isReadOnly() {
            return false;
        }

        public T getPropertyValue() {
            Param param = findParam();
            if(param != null) {
                Property<?> valueProperty = param.getValueProperty();
                if(valueProperty.getType() == type) {
                    return type.cast(valueProperty.getPropertyValue());
                }
            }
            return null;
        }

        protected String getTypeName() {
            if(type == Integer.class) {
                return "int";
            } else if(type == Boolean.class) {
                return "bool";
            } else {
                return type.getSimpleName().toLowerCase();
            }
        }

        protected String getDefaultValue() {
            if(type == Integer.class) {
                return "0";
            } else if(type == Boolean.class) {
                return "false";
            } else {
                return "";
            }
        }

        @SuppressWarnings("unchecked")
        public void setPropertyValue(T value) throws IllegalArgumentException {
            Param param = findParam();
            if(param == null && value != null) {
                CreateNewParam newParam = new CreateNewParam(element, getTypeName(), Theme.this, getDefaultValue()) {
                    @Override
                    protected String makeName() {
                        return paramName;
                    }
                };
                try {
                    newParam.execute(null);
                } catch(IOException ex) {
                    Logger.getLogger(Theme.class.getName()).log(Level.SEVERE, "can't create param", ex);
                }
                param = findParam();
            }
            
            if(param != null) {
                Property<T> valueProperty = (Property<T>)param.getValueProperty();
                if(valueProperty.getType() == type) {
                    if(value == null) {
                        DeleteNodeOperation deleteNodeOperation = new DeleteNodeOperation(param.getDOMElement(), param);
                        try {
                            deleteNodeOperation.execute(null);
                        } catch(IOException ex) {
                            Logger.getLogger(Theme.class.getName()).log(Level.SEVERE, "can't delete param", ex);
                        }
                    } else {
                        valueProperty.setPropertyValue(value);
                    }
                    fireValueChangedCallback();
                }
            }
        }

        protected final Param findParam() {
            for(int i=0 ; i<getNumChildren() ; i++) {
                TreeTableNode child = getChild(i);
                if(child instanceof Param) {
                    Param param = (Param)child;
                    if(paramName.equals(param.getName())) {
                        return param;
                    }
                }
            }
            return null;
        }
    }

    class IntegerParamProperty extends ParamProperty<Integer> implements IntegerModel {
        final int minValue;
        final int maxValue;

        public IntegerParamProperty(String paramName, int minValue, int maxValue) {
            super(paramName, Integer.class);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        public void addCallback(Runnable callback) {
            addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            removeValueChangedCallback(callback);
        }
        public int getValue() {
            Integer value = getPropertyValue();
            return (value == null) ? 0 : value;
        }
        public void setValue(int value) {
            setPropertyValue(value);
        }
        public int getMaxValue() {
            return maxValue;
        }
        public int getMinValue() {
            return minValue;
        }
    }

    class NodeRefParamProperty extends ParamProperty<NodeReference> implements NodeReferenceProperty {
        private final Kind kind;
        private final boolean supportsWildcard;

        public NodeRefParamProperty(String paramName, Kind kind, boolean supportsWildcard) {
            super(paramName, NodeReference.class);
            this.kind = kind;
            this.supportsWildcard = supportsWildcard;
        }

        public Kind getKind() {
            return kind;
        }

        public ThemeTreeNode getLimit() {
            NodeReferenceProperty nrp = findNodeRefProperty();
            if(nrp != null) {
                return nrp.getLimit();
            }
            return null;
        }

        public void handleNodeRenamed(String from, String to, Kind kind) {
        }

        public boolean isSupportsWildcard() {
            return supportsWildcard;
        }

        @Override
        protected String getDefaultValue() {
            return "none";
        }

        @Override
        protected String getTypeName() {
            return kind.name().toLowerCase();
        }

        private NodeReferenceProperty findNodeRefProperty() {
            Param param = findParam();
            if(param != null) {
                Property<?> valueProperty = param.getValueProperty();
                if(valueProperty instanceof NodeReferenceProperty) {
                    return (NodeReferenceProperty)valueProperty;
                }
            }
            return null;
        }
    }
}

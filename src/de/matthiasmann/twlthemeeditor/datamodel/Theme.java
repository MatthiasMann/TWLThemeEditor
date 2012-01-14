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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.AbstractProperty;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParam;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParamFromClass;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewWildcardTheme;
import de.matthiasmann.twlthemeeditor.datamodel.operations.DeleteNodeOperation;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.Undo;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.DerivedNodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.OptionalProperty;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                        new AttributeProperty(element, "allowWildcard", "Allow Wildcard", true), true);
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

    @Override
    protected String getIcon() {
        return "theme";
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

    @Override
    public boolean canPasteElement(Element element) {
        String tag = element.getName();
        return "theme".equals(tag) || "param".equals(tag);
    }

    @Override
    public boolean childrenNeedName() {
        return true;
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
        return operations;
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        ThemeFile.addCreateThemeOperation(operations, this, element);
        operations.add(new CreateNewWildcardTheme(this, element));
        operations.add(new CreateNewParamFromClass(this, element));
        Param.addCreateParam(operations, this, element);
        return operations;
    }

    class ParamProperty<T> extends AbstractProperty<T> implements OptionalProperty<T> {
        final String paramName;
        final Class<T> type;
        final String defaultStr;
        final T defaultValue;
        final L paramCB;
        protected Param param;
        protected Property<T> valueProperty;

        public ParamProperty(String paramName, Class<T> type) {
            this.paramName = paramName;
            this.type = type;
            this.paramCB = new L();
            
            if(type == Integer.class) {
                defaultStr = "0";
                defaultValue = type.cast(Integer.valueOf(0));
            } else if(type == IntegerFormula.class) {
                defaultStr = "0";
                defaultValue = type.cast(new IntegerFormula(0));
            } else if(type == Boolean.class) {
                defaultStr = "false";
                defaultValue = type.cast(Boolean.FALSE);
            } else {
                defaultStr = "";
                defaultValue = null;
            }
        }

        public boolean canBeNull() {
            return false;
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

        public boolean isOptional() {
            return true;
        }

        public boolean isPresent() {
            updateParam();
            return param != null;
        }

        public void setPresent(boolean present) {
            updateParam();
            if(param == null && present) {
                createParam();
            } else if(param != null && !present) {
                deleteOrSetParam(param, null);
            }
        }

        public T getPropertyValue() {
            updateParam();
            if(valueProperty != null && valueProperty.getType() == type) {
                return type.cast(valueProperty.getPropertyValue());
            }
            return defaultValue;
        }

        protected String getTypeName() {
            if(type == Integer.class || type == IntegerFormula.class) {
                return "int";
            } else if(type == Boolean.class) {
                return "bool";
            } else {
                return type.getSimpleName().toLowerCase();
            }
        }

        protected String getDefaultValue() {
            return defaultStr;
        }

        void createParam() {
            CreateNewParam newParam = new CreateNewParam(element, getTypeName(), Theme.this, getDefaultValue()) {
                @Override
                protected String makeName() {
                    return paramName;
                }
            };
            executeOP(newParam);
            updateParam();
        }
        
        void deleteOrSetParam(Param param, T value) {
            if(valueProperty != null && valueProperty.getType() == type) {
                if(value == null) {
                    DeleteNodeOperation deleteNodeOperation = new DeleteNodeOperation(param.getDOMElement(), param);
                    executeOP(deleteNodeOperation);
                    updateParam();
                } else {
                    valueProperty.setPropertyValue(value);
                }
                fireValueChangedCallback();
            }
        }
        
        private void executeOP(ThemeTreeOperation op) {
            Undo.startComplexOperation();
            try {
                op.execute(null);
            } catch(IOException ex) {
                Logger.getLogger(Theme.class.getName()).log(Level.SEVERE, "can't execute operation", ex);
            } finally {
                Undo.endComplexOperation();
            }
        }
        
        public void setPropertyValue(T value) throws IllegalArgumentException {
            updateParam();
            if(param == null && value != null) {
                createParam();
            }
            if(param != null) {
                deleteOrSetParam(param, value);
            }
        }

        @Override
        public void addValueChangedCallback(Runnable cb) {
            boolean hadCallbacks = hasValueChangedCallbacks();
            super.addValueChangedCallback(cb);
            if(!hadCallbacks) {
                updateParam();
                addCB();
            }
        }

        @Override
        public void removeValueChangedCallback(Runnable cb) {
            super.removeValueChangedCallback(cb);
            if(!hasValueChangedCallbacks()) {
                removeCB();
            }
        }

        protected final Param findParam() {
            for(int i=0 ; i<getNumChildren() ; i++) {
                TreeTableNode child = getChild(i);
                if(child instanceof Param) {
                    Param p = (Param)child;
                    if(paramName.equals(p.getName())) {
                        return p;
                    }
                }
            }
            return null;
        }
        
        @SuppressWarnings("unchecked")
        protected final void updateParam() {
            Param newParam = findParam();
            if(param != newParam) {
                removeCB();
                this.param = newParam;
                if(newParam != null) {
                    this.valueProperty = (Property<T>)newParam.getValueProperty();
                } else {
                    this.valueProperty = null;
                }
                addCB();
            }
        }
        
        void removeCB() {
            if(valueProperty != null) {
                valueProperty.removeValueChangedCallback(paramCB);
            }
        }
        
        void addCB() {
            if(valueProperty != null) {
                valueProperty.addValueChangedCallback(paramCB);
            }
        }
        
        void paramChanged() {
            fireValueChangedCallback();
        }
        
        class L implements Runnable {
            public void run() {
                paramChanged();
            }
        }
    }

    class IntegerParamProperty extends ParamProperty<IntegerFormula> implements IntegerModel {
        final int minValue;
        final int maxValue;

        public IntegerParamProperty(String paramName, int minValue, int maxValue) {
            super(paramName, IntegerFormula.class);
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
            IntegerFormula value = getPropertyValue();
            return (value == null) ? 0 : value.getValue();
        }
        public void setValue(int value) {
            setPropertyValue(new IntegerFormula(value));
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
            updateParam();
            if(valueProperty instanceof NodeReferenceProperty) {
                return (NodeReferenceProperty)valueProperty;
            }
            return null;
        }
    }
}

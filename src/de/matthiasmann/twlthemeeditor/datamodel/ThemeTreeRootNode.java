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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreeRootNode extends ThemeTreeNode {

    public ThemeTreeRootNode(ThemeFile themeFile, TreeTableNode parent) {
        super(themeFile, parent, themeFile.getRootElement());
    }

    @Override
    public String getName() {
        String name = themeFile.getVirtualFileName();
        int idx = name.lastIndexOf('/');
        return name.substring(idx+1);
    }

    public void addChildren() throws IOException {
        themeFile.addChildren(this);
    }

    public void addToXPP(DomXPPParser xpp) {
        throw new IllegalStateException("Should not reach here");
    }

    public Kind getKind() {
        return Kind.NONE;
    }

    @Override
    protected String getIcon() {
        return "themes";
    }

    @Override
    protected boolean isModified() {
        return themeFile.isModified();
    }

    @Override
    public boolean canPasteElement(Element element) {
        String tag = element.getName();
        return "theme".equals(tag);
    }

    @Override
    public boolean childrenNeedName() {
        return true;
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        return Collections.<ThemeTreeOperation>emptyList();
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        themeFile.addCreateOperations(operations, this);
        return operations;
    }
}

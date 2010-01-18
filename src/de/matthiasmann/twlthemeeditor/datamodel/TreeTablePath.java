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

/**
 *
 * @author Matthias Mann
 */
public class TreeTablePath {

    final TreeTableNode node;
    final TreeTablePath next;

    private TreeTablePath(TreeTableNode node, TreeTablePath next) {
        this.node = node;
        this.next = next;
    }

    static TreeTablePath create(TreeTableNode node) {
        TreeTablePath path = null;
        while(node != null) {
            path = new TreeTablePath(node, path);
            node = node.getParent();
        }
        return path;
    }

    public int compareTo(TreeTablePath path2) {
        TreeTablePath path1 = this;
        while(path1 != null && path2 != null && path1.node == path2.node) {
            path1 = path1.next;
            path2 = path2.next;
        }
        if(path1 == null) {
            return (path2 == null) ? 0 : -1;
        }
        if(path2 == null) {
            return 1;
        }
        return path1.getIndexInParent() - path2.getIndexInParent();
    }

    public int compareTo(TreeTableNode node) {
        return compareTo(TreeTablePath.create(node));
    }

    private int getIndexInParent() {
        TreeTableNode parent = node.getParent();
        return (parent != null) ? parent.getChildIndex(node) : 0;
    }
}

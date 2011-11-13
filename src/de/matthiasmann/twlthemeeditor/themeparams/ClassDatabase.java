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
package de.matthiasmann.twlthemeeditor.themeparams;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Matthias Mann
 */
public class ClassDatabase implements Serializable {
    
    private static final long serialVersionUID = 1;
    
    private final transient ClassDatabase parent;
    private final HashMap<String, ClassThemeParamInfo> classes;

    private transient String displayName;
    
    public ClassDatabase(ClassDatabase parent) {
        this.parent = parent;
        this.classes = new HashMap<String, ClassThemeParamInfo>();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void add(ClassThemeParamInfo classInfo) {
        classes.put(classInfo.getClassName(), classInfo);
    }
    
    public Collection<ClassThemeParamInfo> getClassInfos() {
        return classes.values();
    }

    public Set<String> getClassNames() {
        return classes.keySet();
    }

    public int size() {
        return classes.size();
    }

    public ClassThemeParamInfo getClassInfo(String name) {
        ClassThemeParamInfo classInfo = classes.get(name);
        if(classInfo == null && parent != null) {
            classInfo = parent.getClassInfo(name);
        }
        return classInfo;
    }
    
    public ThemeParamInfo[] collectParametersForClass(String name) {
        ArrayList<ThemeParamInfo> info = new ArrayList<ThemeParamInfo>();
        while(name != null) {
            ClassThemeParamInfo classInfo = getClassInfo(name);
            if(classInfo == null)  {
                break;
            }
            info.addAll(classInfo.params);
            name = classInfo.superClassName;
        }
        ThemeParamInfo params[] = info.toArray(new ThemeParamInfo[info.size()]);
        Arrays.sort(params, new Comparator<ThemeParamInfo>() {
            public int compare(ThemeParamInfo o1, ThemeParamInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return params;
    }
}

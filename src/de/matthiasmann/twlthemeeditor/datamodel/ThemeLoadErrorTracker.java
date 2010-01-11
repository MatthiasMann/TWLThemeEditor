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

import java.util.ArrayList;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * @author Matthias Mann
 */
public class ThemeLoadErrorTracker {

    private static final ThreadLocal<Stack<ThemeLoadErrorTracker>> tls = new ThreadLocal<Stack<ThemeLoadErrorTracker>>() {
        @Override
        protected Stack<ThemeLoadErrorTracker> initialValue() {
            return new Stack<ThemeLoadErrorTracker>();
        }
    };

    private final ArrayList<DomXPPParser> parsers;

    public ThemeLoadErrorTracker() {
        this.parsers = new ArrayList<DomXPPParser>();
    }

    public static void push(ThemeLoadErrorTracker tracker) {
        tls.get().add(tracker);
    }

    public static ThemeLoadErrorTracker pop() {
        return tls.get().pop();
    }

    public static void register(DomXPPParser parser) {
        final Stack<ThemeLoadErrorTracker> stack = tls.get();
        if(!stack.isEmpty()) {
            stack.peek().parsers.add(parser);
        }
    }

    public Object findErrorLocation() {
        // search from newest parser to oldest
        for(int i=parsers.size() ; i-->0 ;) {
            DomXPPParser parser = parsers.get(i);
            if(parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                parsers.remove(i);
            } else {
                Object location = parser.getLocation();
                if(location != null) {
                    return location;
                }
            }
        }
        return null;
    }

}

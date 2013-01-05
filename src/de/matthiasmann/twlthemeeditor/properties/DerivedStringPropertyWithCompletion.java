/*
 * Copyright (c) 2008-2013, Matthias Mann
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

import de.matthiasmann.twl.model.AutoCompletionDataSource;
import de.matthiasmann.twl.model.AutoCompletionResult;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleAutoCompletionResult;
import de.matthiasmann.twl.utils.TextUtil;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public class DerivedStringPropertyWithCompletion extends DerivedProperty<String> implements AutoCompletionDataSource {

    private final String[] autoCompletionSuggestions;
    
    public DerivedStringPropertyWithCompletion(Property<String> base, String defaultValue, String ... autoCompletionSuggestions) {
        super(base, String.class, defaultValue);
        this.autoCompletionSuggestions = autoCompletionSuggestions;
    }
    
    @Override
    protected String parse(String value) throws IllegalArgumentException {
        return TextUtil.notNull(value);
    }
    
    @Override
    protected String toString(String value) throws IllegalArgumentException {
        if(value.length() == 0) {
            return null;
        }
        return value;
    }

    public AutoCompletionResult collectSuggestions(String text, int cursorPos, AutoCompletionResult prev) {
        String part = text.substring(0, cursorPos);
        ArrayList<String> results = new ArrayList<String>();
        for(String suggestion : autoCompletionSuggestions) {
            if(suggestion.startsWith(part)) {
                results.add(suggestion);
            }
        }
        return new SimpleAutoCompletionResult(text, cursorPos, results);
    }
    
}

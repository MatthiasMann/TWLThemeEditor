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
package de.matthiasmann.twlthemeeditor.dom;

/**
 *
 * @author Matthias Mann
 */
public final class ElementText extends PCS {
    
    final Element element;
    String oldText;
    boolean disableEvent;

    ElementText(Element element) {
        this.element = element;
        this.oldText = computeText();
    }

    public Element getElement() {
        return element;
    }

    public String getText() {
        return oldText;
    }
    
    public void setText(String text) {
        if(!oldText.equals(text)) {
            final boolean oldDisableEvent = disableEvent();
            try {
                element.doSetText(text);
            } finally {
                disableEvent = oldDisableEvent;
            }
            textChanged();
        }
    }
    
    String computeText() {
        ContentList content = element.content;
        String result = "";
        if(content != null) {
            for(int i=0,n=content.size ; i<n ; i++) {
                Content c = content.data[i];
                if(c instanceof Text) {
                    String textValue = ((Text)c).value;
                    if(textValue.length() > 0) {
                        if(result.length() > 0) {
                            return computeText(result, textValue, i);
                        }
                        result = textValue;
                    }
                }
            }
        }
        return result;
    }
    
    String computeText(String part1, String part2, int i) {
        ContentList content = element.content;
        StringBuilder sb = new StringBuilder(part1.length() + part2.length() + 16)
                .append(part1).append(part2);
        
        for(int n=content.size ; ++i<n ;) {
            Content c = content.data[i];
            if(c instanceof Text) {
                sb.append(((Text)c).value);
            }
        }
        return sb.toString();
    }
    
    void textChanged() {
        if(!disableEvent) {
            String newText = computeText();
            if(!oldText.equals(newText)) {
                this.oldText = newText;
                firePropertyChange("text", oldText, newText);
            }
        }
    }
    
    boolean disableEvent() {
        boolean oldValue = disableEvent;
        disableEvent = true;
        return oldValue;
    }
}

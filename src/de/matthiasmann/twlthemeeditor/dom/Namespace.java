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
public final class Namespace {
    
    public static final Namespace NO_NAMESPACE = new Namespace("", "");
    
    final String prefix;
    final String uri;
    final int hash;
    
    Namespace next;

    public Namespace(String prefix, String uri) {
        if(prefix == null) {
            throw new NullPointerException("prefix");
        }
        if(uri == null) {
            throw new NullPointerException("uri");
        }
        this.prefix = prefix;
        this.uri = uri;
        this.hash = hashCode(prefix, uri);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(!(obj instanceof Namespace)) {
            return false;
        }
        final Namespace other = (Namespace)obj;
        return this.hash == other.hash &&
                this.prefix.equals(other.prefix) &&
                this.uri.equals(other.uri);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        if(uri.length() > 0) {
            return prefix+"="+uri;
        }
        return "";
    }
    
    static int hashCode(String prefix, String uri) {
        return prefix.hashCode() * 97 + uri.hashCode();
    }
    
    StringBuilder appendUri(StringBuilder sb) {
        if(uri.length() > 0) {
            sb.append(uri).append(':');
        }
        return sb;
    }
    
    String xmlNS() {
        if(uri.length() > 0) {
            return uri;
        }
        return null;
    }
}

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.xwiki.url;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Base XWikiURL implementation common to all extending classes. Manages XWiki URL parameters.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class AbstractXWikiURL implements XWikiURL
{
    /**
     * @see #getType()
     */
    private XWikiURLType type;
    
    /**
     * @see #getParameters()
     */
    private Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();

    public AbstractXWikiURL(XWikiURLType type)
    {
        setType(type);
    }
    
    /**
     * @return the type of URL (Entity URL, Attachment URL, Template URL, etc) 
     */
    public XWikiURLType getType()
    {
        return this.type;
    }
    
    public void setType(XWikiURLType type)
    {
        this.type = type;
    }

    public void addParameter(String name, String value)
    {
        List<String> list = this.parameters.get(name);
        if (list == null) {
            list = new ArrayList<String>();
        }
        if (value != null) {
            list.add(value);
        }
        this.parameters.put(name, list);
    }

    /**
     * A XWiki URL parameter provides optional additional information about the URL. 
     * For example these will find their way into the Query String when the XWiki URL serialized to a standard URL.
     * Note that there can be several values for the same name (since this is allowed in URLs and we want to map a 
     * URL to a XWiki URL). Also note that the order in the map is the same as the order in the URL.
     * 
     * @return the XWiki URL parameters
     */
    public Map<String, List<String>> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    public List<String> getParameterValues(String name)
    {
        return this.parameters.get(name);
    }
    
    /**
     * @param name the parameter name for which to return the value
     * @return the first parameter value matching the passed parameter name
     */
    public String getParameterValue(String name)
    {
        String result = null;
        List<String> list = this.parameters.get(name);
        if (list != null) {
            result = list.get(0);
        }
        return result;
    }
}
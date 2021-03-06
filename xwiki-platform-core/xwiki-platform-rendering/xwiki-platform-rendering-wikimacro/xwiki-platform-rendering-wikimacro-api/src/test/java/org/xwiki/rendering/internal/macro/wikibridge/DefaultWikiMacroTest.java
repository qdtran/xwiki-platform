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
 */
package org.xwiki.rendering.internal.macro.wikibridge;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptContext;

import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroFactory;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

/**
 * Unit tests for {@link DefaultWikiMacro}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultWikiMacroTest extends AbstractComponentTestCase
{
    /**
     * Dummy document reference of the document which contains the wiki macro.
     */
    private DocumentReference wikiMacroDocumentReference;

    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroManager} component.
     */
    private WikiMacroManager wikiMacroManager;

    private WikiModel mockWikiModel;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        // Script setup.
        ScriptMockSetup scriptMockSetup = new ScriptMockSetup(getMockery(), getComponentManager());
        final DocumentAccessBridge mockDocBridge = scriptMockSetup.bridge;
        this.mockWikiModel = scriptMockSetup.wikiModel;

        this.wikiMacroDocumentReference = new DocumentReference("wiki", "space", "macroPage");
        this.wikiMacroManager = getComponentManager().getInstance(WikiMacroManager.class);

        // Make sure the old XWiki Context is set up in the Execution Context since it's used in
        // DefaultWikiMacro.execute().
        Map<String, Object> xcontext = new HashMap<String, Object>();
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.getContext().setProperty("xwikicontext", xcontext);
        ScriptContextManager scm = getComponentManager().getInstance(ScriptContextManager.class);
        scm.getScriptContext().setAttribute("xcontext", xcontext, ScriptContext.ENGINE_SCOPE);

        getMockery().checking(new Expectations() {{
            allowing(mockDocBridge).getCurrentWiki(); will(returnValue("wiki"));
            allowing(mockDocBridge).getCurrentUser(); will(returnValue("dummy"));

            // This is the document containing the wiki macro that will be put in the context available in the macro
            // Since we're not testing it here, it can be null.
            allowing(mockDocBridge).getDocument(wikiMacroDocumentReference); will(returnValue(null));
        }});
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        // some tests fail because the lookup of this component fails (the implementation is defined in xwiki-core)
        registerMockComponent(WikiMacroFactory.class);
    }

    /**
     * Test normal wiki macro execution.
     */
    @Test
    public void testExecute() throws Exception
    {
        registerWikiMacro("wikimacro1", "This is **bold**", Syntax.XWIKI_2_0);

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1 param1=\"value1\" param2=\"value2\"/}}"), Syntax.XWIKI_2_0,
            Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>This is <strong>bold</strong></p>", printer.toString());
    }

    /**
     * When a wiki macro is used in inline mode and its code starts with a macro, that nested macro is made inline. In
     * other words, the nested macro should not generate extra paragraph elements.
     */
    @Test
    public void testExecuteWhenInlineAndWithMacro() throws Exception
    {
        registerWikiMacro("wikimacro1", "This is **bold**", Syntax.XWIKI_2_0);
        registerWikiMacro("wikimacro2", "{{wikimacro1 param1=\"v1\" param2=\"v2\"/}}", Syntax.XWIKI_2_0);

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        // Note: We're putting the macro after the "Hello" text to force it as an inline macro.
        converter.convert(new StringReader("Hello {{wikimacro2 param1=\"value1\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>Hello This is <strong>bold</strong></p>", printer.toString());
    }

    /**
     * Check that macro used inside wiki macro are executed as part of the document.
     */
    @Test
    public void testExecuteWhenInnerMacro() throws Exception
    {
        registerWikiMacro("wikimacro1", "{{toc/}}", Syntax.XWIKI_2_0);

        getMockery().checking(new Expectations() {{
            DocumentResourceReference reference = new DocumentResourceReference(null);
            reference.setAnchor("Hheading");
            allowing(mockWikiModel).getDocumentViewURL(reference);
            will(returnValue("url"));
        }});

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("= heading\n\n{{wikimacro1 param1=\"value1\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert
            .assertEquals("<h1 id=\"Hheading\"><span>heading</span></h1>"
                + "<ul><li><span class=\"wikilink\"><a href=\"#Hheading\">heading</a></span></li></ul>",
                printer.toString());
    }

    /**
     * Check that macro used inside wiki macro are executed with the right syntax.
     */
    @Test
    public void testExecuteWhenWithDifferentMacroSyntax() throws Exception
    {
        registerWikiMacro("wikimacro", "{{groovy}}println \"[[path:/some/path]]\"{{/groovy}}", Syntax.XWIKI_2_1);

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro param1=\"value1\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert
            .assertEquals("<p><span class=\"wikiexternallink\"><a href=\"/some/path\">" +
            		"<span class=\"wikigeneratedlinkcontent\">/some/path</span></a></span></p>",
                printer.toString());
    }
    
    /**
     * A wiki macro can directly provide the list of blocks instead of having to render them to let
     * {@link DefaultWikiMacro} re-parse it.
     */
    @Test
    public void testExecuteWhenWikiMacroDirectlyProvideTheResult() throws Exception
    {
        registerWikiMacro(
            "wikimacrowithresult",
            "{{groovy}}"
                + "xcontext.macro.result = java.util.Collections.singletonList(new org.xwiki.rendering.block.WordBlock(xcontext.macro.params.param1));"
                + "{{/groovy}}", Syntax.XWIKI_2_0);

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        // Note: We're putting the macro after the "Hello" text to force it as an inline macro.
        converter.convert(new StringReader("Hello {{wikimacrowithresult param1=\"World\" param2=\"param2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>Hello World</p>", printer.toString());
    }

    /**
     * Test default parameter value injection.
     */
    @Test
    public void testDefaultParameterValues() throws Exception
    {
        // Velocity Manager mock.
        final VelocityManager mockVelocityManager = getMockery().mock(VelocityManager.class);
        DefaultComponentDescriptor<VelocityManager> descriptorVM = new DefaultComponentDescriptor<VelocityManager>();
        descriptorVM.setRole(VelocityManager.class);
        getComponentManager().registerComponent(descriptorVM, mockVelocityManager);

        // Initialize velocity engine.
        final VelocityEngine vEngine = getComponentManager().getInstance(VelocityEngine.class);
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        vEngine.initialize(properties);

        // Hack into velocity context.
        Execution execution = getComponentManager().getInstance(Execution.class);
        Map< ? , ? > xwikiContext = (Map< ? , ? >) execution.getContext().getProperty("xwikicontext");
        final VelocityContext vContext = new VelocityContext();
        vContext.put("xcontext", xwikiContext);

        getMockery().checking(new Expectations() {{
            oneOf(mockVelocityManager).getVelocityContext();
            will(returnValue(vContext));
            oneOf(mockVelocityManager).getVelocityEngine();
            will(returnValue(vEngine));
        }});

        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", false, "default_value"));

        registerWikiMacro("wikimacro1", "{{velocity}}$xcontext.macro.params.param1{{/velocity}}", Syntax.XWIKI_2_0, parameterDescriptors);

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1/}}"), Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>default_value</p>", printer.toString());
    }

    private void registerWikiMacro(String macroId, String macroContent, Syntax syntax) throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", true),
                new WikiMacroParameterDescriptor("param2", "This is param2", true));
        registerWikiMacro(macroId, macroContent, syntax, parameterDescriptors);
    }

    private void registerWikiMacro(String macroId, String macroContent, Syntax syntax,
        List<WikiMacroParameterDescriptor> parameterDescriptors) throws Exception
    {
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor(new MacroId(macroId), "Wiki Macro", "Description", "Test",
                WikiMacroVisibility.GLOBAL, new DefaultContentDescriptor(false), parameterDescriptors);

        Parser parser = getComponentManager().getInstance(Parser.class, syntax.toIdString());
        
        DefaultWikiMacro wikiMacro =
            new DefaultWikiMacro(wikiMacroDocumentReference, true, descriptor,
                parser.parse(new StringReader(macroContent)), syntax, getComponentManager());

        this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, wikiMacro);
    }
}

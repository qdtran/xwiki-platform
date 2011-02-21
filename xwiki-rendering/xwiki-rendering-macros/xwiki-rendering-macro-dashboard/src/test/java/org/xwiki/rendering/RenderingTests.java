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
package org.xwiki.rendering;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetSource;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 3.0M3
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Dashboard Macro");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }

    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery mockery = new Mockery();

        // Since we have a dependency on xwiki-core the Context Component Manager will be found and the test will try
        // to look up the Dashboard macro in the User and Wiki Component Manager and thus need a Current User and a
        // Current Wiki. It's easier for this test to simply unregister the Context Component Manager rather than
        // have to provide mocks for them.
        componentManager.unregisterComponent(ComponentManager.class, "context");

        final SkinExtension mockSsfx = mockery.mock(SkinExtension.class, "ssfxMock");
        final SkinExtension mockJsfx = mockery.mock(SkinExtension.class, "jsfxMock");
        mockery.checking(new Expectations()
        {
            {
                allowing(mockSsfx).use(with("uicomponents/container/columns.css"), with(any(Map.class)));
                allowing(mockSsfx).use(with("uicomponents/dashboard/dashboard.css"), with(any(Map.class)));
                allowing(mockJsfx).use(with("js/scriptaculous/dragdrop.js"));
                allowing(mockJsfx).use(with("js/scriptaculous/effects.js"));
                allowing(mockJsfx).use(with("js/xwiki/wysiwyg/xwe/XWikiWysiwyg.js"), with(any(Map.class)));
                allowing(mockJsfx).use(with("uicomponents/dashboard/dashboard.js"), with(any(Map.class)));
            }
        });
        DefaultComponentDescriptor<SkinExtension> ssfxDesc = new DefaultComponentDescriptor<SkinExtension>();
        ssfxDesc.setRole(SkinExtension.class);
        ssfxDesc.setRoleHint("ssfx");
        componentManager.registerComponent(ssfxDesc, mockSsfx);
        DefaultComponentDescriptor<SkinExtension> jsfxDesc = new DefaultComponentDescriptor<SkinExtension>();
        jsfxDesc.setRole(SkinExtension.class);
        jsfxDesc.setRoleHint("jsfx");
        componentManager.registerComponent(jsfxDesc, mockJsfx);

        final GadgetSource mockGadgetSource = mockery.mock(GadgetSource.class);
        mockery.checking(new Expectations()
        {
            {
                allowing(mockGadgetSource).getGadgets(with(any(String.class)),
                    with(any(MacroTransformationContext.class)));
                will(returnValue(Arrays.asList(new Gadget("0", Arrays.<Block> asList(new WordBlock("title")), Arrays
                    .<Block> asList(new WordBlock("content")), "1,1"))));
                allowing(mockGadgetSource).getDashboardSourceMetadata(with(any(String.class)),
                    with(any(MacroTransformationContext.class)));
                will(returnValue(Collections.<Block> emptyList()));
                allowing(mockGadgetSource).isEditing();
                // return true on is editing, to take as many paths possible
                will(returnValue(true));
            }
        });
        DefaultComponentDescriptor<GadgetSource> descriptorGR = new DefaultComponentDescriptor<GadgetSource>();
        descriptorGR.setRole(GadgetSource.class);
        componentManager.registerComponent(descriptorGR, mockGadgetSource);
    }
}

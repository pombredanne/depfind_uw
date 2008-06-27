/*
 *  Copyright (c) 2001-2008, Jean Tessier
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *
 *      * Neither the name of Jean Tessier nor the names of his contributors
 *        may be used to endorse or promote products derived from this software
 *        without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jeantessier.dependency;

import java.util.*;

import junit.framework.*;

public class TestLCOM4Gatherer extends TestCase {
    private LCOM4Gatherer sut;
    private NodeFactory factory;

    protected void setUp() throws Exception {
        super.setUp();

        factory = new NodeFactory();
        sut = new LCOM4Gatherer();
    }

    public void testNothing() {
        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertTrue(actualResults.isEmpty());
    }

    public void testEmptyPackage() {
        factory.createPackage("");

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertTrue(actualResults.isEmpty());
    }

    public void testEmptyClass() {
        ClassNode classNode = factory.createClass("Empty");

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(classNode));

        Collection<Collection<FeatureNode>> components = actualResults.get(classNode);
        assertEquals("LCOM4 of empty class", 0, components.size());
    }

    /**
     * Tests One.one, where class One will have an LCOM4 of 1 and
     * the component will be [One.one].
     */
    public void testOneFeature() {
        FeatureNode featureNode = factory.createFeature("One.one");

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode.getClassNode());
        assertEquals("LCOM4 of class w/ one feature", 1, components.size());

        Collection<FeatureNode> component = components.iterator().next();
        assertEquals("Size of first component", 1, component.size());
        assertTrue(component.contains(featureNode));
    }

    /**
     * Tests One.one and One.two, where class One will have an LCOM4 of 2 and
     * the components will be [One.one] and [One.two].
     */
    public void testTwoDisjointFeatures() {
        FeatureNode featureNode1 = factory.createFeature("Two.one");
        FeatureNode featureNode2 = factory.createFeature("Two.two");

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode1.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode1.getClassNode());
        assertEquals("LCOM4 of class w/ two connected features", 2, components.size());

        assertAtLeastOneComponentEquals(components, featureNode1);
        assertAtLeastOneComponentEquals(components, featureNode2);
    }

    /**
     * Tests One.one --> One.two, where class One will have an LCOM4 of 1 and
     * the component will be [One.one, One.two].
     */
    public void testTwoConnectedFeatures() {
        FeatureNode featureNode1 = factory.createFeature("Two.one");
        FeatureNode featureNode2 = factory.createFeature("Two.two");

        featureNode1.addDependency(featureNode2);

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode1.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode1.getClassNode());
        assertEquals("LCOM4 of class w/ two connected features", 1, components.size());

        assertAtLeastOneComponentEquals(components, featureNode1, featureNode2);
    }

    /**
     * Tests One.one --> One.two --> One.three, where class One will have an
     * LCOM4 of 1 and the component will be [One.one, One.two, One.three].
     */
    public void testThreeConnectedFeatures() {
        FeatureNode featureNode1 = factory.createFeature("Three.one");
        FeatureNode featureNode2 = factory.createFeature("Three.two");
        FeatureNode featureNode3 = factory.createFeature("Three.three");

        featureNode1.addDependency(featureNode2);
        featureNode2.addDependency(featureNode3);

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode1.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode1.getClassNode());
        assertEquals("LCOM4 of class w/ three connected features", 1, components.size());

        assertAtLeastOneComponentEquals(components, featureNode1, featureNode2, featureNode3);
    }

    /**
     * Tests One.one --> Two.two, where class One will have an LCOM4 of 1 and
     * the component will be [One.one].  Two.two is not included because it is
     * in another class.
     */
    public void testTwoConnectedFeaturesInSeparateClasses() {
        FeatureNode featureNode1 = factory.createFeature("One.one");
        FeatureNode featureNode2 = factory.createFeature("Two.two");

        featureNode1.addDependency(featureNode2);

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(2, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode1.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode1.getClassNode());
        assertEquals("LCOM4 of class w/ feature connected to other class", 1, components.size());

        assertAtLeastOneComponentEquals(components, featureNode1);
    }

    /**
     * Tests One.one --> Two.two --> One.two, where class One will have an
     * LCOM4 of 2 and the components will be [One.one] and [One.two].  There
     * are two components because the link between the methods is outside
     * class One.
     */
    public void testTwoIndirectlyConnectedFeatures() {
        FeatureNode featureNode1 = factory.createFeature("One.one");
        FeatureNode featureNode2 = factory.createFeature("Two.two");
        FeatureNode featureNode3 = factory.createFeature("One.two");

        featureNode1.addDependency(featureNode2);
        featureNode2.addDependency(featureNode3);

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(2, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(featureNode1.getClassNode()));

        Collection<Collection<FeatureNode>> components = actualResults.get(featureNode1.getClassNode());
        assertEquals("LCOM4 of class w/ features connected through other class", 2, components.size());

        assertAtLeastOneComponentEquals(components, featureNode1);
        assertAtLeastOneComponentEquals(components, featureNode3);
    }

    public void testIgnoreConstructor() {
        ClassNode classNode = factory.createClass("One");
        factory.createFeature("One.One()");

        sut.traverseNodes(factory.getPackages().values());

        Map<ClassNode, Collection<Collection<FeatureNode>>> actualResults = sut.getResults();
        assertEquals(1, actualResults.keySet().size());
        assertTrue(actualResults.containsKey(classNode));

        Collection<Collection<FeatureNode>> components = actualResults.get(classNode);
        assertEquals("LCOM4 of class w/ only constructors", 0, components.size());
    }

    private void assertAtLeastOneComponentEquals(Collection<Collection<FeatureNode>> components, FeatureNode ... expectedNodes) {
        boolean found = false;

        for (Collection<FeatureNode> component : components) {
            found = found || checkComponentEquals(component, expectedNodes);
        }

        assertTrue(Arrays.asList(expectedNodes) + " not in " + components, found);
    }

    private boolean checkComponentEquals(Collection<FeatureNode> component, FeatureNode ... expectedNodes) {
        boolean result = expectedNodes.length == component.size();

        for (FeatureNode expectedNode : expectedNodes) {
            result = result && component.contains(expectedNode);
        }

        return result;
    }
}

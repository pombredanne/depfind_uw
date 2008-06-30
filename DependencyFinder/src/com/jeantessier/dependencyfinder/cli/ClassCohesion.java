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

package com.jeantessier.dependencyfinder.cli;

import java.util.*;

import org.apache.log4j.*;

import com.jeantessier.commandline.*;
import com.jeantessier.dependency.*;

public class ClassCohesion extends DependencyGraphCommand {
    private static final String DEFAULT_ENCODING = "utf-8";
    private static final String DEFAULT_DTD_PREFIX = "http://depfind.sourceforge.net/dtd";
    private static final String DEFAULT_INDENT_TEXT = "    ";

    protected void populateCommandLineSwitches()  {
        super.populateCommandLineSwitches();
        populateCommandLineSwitchesForXMLOutput(DEFAULT_ENCODING, DEFAULT_DTD_PREFIX, DEFAULT_INDENT_TEXT);

        getCommandLine().addToggleSwitch("csv");
        getCommandLine().addToggleSwitch("txt");
        getCommandLine().addToggleSwitch("xml");
        getCommandLine().addToggleSwitch("list");
    }

    protected ParameterStrategy getParameterStrategy() {
        return new AtLeastParameterStrategy(1);
    }

    protected Collection<CommandLineException> parseCommandLine(String[] args) {
        Collection<CommandLineException> exceptions = super.parseCommandLine(args);
        int modeSwitch = 0;

        if (getCommandLine().getToggleSwitch("csv")) {
            modeSwitch++;
        }
        if (getCommandLine().getToggleSwitch("txt")) {
            modeSwitch++;
        }
        if (getCommandLine().getToggleSwitch("xml")) {
            modeSwitch++;
        }
        if (modeSwitch != 1) {
            exceptions.add(new CommandLineException("Must have one and only one of -csv, -txt, or -xml"));
        }

        return exceptions;
    }

    public void doProcessing() throws Exception {
        LCOM4Gatherer gatherer = new LCOM4Gatherer();

        Logger.getLogger(OOMetrics.class).debug("Reading classes and computing metrics as we go ...");
        getVerboseListener().print("Reading classes and computing metrics as we go ...");
        gatherer.traverseNodes(loadGraphs().getPackages().values());

        Logger.getLogger(OOMetrics.class).debug("Printing results ...");
        getVerboseListener().print("Printing results ...");

        if (getCommandLine().isPresent("csv")) {
            printCSVFiles(gatherer.getResults());
        } else if (getCommandLine().isPresent("txt")) {
            printTextFile(gatherer.getResults());
        } else if (getCommandLine().isPresent("xml")) {
            printXMLFile(gatherer.getResults());
        }

        Logger.getLogger(OOMetrics.class).debug("Done.");
    }

    private void printCSVFiles(Map<ClassNode, Collection<Collection<FeatureNode>>> results) {
        out.println("class, LCOM4");
        for (Map.Entry<ClassNode, Collection<Collection<FeatureNode>>> entry : results.entrySet()) {
            out.println(entry.getKey().getName() + ", " + entry.getValue().size());
        }
    }

    private void printTextFile(Map<ClassNode, Collection<Collection<FeatureNode>>> results) {
        String indentText = getCommandLine().getSingleSwitch("indent-text");

        for (Map.Entry<ClassNode, Collection<Collection<FeatureNode>>> entry : results.entrySet()) {
            out.println(entry.getKey().getName() + ": " + entry.getValue().size());
            if (entry.getValue().size() > 1 && getCommandLine().isPresent("list")) {
                out.println(indentText + "--------");
                for (Collection<FeatureNode> component : entry.getValue()) {
                    for (FeatureNode feature : component) {
                        out.println(indentText + feature.getName());
                    }
                    out.println(indentText + "--------");
                }
            }
        }
    }

    private void printXMLFile(Map<ClassNode, Collection<Collection<FeatureNode>>> results) {
        String indentText = getCommandLine().getSingleSwitch("indent-text");

        out.println("<classes>");
        for (Map.Entry<ClassNode, Collection<Collection<FeatureNode>>> entry : results.entrySet()) {
            if (entry.getValue().size() > 1) {
                out.println(indentText + "<class name=\"" + entry.getKey().getName() + "\" lcom4=\"" + entry.getValue().size() + "\">");
                for (Collection<FeatureNode> component : entry.getValue()) {
                    out.println(indentText + indentText + "<component>");
                    for (FeatureNode feature : component) {
                        out.println(indentText + indentText + indentText + "<feature name=\"" + feature.getName() + "\"/>");
                    }
                    out.println(indentText + indentText + "</component>");
                }
                out.println(indentText + "</class>");
            } else {
                out.println(indentText + "<class name=\"" + entry.getKey().getName() + "\" lcom4=\"" + entry.getValue().size() + "\"/>");                
            }
        }
        out.println("</classes>");
    }

    public static void main(String[] args) throws Exception {
        new ClassCohesion().run(args);
    }
}
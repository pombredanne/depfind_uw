/*
 *  Copyright (c) 2001-2003, Jean Tessier
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  
 *  	* Redistributions of source code must retain the above copyright
 *  	  notice, this list of conditions and the following disclaimer.
 *  
 *  	* Redistributions in binary form must reproduce the above copyright
 *  	  notice, this list of conditions and the following disclaimer in the
 *  	  documentation and/or other materials provided with the distribution.
 *  
 *  	* Neither the name of the Jean Tessier nor the names of his contributors
 *  	  may be used to endorse or promote products derived from this software
 *  	  without specific prior written permission.
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

import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.jeantessier.classreader.*;
import com.jeantessier.commandline.*;
import com.jeantessier.metrics.*;

public class OOMetrics {
	public static final String DEFAULT_PROJECT_NAME = "Project";
	public static final String DEFAULT_SORT         = "name";
	public static final String DEFAULT_DISPOSE      = "median";
	public static final String DEFAULT_LOGFILE      = "System.out";

	public static void Error(CommandLineUsage clu, String msg) {
		System.err.println(msg);
		Error(clu);
	}

	public static void Error(CommandLineUsage clu) {
		System.err.println(clu);
		System.err.println();
		System.err.println("If no files are specified, it processes the current directory.");
		System.err.println();
		System.err.println("If file is a directory, it is recusively scanned for files");
		System.err.println("ending in \".class\".");
		System.err.println();
		System.err.println("Defaults is text output to the console.");
		System.err.println();
	}

	public static void main(String[] args) throws Exception {
		// Parsing the command line
		CommandLine command_line = new CommandLine();
		command_line.AddSingleValueSwitch("project-name", DEFAULT_PROJECT_NAME);
		command_line.AddSingleValueSwitch("default-configuration", true);
		command_line.AddSingleValueSwitch("configuration");
		command_line.AddToggleSwitch("csv");
		command_line.AddToggleSwitch("txt");
		command_line.AddToggleSwitch("xml");
		command_line.AddToggleSwitch("validate");
		command_line.AddSingleValueSwitch("dtd-prefix",   com.jeantessier.metrics.XMLPrinter.DEFAULT_DTD_PREFIX);
		command_line.AddSingleValueSwitch("indent-text");
		command_line.AddToggleSwitch("all");
		command_line.AddToggleSwitch("project");
		command_line.AddToggleSwitch("groups");
		command_line.AddToggleSwitch("classes");
		command_line.AddToggleSwitch("methods");
		command_line.AddSingleValueSwitch("sort",         DEFAULT_SORT);
		command_line.AddSingleValueSwitch("dispose",      DEFAULT_DISPOSE);
		command_line.AddToggleSwitch("expand");
		command_line.AddToggleSwitch("reverse");
		command_line.AddToggleSwitch("time");
		command_line.AddSingleValueSwitch("out");
		command_line.AddToggleSwitch("help");
		command_line.AddOptionalValueSwitch("verbose",    DEFAULT_LOGFILE);

		CommandLineUsage usage = new CommandLineUsage("OOMetrics");
		command_line.Accept(usage);

		try {
			command_line.Parse(args);
		} catch (IllegalArgumentException ex) {
			Error(usage, ex.toString());
			System.exit(1);
		} catch (CommandLineException ex) {
			Error(usage, ex.toString());
			System.exit(1);
		}

		if (command_line.ToggleSwitch("help")) {
			Error(usage);
			System.exit(1);
		}

		if (!command_line.ToggleSwitch("all") && !command_line.ToggleSwitch("project") && !command_line.ToggleSwitch("groups") && !command_line.ToggleSwitch("classes") && !command_line.ToggleSwitch("methods")) {
			Error(usage, "Must have at least one of -all, -project, -groups, -classes, or -methods");
			System.exit(1);
		}

		int mode_switch = 0;
		
		if (command_line.ToggleSwitch("csv")) {
			mode_switch++;
		}
		if (command_line.ToggleSwitch("txt")) {
			mode_switch++;
		}
		if (command_line.ToggleSwitch("xml")) {
			mode_switch++;
		}
		if (mode_switch != 1) {
			Error(usage, "Must have one and only one of -csv, -txt, or -xml");
			System.exit(1);
		}

		VerboseListener verbose_listener = new VerboseListener();
		if (command_line.IsPresent("verbose")) {
			if ("System.out".equals(command_line.OptionalSwitch("verbose"))) {
				verbose_listener.Writer(System.out);
			} else {
				verbose_listener.Writer(new FileWriter(command_line.OptionalSwitch("verbose")));
			}
		}

		/*
		 *  Beginning of main processing
		 */

		Date start = new Date();

		Logger.getLogger(OOMetrics.class).debug("Reading sources ...");

		List parameters = command_line.Parameters();
		if (parameters.size() == 0) {
			parameters.add(".");
		}

		ClassfileLoader loader = new AggregatingClassfileLoader();
		loader.addLoadListener(verbose_listener);
		loader.Load(parameters);

		Logger.getLogger(OOMetrics.class).debug("Reading configuration ...");

		String project_name = command_line.SingleSwitch("project-name");
		
		MetricsFactory factory;
		
		if (command_line.IsPresent("configuration")) {
			factory = new MetricsFactory(project_name, new MetricsConfigurationLoader(command_line.ToggleSwitch("validate")).Load(command_line.SingleSwitch("configuration")));
		} else {
			factory = new MetricsFactory(project_name, new MetricsConfigurationLoader(command_line.ToggleSwitch("validate")).Load(command_line.SingleSwitch("default-configuration")));
		}

		Logger.getLogger(OOMetrics.class).debug("Computing metrics ...");

		com.jeantessier.metrics.MetricsGatherer metrics = new com.jeantessier.metrics.MetricsGatherer(project_name, factory);
		metrics.addMetricsListener(verbose_listener);

		Iterator j = loader.Classfiles().iterator();
		while (j.hasNext()) {
			((Classfile) j.next()).Accept(metrics);
		}

		Logger.getLogger(OOMetrics.class).debug("Printing results ...");
		verbose_listener.println("Printing results ...");
		
		if (command_line.IsPresent("csv")) {
			PrintCSVFiles(start, command_line, metrics.MetricsFactory());
		} else if (command_line.IsPresent("txt")) {
			PrintTextFile(start, command_line, metrics.MetricsFactory());
		} else if (command_line.IsPresent("xml")) {
			PrintXMLFile(start, command_line, metrics.MetricsFactory());
		}

		Logger.getLogger(OOMetrics.class).debug("Done.");

		if (command_line.ToggleSwitch("time")) {
			Date end = new Date();

			System.out.println();
			System.out.println(OOMetrics.class.getName() + ": " + ((end.getTime() - (double) start.getTime()) / 1000) + " secs.");
		}

		verbose_listener.close();
	}

	private static void PrintCSVFiles(Date start, CommandLine command_line, MetricsFactory factory) throws IOException {
		MetricsComparator comparator = new MetricsComparator(command_line.SingleSwitch("sort"));
		if (command_line.ToggleSwitch("reverse")) {
			comparator.Reverse();
		}

		List               metrics;
		Iterator           i;
		com.jeantessier.metrics.Printer printer;
		PrintWriter        out = new PrintWriter(new OutputStreamWriter(System.out));

		if (command_line.ToggleSwitch("project") || command_line.ToggleSwitch("all")) {
			if (command_line.IsPresent("out")) {
				out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + "_project.csv"));
			} else {
				out.println("Project:");
			}
			
			metrics = new ArrayList(factory.ProjectMetrics());
			Collections.sort(metrics, comparator);
			printer = new com.jeantessier.metrics.CSVPrinter(factory.Configuration().ProjectMeasurements());
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			
			if (command_line.IsPresent("out")) {
				out.close();
			} else {
				out.println();
			}
		}

		if (command_line.ToggleSwitch("groups") || command_line.ToggleSwitch("all")) {
			if (command_line.IsPresent("out")) {
				out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + "_groups.csv"));
			} else {
				out.println("Packages:");
			}

			metrics = new ArrayList(factory.GroupMetrics());
			Collections.sort(metrics, comparator);
			printer = new com.jeantessier.metrics.CSVPrinter(factory.Configuration().GroupMeasurements());
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			
			if (command_line.IsPresent("out")) {
				out.close();
			} else {
				out.println();
			}
		}

		if (command_line.ToggleSwitch("classes") || command_line.ToggleSwitch("all")) {
			if (command_line.IsPresent("out")) {
				out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + "_classes.csv"));
			} else {
				out.println("Classes:");
			}

			metrics = new ArrayList(factory.ClassMetrics());
			Collections.sort(metrics, comparator);
			printer = new com.jeantessier.metrics.CSVPrinter(factory.Configuration().ClassMeasurements());
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			
			if (command_line.IsPresent("out")) {
				out.close();
			} else {
				out.println();
			}
		}

		if (command_line.ToggleSwitch("methods") || command_line.ToggleSwitch("all")) {
			if (command_line.IsPresent("out")) {
				out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + "_methods.csv"));
			} else {
				out.println("Methods:");
			}

			metrics = new ArrayList(factory.MethodMetrics());
			Collections.sort(metrics, comparator);
			printer = new com.jeantessier.metrics.CSVPrinter(factory.Configuration().MethodMeasurements());
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
		
			if (command_line.IsPresent("out")) {
				out.close();
			}
		}
	}

	private static void PrintTextFile(Date start, CommandLine command_line, MetricsFactory factory) throws IOException {
		PrintWriter out;
		if (command_line.IsPresent("out")) {
			out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + ".txt"));
		} else {
			out = new PrintWriter(new OutputStreamWriter(System.out));
		}

		MetricsComparator comparator = new MetricsComparator(command_line.SingleSwitch("sort"));
		if (command_line.ToggleSwitch("reverse")) {
			comparator.Reverse();
		}

		List               metrics;
		Iterator           i;

		if (command_line.ToggleSwitch("project") || command_line.ToggleSwitch("all")) {
			out.println("Project metrics");
			out.println("---------------");
			metrics = new ArrayList(factory.ProjectMetrics());
			Collections.sort(metrics, comparator);
			com.jeantessier.metrics.PrettyPrinter printer = new com.jeantessier.metrics.PrettyPrinter(factory.Configuration().ProjectMeasurements());
			printer.ExpandCollectionMeasurements(command_line.ToggleSwitch("expand"));
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			out.println();
		}

		if (command_line.ToggleSwitch("groups") || command_line.ToggleSwitch("all")) {
			out.println("Group metrics");
			out.println("-------------");
			metrics = new ArrayList(factory.GroupMetrics());
			Collections.sort(metrics, comparator);
			com.jeantessier.metrics.PrettyPrinter printer = new com.jeantessier.metrics.PrettyPrinter(factory.Configuration().GroupMeasurements());
			printer.ExpandCollectionMeasurements(command_line.ToggleSwitch("expand"));
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			out.println();
		}

		if (command_line.ToggleSwitch("classes") || command_line.ToggleSwitch("all")) {
			out.println("Class metrics");
			out.println("-------------");
			metrics = new ArrayList(factory.ClassMetrics());
			Collections.sort(metrics, comparator);
			com.jeantessier.metrics.PrettyPrinter printer = new com.jeantessier.metrics.PrettyPrinter(factory.Configuration().ClassMeasurements());
			printer.ExpandCollectionMeasurements(command_line.ToggleSwitch("expand"));
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			out.println();
		}
		
		if (command_line.ToggleSwitch("methods") || command_line.ToggleSwitch("all")) {
			out.println("Method metrics");
			out.println("--------------");
			metrics = new ArrayList(factory.MethodMetrics());
			Collections.sort(metrics, comparator);
			com.jeantessier.metrics.PrettyPrinter printer = new com.jeantessier.metrics.PrettyPrinter(factory.Configuration().MethodMeasurements());
			printer.ExpandCollectionMeasurements(command_line.ToggleSwitch("expand"));
			if (command_line.IsPresent("indent-text")) {
				printer.IndentText(command_line.SingleSwitch("indent-text"));
			}

			printer.VisitMetrics(metrics);
			out.print(printer);
			out.println();
		}
		
		out.close();
	}

	private static void PrintXMLFile(Date start, CommandLine command_line, MetricsFactory factory) throws IOException {
		PrintWriter out;
		if (command_line.IsPresent("out")) {
			out = new PrintWriter(new FileWriter(command_line.SingleSwitch("out") + ".xml"));
		} else {
			out = new PrintWriter(new OutputStreamWriter(System.out));
		}

		MetricsComparator comparator = new MetricsComparator(command_line.SingleSwitch("sort"));
		if (command_line.ToggleSwitch("reverse")) {
			comparator.Reverse();
		}

		List               metrics;
		Iterator           i;
		com.jeantessier.metrics.Printer printer;

		metrics = new ArrayList(factory.ProjectMetrics());
		Collections.sort(metrics, comparator);
		printer = new com.jeantessier.metrics.XMLPrinter(factory.Configuration(), command_line.SingleSwitch("dtd-prefix"));
		if (command_line.IsPresent("indent-text")) {
			printer.IndentText(command_line.SingleSwitch("indent-text"));
		}

		printer.VisitMetrics(metrics);
		out.print(printer);

		out.close();
	}
}

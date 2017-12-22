package com.ptvgroup.devcomp.tooling.shortcut;

import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.IStringConverter;

/**
 * @author Milena Neumann
 *
 */
public class ProjectListConverter implements IStringConverter<List<String>> {
	  @Override
	  public List<String> convert(String projects) {
	    String [] names = projects.split(",");
	    
	    return Arrays.asList(names);
	  }
	}

package org.robotframework.rappio.agent;

import java.lang.instrument.Instrumentation;

public class TestJavaAgent {

	  public static void premain(String agentArgument, Instrumentation instrumentation){
		  System.out.println("Java Agent!");
	  }
	  
}

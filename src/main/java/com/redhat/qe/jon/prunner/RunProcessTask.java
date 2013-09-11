package com.redhat.qe.jon.prunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RunProcessTask implements Callable<Integer> {

    private static Logger log = Logger.getLogger(RunProcessTask.class.getName());
    private final File cmdFile;
    private static final String NEWLINE = System.getProperty("line.separator");

    public RunProcessTask(File cmd) throws FileNotFoundException {
	this.cmdFile = cmd;
	log.info("Starting job from file ["+this.cmdFile.getName()+"]");
	this.writeStatus(ProcessStatus.STARTED);
    }

    private String readCommand() throws IOException {
	log.fine("Reading command from ["+this.cmdFile+"]");
	BufferedReader br = new BufferedReader(new FileReader(this.cmdFile));
	try {
	    StringBuilder sb = new StringBuilder();
	    String line = br.readLine();

	    while (line != null) {
		sb.append(line);
		sb.append(' ');
		line = br.readLine();
	    }
	    return sb.toString().trim();
	} finally {
	    br.close();
	}
    }
    /**
     * gets default environment variables that will inherit child process
     * @return
     */
    private String[] getEnvironment()  throws IOException {
	List<String> env = new ArrayList<String>();
	for (Entry<String,String> e : readEnvp().entrySet()) {
	    env.add(e.getKey()+"="+e.getValue());
	}
	return env.toArray(new String[]{});
    }

    private Map<String,String> readEnvp() throws IOException {
	File envFile = new File(this.cmdFile.getAbsolutePath() + ".env");
	if (!envFile.exists()) {
	    log.fine(".env file does not exist");
	    return System.getenv();
	}
	log.fine("Reading environment variables from ["+envFile.getAbsolutePath()+"]");
	Map<String,String> env = new LinkedHashMap<String, String>(System.getenv());
	BufferedReader br = new BufferedReader(new FileReader(envFile));
	try {
	    String line = br.readLine();
	    while (line != null) {
		String[] var = line.split("=",2);
		if (var.length==2) {
		    env.put(var[0], var[1]);
		}
		line = br.readLine();
	    }
	    return env;
	} finally {
	    br.close();
	}
    }

    private void writeStatus(ProcessStatus status) throws FileNotFoundException {
	writeStatus(status.toString());
    }

    private void writeStatus(String status) throws FileNotFoundException {
	PrintWriter writer = new PrintWriter(this.cmdFile.getAbsolutePath() + ".state");
	writer.println(status);
	writer.close();
    }

    public Integer call() {

	try {
	    final String command = readCommand();
	    String[] cmd;
	    if (Pattern.compile(".*[Ww]indows.*").matcher(System.getProperty("os.name")).matches()) {
		cmd = new String[] { "cmd", "/C", command };
	    } else {
		cmd = new String[] { "/bin/sh", "-c", command };
	    }
	    log.info("Running command: " + Arrays.toString(cmd));
	    final Process p = Runtime.getRuntime().exec(cmd, getEnvironment(), cmdFile.getParentFile());

	    this.writeStatus(ProcessStatus.RUNNING);
	    final PrintWriter output = new PrintWriter(this.cmdFile.getAbsolutePath() + ".out");
	    final PrintWriter error = new PrintWriter(this.cmdFile.getAbsolutePath() + ".err");
	    log.fine("Capturing stdout to "+this.cmdFile.getAbsolutePath() + ".out");
	    log.fine("Capturing sterr to "+this.cmdFile.getAbsolutePath() + ".err");
	    new Runnable() {

		public void run() {
		    String line;
		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    try {
			while ((line = input.readLine()) != null) {
			    output.append(line + NEWLINE);
			    output.flush();
			}
		    } catch (IOException e1) {
			e1.printStackTrace();
		    }
		    try {
			input.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }.run();

	    new Runnable() {

		public void run() {
		    String line;
		    BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		    try {
			while ((line = input.readLine()) != null) {
			    error.append(line + NEWLINE);
			    error.flush();
			}
		    } catch (IOException e1) {
			e1.printStackTrace();
		    }
		    try {
			input.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }.run();

	    p.waitFor();
	    log.info("Finished job from file "+this.cmdFile.getName()+" exit value:"+p.exitValue());
	    output.close();
	    error.close();
	    this.writeStatus(ProcessStatus.FINISHED + ":" + p.exitValue());
	    return p.exitValue();
	} catch (Exception e) {
	    try {
		e.printStackTrace();
		this.writeStatus(ProcessStatus.FAILED);

	    } catch (FileNotFoundException e1) {
		e1.printStackTrace();
	    }
	    return -1;
	}

    }

}

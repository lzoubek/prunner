package com.redhat.qe.jon.prunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Main {

    private static Logger log = Logger.getLogger(Main.class.getName());
    /**
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
	

	String targetDir = System.getProperty("cmd.dir", System.getProperty("java.io.tmpdir","."));
	int threads = Integer.parseInt(System.getProperty("worker.threads","10"));
	int polltime = Integer.parseInt(System.getProperty("prunner.polltime","5"));
	ExecutorService pool = Executors.newFixedThreadPool(threads);
	log.info("Initializing prunner");
	log.info("worker.threads="+threads +" prunner.polltime="+polltime+" cmd.dir="+targetDir);
	log.info("prunner started: Waiting for command files in ["+new File(targetDir)+ "] including *.prun");
	while (true) {
	    Thread.sleep(polltime * 1000L);
	    for (File file : new File(targetDir).listFiles(new FilenameFilter(){
		public boolean accept(File dir, String name) {
		    // pickup only new commands
		    if (name.endsWith(".prun") && !new File(dir, name+".state").exists()) {
			return true;
		    }		    
		    return false;
		}})) {
		
		try {
		    log.info("Found new job ["+file.getName()+"]");
		    pool.submit(new RunProcessTask(file));
		} catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
		
	    }
	    for (File file : new File(targetDir).listFiles(new FilenameFilter(){
		public boolean accept(File dir, String name) {
		    // pickup only new commands
		    if (name.endsWith(".prun.state")) {
			return true;
		    }		    
		    return false;
		}})) {
		
		File prunFile = new File(file.getAbsolutePath().replaceAll("\\.state$", ""));
		if (!prunFile.exists()) {
		    // clean up all produced artifacts
		    log.info("Command ["+prunFile.getName()+"] was removed, cleaning up files");
		    new File(prunFile.getAbsolutePath()+".env").delete();
		    new File(prunFile.getAbsolutePath()+".out").delete();
		    new File(prunFile.getAbsolutePath()+".err").delete();
		    new File(prunFile.getAbsolutePath()+".state").delete();		
		}
		
	    }
	}
	
    }
    

}

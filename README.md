prunner
=======

simple app sniffing on given directory and executing commands based on file content

This simple project helps me integrate cygwin scripts that need to execute native windows processes. Prunner 
looks up **.prun** and **.prun.env** files and executes them.

You can run it using:
```
mvn package exec:java
```

There are several options you can pass
 * ```-Dcmd.dir=/some/dir``` specifies directory where prunner will pick up command files
 * ```-Dworker.threads=3``` specifices thread pool size for executing processes
 * ```-Dprunner.polltime=5``` means that prunner will lookup command files every 5 seconds

Optionally create file with environment variables ```/some/dir/task.prun.env``` with content ```FOO=hello```

Then you create file ```/some/dir/task.prun``` with contents ```echo %FOO%```

Once prunner picks up command it creates
 * **task.prun.state** - state file (STARTED/RUNNING/FINISHED/FAILED)
 * **task.prun.out** - contains STDOUT of your command
 * **task.prun.err** - contains STDERR of your command

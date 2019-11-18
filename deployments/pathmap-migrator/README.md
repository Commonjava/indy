Indy path mapped storage migrator
---

This command line tools is used to do one-off migration task from legacy file based storage to new path mapped storage


### How to use
There are two commands here: scan and migrate

#### scan: generate files to store all paths
Usage: java -jar ${package}.jar scan [options]

Options:  
-B (--batch) N      : Batch of paths to process each time  
-b (--base) VAL     : Base dir of storage for all indy artifacts  
-w (--workdir) VAL  : Work dir to store all generated working files
-f (--filter) VAL   : Regex style filter string to filter some files which are unwanted

#### migrate: read all files for paths and migrate them to cassandra db
##### Note: Before this command, please use "scan" to generate all paths files first
Usage: java -jar ${package}.jar migrate [options]

Options:  
-w (--workdir) VAL  : Work dir to store all generated working files  
-H (--host) VAL     : Cassandra server hostname  
-P (--port) VAL     : Cassandra server port  
-k (--keyspace) VAL : Cassandra server keyspace  
-p (--password) VAL : Cassandra server password  
-u (--user) VAL     : Cassandra server username  
-d (--dedupe)       : If to use checksum to dedupe all files in file storage  

For migrate command, when it start, there will be a "status" file generated in ${workDir} to record current processing status, and will be updated every 30 seconds.
## Flume-ng-oci-streaming-sink

This is not ORACLE official product.

Oracle OCI streaming Sink for flume 1.4.0.

### Install

    mvn assembly:assembly
    cp target/flume-ng-oci-streaming-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar /path/to/flume/lib

### Configure

##Sink

This example reads from syslog udp 514 and writes them directly to my-topic in OCI Streaming.

    a1.sources  = r1
    a1.channels = channel1
    a1.sinks    = oci1

    a1.sources.r1.type                  = syslogudp
    a1.sources.r1.port                  = 514
    a1.sources.r1.channels              = oci11

    a1.channels.channel1.type                = memory
    a1.channels.channel1.capacity            = 10000
    a1.channels.channel1.transactionCapacity = 200

    a1.sinks.oci1.type                   = org.apache.flume.sinks.OCIStreamingSink
    a1.sinks.oci1.streamName             = igor
    a1.sinks.oci1.compartmentId          = xxx
    a1.sinks.oci1.key                    = yourkey
    a1.sinks.oci1.channel                = c1
    
##Source    

This example reads from OCI Streaming and writes them in the log.

    a1.sources = r1
    a1.channels = c1
    a1.sinks = oci1

    a1.sources.r1.type = org.apache.flume.source.OCIStreamingSource
    a1.sources.r1.streamName = igor
    a1.sources.r1.compartmentId = xxx
    a1.sources.r1.partition = 0
    a1.sources.r1.channels = c1

    a1.channels.c1.type = memory
    a1.channels.c1.capacity = 1000
    a1.channels.c1.transactionCapacity = 1000

    a1.sinks.oci1.type = logger
    a1.sinks.oci1.channel = c1
    
### run

    ./bin/flume-ng agent --conf ./conf/  --conf-file ./conf/yourconffile.conf --name a1
    
    
### Work in progress
Flume Sink

Flume Source


### License

Free - Open


## Disclaimer
This is a personal repository. Any code, views or opinions represented here are personal and belong solely to me and do not represent those of people, institutions or organizations that I may or may not be associated with in professional or personal capacity, unless explicitly stated.

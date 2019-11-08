## Flume-ng-oci-streaming-sink

This is not ORACLE official product.

Oracle OCI streaming Sink for flume 1.4.0.

### Install

    mvn assembly:assembly
    cp target/flume-ng-oci-streaming-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar /path/to/flume/libs

### Configure

This example reads from syslog udp 514, and writes them directly to my-topic in OCI Streaming.

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
    
### Work in progress
Flume Sink

Flume Source


### License

Free - Open


## Disclaimer
This is a personal repository. Any code, views or opinions represented here are personal and belong solely to me and do not represent those of people, institutions or organizations that I may or may not be associated with in professional or personal capacity, unless explicitly stated.

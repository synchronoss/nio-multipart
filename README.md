Non-Blocking Multipart parser
=============================

The NIO Multipart project contains a lightweight, generic java library to process multipart requests and responses in a non blocking way
and with a configurable, but constant, memory footprint.
It integrates gracefully with the Servlet 3.1 NIO features but it can be easily used in a blocking IO fashion.
The library is intentionally decoupled from the java servlet APIs and it's based on InputStreams and OutputStreams instead.
This makes the library generic and usable for processing not only Http Requests, but also Http responses and potentially other transport protocols.
It is also a good solution for Android given it's reduced dependencies tree.

Requires JDK 1.6 or higher.

Latest release
--------------

The most recent release is nio-multipart 0.0.1.

To add a dependency on NIO Multipart Parser using Maven, use the following:

```xml
<dependency>
  <groupId>com.synchronoss.cloud</groupId>
  <artifactId>nio-multipart-parser</artifactId>
  <version>0.0.1</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'com.synchronoss.cloud:nio-multipart-parser:0.0.1'
}
```

How to use
----------




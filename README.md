Non-Blocking Multipart parser
=============================

The NIO Multipart project contains a lightweight, generic java library to process multipart requests and responses in a non blocking way
and with a configurable, but constant, memory footprint.
It integrates gracefully with the Servlet 3.1 NIO features but it can be easily used in a blocking IO fashion.
The library is intentionally decoupled from the java servlet APIs and it's based on InputStreams and OutputStreams instead.
This makes the library generic and usable for processing not only Http Requests, but also Http responses and potentially other transport protocols.
It is also a good solution for Android given it's reduced dependencies tree.

Requires JDK 1.7 or higher.

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

Get started
-----------
The simplest way to get started is using the simple fluent API provided with the library. Instantiating a parser is straightforward:

```java
NioMultipartParser parser = newParser(context, listener).forNio();
```

The only two mandatory arguments are a multipart context, holding information about the current request/response, and a listener that will be notified on the progress of the parsing.
The following line shows how a context can be created from an HttpServletRequest:

```java
MultipartContext context = MultipartContext(request.getContentType(), request.getContentLength(), request.getCharacterEncoding())
```

The listener is where application logic starts. The parser notifies the client when something happen via several methods defined by the NioMultipartParserListener interface.
Clients can decide to inline the definition (like the example below) or create a class that extends the interface. 
What is important is that the client reacts to the events to implement the desired behaviour.
 
```java
NioMultipartParserListener listener = new NioMultipartParserListener() {

    @Override
    public void onPartReady(final PartStreams partStreams, final Map<String, List<String>> headersFromPart) {
        // The parser completed parsing the part. 
        // The parsed headers are available in the headersFromPart map
        // The data can be read from the stream partStreams.getPartInputStream()
    }

    @Override
    public void onNestedPartStarted(final Map<String, List<String>> headersFromParentPart) {
        // The parser identified that the current part contains a nested multipart body
        // The headers are provided in the headersFromParentPart
        // Like for the level 0 parts, the parser will notify the sub parts completion via the methods onPartReady(...) and onFormFieldPartReady(...)
        // When the nested multipart body is finished, the parser will call onNestedPartFinished()
    }

    @Override
    public void onNestedPartFinished() {
        // Called when a nested multipart body has been parsed
    }

    @Override
    public void onFormFieldPartReady(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
        // Called when the part represents a form field.
        // Instead of a PartStreams object, the field name and filedValue are already extracted by the parser and returned to the client.
    }

    @Override
    public void onAllPartsFinished() {
        // Called when the multipart processing finished (encountered the close boundary). No more parts are available!
    }

    @Override
    public void onError(String message, Throwable cause) {
        // An error happened during the parsing.
        // At this point the parser is in an error state and it cannot process any data anymore.
    }
};
```

The final step is to feed the parser with the bytes of the multipart body. In a Servlet 3.1 scenario it could look something like:


```java
final AsyncContext asyncContext = request.startAsync();
final ServletInputStream inputStream = request.getInputStream();

inputStream.setReadListener(new ReadListener() {

    @Override
    public void onDataAvailable() throws IOException {
        int bytesRead;
        byte bytes[] = new byte[1024];
        while (inputStream.isReady()  && (bytesRead = inputStream.read(bytes)) != -1) {
            // Pass the received bytes into the parser. 
            // When something is ready the listener will be called!
            parser.write(bytes, 0, bytesRead);
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        // NOTE - This method might be called before the parser actually finished the parsing, so parser.close() shouldn't be called without ensuring
        // onAllPartsFinished() or onError() has been called
    }

    @Override
    public void onError(Throwable throwable) {
        // Here the parser should be closed
        parser.close();
    }
});
```

It is important to close the parser if an error happens, if the client decides to stop the processing and if the parsing finishes correctly.

References
----------
[RFC1867](http://www.ietf.org/rfc/rfc1867.txt)
[RFC1521](http://www.ietf.org/rfc/rfc1521.txt)
[RFC1522](http://www.ietf.org/rfc/rfc1522.txt)



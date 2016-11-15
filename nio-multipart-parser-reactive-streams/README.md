Non-Blocking HTTP Multipart parser Reactive Streams based
=========================================================

Overview
--------
This module is an attempt to make the Nio Multipart Parser compliant with the [Reactive Streams](http://www.reactive-streams.org/) pattern.

High level requirements
-----------------------

* Easy to use: The final API should be easy to use
* Easy integration with servlet 3.1. The key part here is that the servlet 3.1. ReadListener needs to be translated into a Publisher.
* Android compatible (Nice to have). It would be nice to have the library Android compatible in order to use it to parse multipart responses.
* Dependency free (as much as possible): One of the key targets for the library was to keep it a small standalone library. This should still be the case if possible.
* Keep the same performances and the same memory footprint (or improve it). Limit the GC as much as possible.
* Adapter for blocking IO and for Callback based.

The current situation
---------------------
Currently the parser works as follows:

* The ReadListener is attached to the ServletInputStream and when the onDataAvailable is triggered, the bytes are written to the NioMultipartParser.
* The NioMultipartParser is writing the data into a smart circular buffer that keeps track of the various sections of the multipart message.
* Every time:
  - The circular buffer is full, data is flushed into a sink to free some space. The sink can be:
    1. Null (for preamble and epilogue)
    2. In memory ByteArrayOutputStream (for form parameters and headers)
    3. StreamStorage (for files/attachments). This is the potentially blocking because disk IO, DB IO, Network IO can be involved...
  - The buffer identifies the end of a section, the NioMultipartParser is firing the correspondent event for the attached listener


![Nio Multipart Parser Components Overview](../docs/diagrams/nio-multipart-components-overview.png)

Initial investigation
---------------------

* Everything should become a reactive component that is part of a pipeline. The framework will then automatically handle back pressure.
* Initially use the Reactor 3 framework and in case refactor to use the reactive-streams-commons to achieve Android compatibility.
* Initially stay away from the StreamStorage and evaluate later on if it is the case to refactor the StreamStorage to be Reactive based.


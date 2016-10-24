# Java HTTP Proxy
A locally hosted Java proxy server. It is a multithreaded program, employing the “man in the middle” design. Once the user’s browser has been connected to the locally hosted port number of the proxy, all HTTP requests are forwarded from the user’s browser (the client), to the Java proxy. The proxy then relays this request to the server, and returns the response to the client. In order to save bandwidth and time, certain websites are cached based on the 'max-age' header. The user can also block certain URLs on the server.


### To Compile
```sh
$ javac JavaProxy.java
$ java JavaProxy
```
This will run the Proxy at localhost 8001.

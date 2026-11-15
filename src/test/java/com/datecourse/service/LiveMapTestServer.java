package com.datecourse.service;

/** Isolated loopback port for a live test when the user's application owns 8080. */
public final class LiveMapTestServer implements AutoCloseable {
    private final MapPageServer server;
    public LiveMapTestServer() throws java.io.IOException {server=new MapPageServer(0,java.nio.file.Path.of(""),System.getenv());}
    public java.net.URI page() {return server.pageUri();}
    @Override public void close(){server.close();}
}

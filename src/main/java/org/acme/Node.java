package org.acme;

import java.net.URI;

public class Node {

    public String name;
    public String nodeType;
    public Target target;

    public static class Target {
        public URI connectUrl;
        public String alias;
    }

}

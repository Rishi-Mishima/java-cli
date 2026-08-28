package com.mycliagent.tool;

public interface BrowserConnector {
    String connectDefault();

    String disconnect();

    String status();
}

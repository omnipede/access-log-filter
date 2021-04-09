package io.omnipede.system.filter.accesslog;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * HostName of server
 */
class HostName {

    private String hostName;

    public HostName() {
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            this.hostName = "Unknown";
        }
    }

    @Override
    public String toString() {
        return hostName;
    }
}

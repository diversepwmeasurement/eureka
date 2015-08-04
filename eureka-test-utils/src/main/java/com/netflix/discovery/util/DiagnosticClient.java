package com.netflix.discovery.util;

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.DefaultEurekaServerConfig;
import com.netflix.eureka.cluster.JerseyReplicationClient;

import static com.netflix.discovery.util.ApplicationFunctions.countInstances;

/**
 * A tool for running diagnostic tasks against a discovery server. Currently limited to observing
 * of consistency of delta updates.
 *
 * @author Tomasz Bak
 */
public class DiagnosticClient {

    public static void main(String[] args) throws InterruptedException {
        String discoveryURL = args[0];
        long startTime = System.currentTimeMillis();
        JerseyReplicationClient client = new JerseyReplicationClient(new DefaultEurekaServerConfig("eureka."), discoveryURL);
        Applications applications = client.getApplications().getEntity();
        System.out.println("Applications count=" + applications.getRegisteredApplications().size());
        System.out.println("Instance count=" + countInstances(applications));
        while (true) {
            long delay = System.currentTimeMillis() - startTime;
            if (delay >= 30000) {
                System.out.println("Processing delay exceeds 30sec; we may be out of sync");
            } else {
                long waitTime = 30 * 1000 - delay;
                System.out.println("Waiting " + waitTime / 1000 + "sec before next fetch...");
                Thread.sleep(15 * 1000);
            }
            startTime = System.currentTimeMillis();
            Applications delta = client.getDelta().getEntity();
            Applications merged = ApplicationFunctions.merge(applications, delta);
            if (merged.getAppsHashCode().equals(delta.getAppsHashCode())) {
                System.out.println("Hash codes match: " + delta.getAppsHashCode() + "(delta count=" + countInstances(delta) + ')');
                applications = merged;
            } else {
                System.out.println("ERROR: hash codes do not match (" + delta.getAppsHashCode() + "(delta) != "
                                + merged.getAppsHashCode() + " (merged) != "
                                + applications.getAppsHashCode() + "(old apps)" +
                                "(delta count=" + countInstances(delta) + ')'
                );
                applications = client.getApplications().getEntity();
            }
        }
    }
}
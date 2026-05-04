package com.hubfeatcreators.infra.job;

public interface JobHandler {
    void handle(Job job) throws Exception;
}

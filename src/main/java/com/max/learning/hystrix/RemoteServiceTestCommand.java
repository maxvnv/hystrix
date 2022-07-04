package com.max.learning.hystrix;

import com.netflix.hystrix.HystrixCommand;

public class RemoteServiceTestCommand extends HystrixCommand<String> {

    private final RemoteServiceSimulator remoteServiceSimulator;

    RemoteServiceTestCommand(Setter config, RemoteServiceSimulator remoteServiceSimulator) {
        super(config);
        this.remoteServiceSimulator = remoteServiceSimulator;

    }

    @Override
    protected String run() throws InterruptedException {
        return remoteServiceSimulator.execute();
    }

//    @Override
//    protected String getFallback() {
//        return "Fallback executed!";
//    }
}

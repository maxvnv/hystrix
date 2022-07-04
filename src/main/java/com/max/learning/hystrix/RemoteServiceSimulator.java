package com.max.learning.hystrix;

public class RemoteServiceSimulator {

    private final long wait;


    public RemoteServiceSimulator(long wait) {
        this.wait = wait;
    }

    String execute() throws InterruptedException {
        Thread.sleep(wait);
        return "Success";
    }
}

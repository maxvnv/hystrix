package com.max.learning.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class HystrixTests {

    @Test
    void shouldRunTestCommand() {
        assertThat(new CommandHelloWorld("Bob").execute(), equalTo("Hello Bob!"));
    }

    @Test
    void shouldTimeOutOnRemoteService() {
        HystrixCommand.Setter config = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("remote-service"))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("remote-service-thread-pool"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(1_000));


        RemoteServiceTestCommand command = new RemoteServiceTestCommand(config, new RemoteServiceSimulator(3_000));

        Assertions.assertThatThrownBy(command::execute)
                .isInstanceOf(HystrixRuntimeException.class);
    }

    @Test
    void shouldRunRemoteServiceCommand1() {
        HystrixCommand.Setter config = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("remote-service"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(1_000));


        RemoteServiceTestCommand command = new RemoteServiceTestCommand(config, new RemoteServiceSimulator(3_000));

        assertThat(command.execute(), equalTo("Success"));
    }

    @Test
    void testingParallelExecution() throws Exception {
        HystrixCommand.Setter config = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("remote-service"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(100000))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withMaxQueueSize(3) // circuit breaker request volume threshold
                        .withCoreSize(5)
                        .withQueueSizeRejectionThreshold(10) // max concurrent requests
                );


        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Callable<String>> callables = getCallables(config, new RemoteServiceSimulator(2000));

        List<Future<String>> results = executorService.invokeAll(callables);

        for (Future<String> result : results) {
            result.get();
        }

    }

    private List<Callable<String>> getCallables(HystrixCommand.Setter config, RemoteServiceSimulator remoteServiceSimulator) {
        return IntStream.range(0, 10)
                .mapToObj(it -> Pair.of(new RemoteServiceTestCommand(config, remoteServiceSimulator), it))
                .map(commandToIndex -> executeAndLog(commandToIndex.getLeft(), commandToIndex.getRight()))
                .toList();
    }

    private <T> Callable<T> executeAndLog(HystrixCommand<T> command, int it) {
        return () -> {
            System.out.printf("EXECUTING task # %d on a thread %s %n", it, Thread.currentThread().getName());
            T result = command.execute();
            System.out.printf("EXECUTED task # %d on a thread %s %n", it, Thread.currentThread().getName());
            return result;
        };
    }

}
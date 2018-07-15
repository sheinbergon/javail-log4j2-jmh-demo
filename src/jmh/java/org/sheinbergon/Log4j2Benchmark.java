package org.sheinbergon;


import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class Log4j2Benchmark {

    public enum LoggerType {
        ROLLING("Rolling"),
        MMAP("MMap");
        private final String name;

        LoggerType(String name) {
            this.name = name;
        }
    }

    private final static int FORKS = 2;
    private final static int WARMUPS = 2;
    private final static int ITERATIONS = 5;
    private final static int MILLISECONDS = 50;

    private final static int RANDOM_STRING_MIN_LENGTH = 100;
    private final static int RANDOM_STRING_MAX_LENGTH = 200;
    private final static int MESSAGE_COUNT = 500000;

    private Logger log;
    private List<String> messages;
    @Param({"MMAP", "ROLLING"})
    private LoggerType loggerType;

    @Param({"true", "false"})
    private boolean filtered;

    @Setup
    public void setup() {
        messages = IntStream.range(0, MESSAGE_COUNT)
                .mapToObj(index -> RandomStringUtils.randomAlphanumeric(RANDOM_STRING_MIN_LENGTH, RANDOM_STRING_MAX_LENGTH))
                .collect(Collectors.toList());
        log = LogManager.getLogger(loggerType.name);
    }

    @Benchmark
    @Fork(value = FORKS, jvmArgsAppend = {
            "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
            "-Xmx4096m",
            "-Xms2048m",
            "-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector",
            "-Dlog4j2.asyncLoggerThreadNameStrategy=CACHED",
            "-Dlog4j2.asyncLoggerRingBufferSize=8388608",
            "-Dlog4j2.asyncLoggerWaitStrategy=Sleep",
            "-Dlog4j2.enable.direct.encoders=true",
            "-Dlog4j2.enable.threadlocals=true"})
    @Warmup(iterations = WARMUPS, timeUnit = TimeUnit.MILLISECONDS, time = MILLISECONDS)
    @Measurement(iterations = ITERATIONS, timeUnit = TimeUnit.MILLISECONDS, time = MILLISECONDS)
    public void asynchronousLogging() {
        int index = RandomUtils.nextInt(0, MESSAGE_COUNT);
        if (filtered) {
            log.debug(messages.get(index));
        } else {
            log.info(messages.get(index));
        }
    }


    @Benchmark
    @Fork(value = FORKS, jvmArgsAppend = {
            "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
            "-Xmx4096m",
            "-Xms2048m",
            "-Dlog4j2.enable.direct.encoders=true",
            "-Dlog4j2.enable.threadlocals=true"})
    @Warmup(iterations = WARMUPS, timeUnit = TimeUnit.MILLISECONDS, time = MILLISECONDS)
    @Measurement(iterations = ITERATIONS, timeUnit = TimeUnit.MILLISECONDS, time = MILLISECONDS)
    public void synchronousLogging() {
        int index = RandomUtils.nextInt(0, MESSAGE_COUNT);
        if (filtered) {
            log.debug(messages.get(index));
        } else {
            log.info(messages.get(index));
        }
    }
}

package io.axual.ksml.runner.backend;

import java.time.Duration;

public interface Scheduled {

    boolean shouldReschedule();

    Duration interval();

}

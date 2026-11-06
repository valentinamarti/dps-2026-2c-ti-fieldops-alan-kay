package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "fieldops.events", name = "dispatcher-enabled", havingValue = "true")
public class EventDispatchScheduler {
    private final EventDispatcher dispatcher;

    public EventDispatchScheduler(EventDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher is required");
    }

    @Scheduled(fixedDelayString = "${fieldops.events.dispatch-interval}")
    void dispatchPending() {
        dispatcher.dispatchPending();
    }
}

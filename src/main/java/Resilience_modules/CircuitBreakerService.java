package Resilience_modules;
import failues.AlwaysSlowNSeconds;
import failures.SucceedNTimesAndThenFail;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import model.Ingredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import service.IngredientService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Service
public class CircuitBreakerService {

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public List<Ingredient> TimeoutFailure() {
        ResponseEntity<Ingredient> response =
                restTemplate.getForEntity("http://localhost:8081/ingredient", Ingredient.class);
        return Collections.emptyList();
    }

    private final String CIRCUIT_BREAKER_INGREDIENTS_PROPERTY = "IngredientService"; //used in application.yml

    @CircuitBreaker(name = CIRCUIT_BREAKER_INGREDIENTS_PROPERTY, fallbackMethod = "ingredientFallback")
    public List<Ingredient> GetIngredientsTimeoutError() throws Exception {
        return TimeoutFailure();
    }

    public List<Ingredient> ingredientFallback(Exception e) {
        System.out.println("CircuitBreaker fallback method... Couldn't get the ingredients :(");
        return Collections.emptyList();
    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss SSS");

    void displayDefaultValues() {
        CircuitBreakerConfig config = CircuitBreakerConfig.ofDefaults();
        System.out.println("failureRateThreshold = " + config.getFailureRateThreshold());
        System.out.println("minimumNumberOfCalls = " + config.getMinimumNumberOfCalls());
        System.out.println("permittedNumberOfCallsInHalfOpenState = " + config.getPermittedNumberOfCallsInHalfOpenState());
        System.out.println("maxWaitDurationInHalfOpenState = " + config.getMaxWaitDurationInHalfOpenState());
        System.out.println("slidingWindowSize = " + config.getSlidingWindowSize());
        System.out.println("slidingWindowType = " + config.getSlidingWindowType());
        System.out.println("slowCallRateThreshold = " + config.getSlowCallRateThreshold());
        System.out.println("slowCallDurationThreshold = " + config.getSlowCallDurationThreshold());
        System.out.println("automaticTransitionFromOpenToHalfOpenEnabled = " + config.isAutomaticTransitionFromOpenToHalfOpenEnabled());
        System.out.println("writableStackTraceEnabled = " + config.isWritableStackTraceEnabled());
    }

    public List<Ingredient> countBasedSlidingWindow_FailedCalls() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(70.0f)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        ingredientService.setPotentialFailure(new SucceedNTimesAndThenFail(10));

        Supplier<List<Ingredient>> ingredientsSupplier = () -> ingredientService.getIngredients();
        Supplier<List<Ingredient>> decoratedIngredientsSupplier = circuitBreaker.decorateSupplier(ingredientsSupplier);

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println("Getting results...");
                System.out.println(decoratedIngredientsSupplier.get());
            } catch (Exception e) {
                System.out.println("Circuit breaker should be open and not permit further requests..");
//                e.printStackTrace();
            }
        }
        return decoratedIngredientsSupplier.get();
    }


    public List<Ingredient> countBasedSlidingWindow_SlowCalls() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5) // max number of requests before OPEN CIRCUIT
                .slowCallRateThreshold(70.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        ingredientService.setPotentialDelay(new AlwaysSlowNSeconds(2));

        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() -> ingredientService.getIngredients());

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println("Getting results...");
                System.out.println(ingredientSupplier.get());
            } catch (Exception e) {
                System.out.println("Circuit breaker should be open and not permit further requests..");
//                e.printStackTrace();
            }
        }
        return ingredientSupplier.get();
    }

    public List<Ingredient> countBasedSlidingWindow_Failed_And_SlowCalls() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(70.0f)
                .slowCallRateThreshold(70.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        ingredientService.setPotentialDelay(new AlwaysSlowNSeconds(2));

        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() -> ingredientService.getIngredients());

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println("Getting results...");
                System.out.println(ingredientSupplier.get());
            } catch (Exception e) {
                System.out.println("Circuit breaker should be half/open and not permit further requests..");
//                e.printStackTrace();
            }
        }
        return ingredientSupplier.get();
    }

    public List<Ingredient> timeBasedSlidingWindow_FailedCalls() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .minimumNumberOfCalls(3)
                .slidingWindowSize(10)
                .failureRateThreshold(70.0f)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        ingredientService.setPotentialFailure(new SucceedNTimesAndThenFail(3));
        ingredientService.setPotentialDelay(new AlwaysSlowNSeconds(0));

        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() -> ingredientService.getIngredients());

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println("Getting results...");
                System.out.println(ingredientSupplier.get());
            } catch (Exception e) {
                System.out.println("Circuit breaker should be half/open and not permit further requests..");
//                e.printStackTrace();
            }
        }
        return ingredientSupplier.get();
    }

    public List<Ingredient> timeBasedSlidingWindow_SlowCalls() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .minimumNumberOfCalls(3)
                .slidingWindowSize(10)
                .failureRateThreshold(70.0f)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        ingredientService.setPotentialDelay(new AlwaysSlowNSeconds(1));

        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() -> ingredientService.getIngredients());

        System.out.println("Start time: " + LocalDateTime.now().format(formatter));

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println("Getting results...");
                System.out.println(ingredientSupplier.get());
            } catch (Exception e) {
                System.out.println("Circuit breaker should be half/open and not permit further requests..");
//                e.printStackTrace();
            }
        }
        return ingredientSupplier.get();
    }

    public List<Ingredient> circuitBreakerOpenAndThenClose() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(25.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(4)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        circuitBreaker.getEventPublisher().onCallNotPermitted(e -> {
            System.out.println(e.toString());
            // just to simulate lag so the circuitbreaker can change state
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });
        circuitBreaker.getEventPublisher().onError(e -> System.out.println(e.toString()));
        circuitBreaker.getEventPublisher().onStateTransition(e -> System.out.println(e.toString()));


        ingredientService.setPotentialFailure(new failures.SucceedXTimesFailYTimesAndThenSucceed(4, 4));

        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() -> ingredientService.getIngredients());

        System.out.println("Start time: " + LocalDateTime.now().format(formatter));

        for (int i=0; i<50; i++) {
            try {
                System.out.println(ingredientSupplier.get());
                Thread.sleep(1000);
            }
            catch (Exception e) {
//                e.printStackTrace();
                System.out.println("Failing on purpose to open the circuit...");
            }
        }
        return ingredientSupplier.get();
    }


    // Import Errors migrating from Maven -> Gradle
    public List<Ingredient> circuitBreakerFallback() {
//        CircuitBreakerConfig config = CircuitBreakerConfig
//                .custom()
//                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
//                .minimumNumberOfCalls(5)
//                .slidingWindowSize(10)
//                .failureRateThreshold(50.0f)
//                .writableStackTraceEnabled(false)
//                .build();
//        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
//        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");
//
//        circuitBreaker.getEventPublisher().onStateTransition(e -> System.out.println(e.toString()));
//        circuitBreaker.getEventPublisher().onError(e -> System.out.println(e.toString()));
//
//        ingredientService.setPotentialFailure(new SucceedNTimesAndThenFail(3));
//
//        Supplier<List<Ingredient>> ingredientSupplier = () -> ingredientService.getIngredients();
//
//        Supplier<List<Ingredient>> decorated = Decorators
//                .ofSupplier(ingredientSupplier)
//                .withCircuitBreaker(circuitBreaker)
//                .withFallback(Arrays.asList(CallNotPermittedException.class),
//                        e -> this.ingredientService.getIngredientsfromCache())
//                .decorate();
//
//        for (int i=0; i<10; i++) {
//            try {
//                System.out.println(decorated.get());
//            }
//            catch (Exception e) {
//            }
//        }
//        return decorated.get();
        return Collections.emptyList();
    }

    public List<Ingredient> getIngredientsCircuitBreakerEvents() {
        CircuitBreakerConfig config = CircuitBreakerConfig
                .custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .failureRateThreshold(70.0f)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(e -> System.out.println(e.toString()));
        circuitBreaker.getEventPublisher().onError(e -> System.out.println(e.toString()));
        circuitBreaker.getEventPublisher()
                .onFailureRateExceeded(e -> System.out.println(e.toString()));
        circuitBreaker.getEventPublisher().onStateTransition(e -> System.out.println(e.toString()));

        ingredientService.setPotentialFailure(new SucceedNTimesAndThenFail(3));


        Supplier<List<Ingredient>> ingredientSupplier = circuitBreaker.decorateSupplier(() ->
                ingredientService.getIngredients());

        for (int i = 0; i < 20; i++) {
            try {
                System.out.println(ingredientSupplier.get());
            } catch (Exception e) {
            }
        }
        return ingredientSupplier.get();
    }

    public List<Ingredient> getIngredientsCircuitBreakerMetrics() {
//        CircuitBreakerConfig config = CircuitBreakerConfig
//                .custom()
//                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
//                .slidingWindowSize(10)
//                .failureRateThreshold(70.0f)
//                .build();
//        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
//        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = registry.circuitBreaker("IngredientService");
//
//        MeterRegistry meterRegistry = new SimpleMeterRegistry();
//        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry)
//                .bindTo(meterRegistry);
//
//        circuitBreaker.getEventPublisher()
//                .onCallNotPermitted(e -> printMetricDetails(meterRegistry));
//        circuitBreaker.getEventPublisher().onError(e -> printMetricDetails(meterRegistry));
//        circuitBreaker.getEventPublisher()
//                .onFailureRateExceeded(e -> printMetricDetails(meterRegistry));
//        circuitBreaker.getEventPublisher().onStateTransition(e -> printMetricDetails(meterRegistry));
//
//
//        ingredientService.setPotentialFailure(new SucceedNTimesAndThenFail(3));
//
//        Supplier<List<Ingredient>> ingredientsSupplier = circuitBreaker
//                .decorateSupplier(() -> ingredientService.getIngredients());
//
//        for (int i = 0; i < 20; i++) {
//            try {
//                Thread.sleep(1000);
//                System.out.println("\ngetting results and CIRCUITBREAKER Metric config: ");
//                System.out.println(ingredientsSupplier.get());
//
//            } catch (Exception e) {
//                System.out.println("Failing on point/CircuitBreaker OPEN");
//            }
//        }
//        printMetricDetails(meterRegistry);
//        return ingredientsSupplier.get();
        return Collections.emptyList();
    }

    void printMetricDetails(MeterRegistry meterRegistry) {
        Consumer<Meter> meterConsumer = meter -> {
            String desc = meter.getId().getDescription();
            String metricName = meter.getId().getName();
            String tagName = "";
            String tagValue = "";
            if (metricName.equals("resilience4j.circuitbreaker.state")) {
                tagName = "state";
                tagValue = meter.getId().getTag(tagName);
            }
            if (metricName.equals("resilience4j.circuitbreaker.calls")) {
                tagName = "kind";
                tagValue = meter.getId().getTag(tagName);
            }
            Double metricValue = StreamSupport.stream(meter.measure().spliterator(), false)
                    .filter(m -> {
                        return m.getStatistic().name().equals("VALUE");
                    })
                    .findFirst()
                    .map(m -> m.getValue())
                    .orElse(0.0);
            System.out.print(desc + " - " + metricName + ": " + metricValue);
            if (!tagValue.isEmpty()) {
                System.out.println(", " + tagName + ": " + tagValue);
            }
            else {
                System.out.println();
            }
        };
        meterRegistry.forEachMeter(meterConsumer);
    }
}

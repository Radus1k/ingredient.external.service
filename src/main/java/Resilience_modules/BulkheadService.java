package Resilience_modules;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
//import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import model.Ingredient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.IngredientService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Service
public class BulkheadService {

    @Autowired
    private IngredientService ingredientService;


    private final String INGREDIENTS_PROPERTY = "IngredientService"; //used in application.yml


    @Bulkhead(name=INGREDIENTS_PROPERTY, fallbackMethod = "ingredientBulkHeadConcurrentCallsFallback")
    public List<Ingredient> ingredientBulkHeadConcurrentCallsFallback(){
        System.out.println("Ingredient BUlkhead fallback called!");
        return Collections.emptyList();
    }

    @Bulkhead(name=INGREDIENTS_PROPERTY, fallbackMethod = "ingredientBulkHeadFallback")
    public List<Ingredient> ingredientBulkHeadFallback(Exception e){
        System.out.println("IngredientService is down!");
       return Collections.emptyList();
    }

    // Use cases

    void displayDefaultValues() {
        BulkheadConfig config = BulkheadConfig.ofDefaults();
        System.out.println("Max concurrent calls = " + config.getMaxConcurrentCalls());
        System.out.println("Max wait duration = " + config.getMaxWaitDuration());
        System.out.println("Writable stack trace enabled = " + config.isWritableStackTraceEnabled());
        System.out.println("Fair call handling enabled = " + config.isFairCallHandlingEnabled());
    }

    public List<Ingredient> GetIngredientsSuccesfullyBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofSeconds(2))
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead("IngredientService");


        Supplier<List<Ingredient>> ingredientsSupplier = () -> ingredientService.getIngredientsInOneSecond();
        Supplier<List<Ingredient>> decoratedIngredientsSupplier = io.github.resilience4j.bulkhead.Bulkhead.decorateSupplier(bulkhead, ingredientsSupplier);

        for (int i=0; i<4; i++) {
            CompletableFuture
                    .supplyAsync(decoratedIngredientsSupplier)
                    .thenAccept(ingredients -> System.out.println("Received results"));
        }
        return decoratedIngredientsSupplier.get();
    }

    public List<Ingredient> GetIngredientsExceptionByBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofSeconds(3))//
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead("IngredientService");


        Supplier<List<Ingredient>> ingredientsSupplier = () -> ingredientService.getIngredientsInOneSecond();
        Supplier<List<Ingredient>> decoratedIngredientsSupplier = io.github.resilience4j.bulkhead.Bulkhead.decorateSupplier(bulkhead, ingredientsSupplier);

        for (int i=0; i<7; i++) {
            CompletableFuture
                    .supplyAsync(decoratedIngredientsSupplier)
                    .whenComplete( (r, t) -> {
                        if (t != null) {
                            Throwable cause = t.getCause();
                            if (cause != null) {
                                cause.printStackTrace();
                            }
                        }
                        if (r != null) {
                            System.out.println("Received results");
                        }
                    });
        }
        return decoratedIngredientsSupplier.get();
    }

    public List<Ingredient> GetIngredientsEventsByBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxWaitDuration(Duration.ofMillis(500))
                .maxConcurrentCalls(6)
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead("IngredientService");

        bulkhead.getEventPublisher().onCallPermitted(e -> System.out.println(e.toString()));
        bulkhead.getEventPublisher().onCallFinished(e -> System.out.println(e.toString()));
        bulkhead.getEventPublisher().onCallRejected(e -> System.out.println(e.toString()));


        Supplier<List<Ingredient>> ingredientsSupplier = () -> ingredientService.getIngredientsTakingRandomTime();
        Supplier<List<Ingredient>> decoratedIngredientsSupplier = io.github.resilience4j.bulkhead.Bulkhead.decorateSupplier(bulkhead, ingredientsSupplier);

        for (int i=0; i<8; i++) {
            CompletableFuture
                    .supplyAsync(decoratedIngredientsSupplier)
                    .whenComplete( (r, t) -> {
                        if (t != null) {
                            t.printStackTrace();
                        }
                        if (r != null) {
                            System.out.println("Received results");
                        }
                    });
        }
        return decoratedIngredientsSupplier.get();
    }

    // Import Errors in Gradle..
//    public List<Ingredient> GetIngredientsMetricsByBulkhead() {
//        BulkheadConfig config = BulkheadConfig.custom()
//                .maxWaitDuration(Duration.ofMillis(500))
//                .maxConcurrentCalls(8)
//                .build();
//        BulkheadRegistry registry = BulkheadRegistry.of(config);
//        io.github.resilience4j.bulkhead.Bulkhead bulkhead = registry.bulkhead("IngredientService");
//
//        MeterRegistry meterRegistry = new SimpleMeterRegistry();
//        TaggedBulkheadMetrics.ofBulkheadRegistry(registry).bindTo(meterRegistry);
//
//        bulkhead.getEventPublisher().onCallPermitted(e -> printMetricDetails(meterRegistry));
//        bulkhead.getEventPublisher().onCallRejected(e -> printMetricDetails(meterRegistry));
//        bulkhead.getEventPublisher().onCallFinished(e -> printMetricDetails(meterRegistry));
//
//
//        Supplier<List<Ingredient>> ingredientsSupplier = () -> ingredientService.getIngredientsTakingRandomTime();
//        Supplier<List<Ingredient>> decoratedIngredientsSupplier = io.github.resilience4j.bulkhead.Bulkhead.decorateSupplier(bulkhead, ingredientsSupplier);
//
//        for (int i=0; i<5; i++) {
//            CompletableFuture.supplyAsync(decoratedIngredientsSupplier)
//                    .whenComplete( (r, t) -> {
//                        if (r != null) {
//                            System.out.println("Received results");
//                        }
//                    });
//        }
//        return decoratedIngredientsSupplier.get();
//    }

    void printMetricDetails(MeterRegistry meterRegistry) {
        Consumer<Meter> meterConsumer = meter -> {
            String desc = meter.getId().getDescription();
            String metricName = meter.getId().getName();
            Double metricValue = StreamSupport.stream(meter.measure().spliterator(), false)
                    .filter(m -> m.getStatistic().name().equals("VALUE"))
                    .findFirst()
                    .map(m -> m.getValue())
                    .orElse(0.0);
            System.out.println(desc + " - " + metricName + ": " + metricValue);
        };
        meterRegistry.forEachMeter(meterConsumer);
    }

    static void delay(int seconds) {
        // sleep to simulate delay
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}

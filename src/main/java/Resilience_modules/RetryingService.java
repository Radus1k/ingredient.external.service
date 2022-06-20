package Resilience_modules;

import failures.*;
import failures.FailNTimes;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.metrics.Metrics;
//import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
//import io.micrometer.core.instrument.Meter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import model.Ingredient;
import org.hibernate.dialect.identity.Ingres9IdentityColumnSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import service.IngredientService;

import javax.persistence.criteria.CriteriaBuilder;
import javax.swing.plaf.synth.SynthTextAreaUI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class RetryingService {

    @Autowired
    private IngredientService ingredientService;

    private final String GET_INGREDIENTS_MICROSERVICE_PROPERTY = "ServiceGetIngredients"; // Used in application.yml

    private final String ADD_INGREDIENT_MICROSERVICE_PROPERTY = "ServiceAddIngredient"; // Used in application.yml

    @Retry(name = GET_INGREDIENTS_MICROSERVICE_PROPERTY, fallbackMethod = "retryGetIngredientsFallback")
    public List<Ingredient> getIngredientsTrowingException() throws Exception {
        return ingredientService.getIngredientsThrowingException();
    }

    @Retry(name = ADD_INGREDIENT_MICROSERVICE_PROPERTY, fallbackMethod = "retryAddIngredientFallback")
    public Ingredient addIngredientThrowingException(String ingredientName) throws Exception {
        return ingredientService.addIngredientThrowingException(ingredientName);
    }

    public List<Ingredient> retryGetIngredientsFallback(Exception e) {
        System.out.println("Retried to get the data, no succes :(");
        return Collections.emptyList();
    }

    public Ingredient retryAddIngredientFallback(Exception e) {
        System.out.println("Tried to add an ingredient, no succes :(");
        return null;
    }


    public List<Ingredient> defaultGetIngredientsConfigUsage() {
        // Successful API Call.  Flow: request->response
        RetryConfig config = RetryConfig.ofDefaults();
        RetryRegistry registry = RetryRegistry.of(config);
        io.github.resilience4j.retry.Retry retry = registry.retry("IngredientService", config);

        Supplier<List<Ingredient>> getIngredientSupplier = () -> ingredientService.getIngredients();

        System.out.println(getIngredientSupplier.get());

        Supplier<List<Ingredient>> retryingGetIngredients = io.github.resilience4j.retry.Retry.decorateSupplier(retry, getIngredientSupplier);

        System.out.println(retryingGetIngredients.get());

        return retryingGetIngredients.get();
    }

    public List<Ingredient> basicGetIngredientsUsage() {
        // Fail Request with basic config Flow: -> reqeust->fail -> Retrying 4 Times. 5th Success -> send response
        RetryConfig config = RetryConfig.custom().maxAttempts(5).waitDuration(Duration.of(2, SECONDS)).build();
        RetryRegistry registry = RetryRegistry.of(config);
        io.github.resilience4j.retry.Retry retry = registry.retry("IngredientService", config);

        ingredientService.setPotentialFailure(new FailNTimes(4));
        Supplier<List<Ingredient>> getIngredientSupplier = () -> ingredientService.getIngredients();

        Supplier<List<Ingredient>> retryingGetIngredients = io.github.resilience4j.retry.Retry.decorateSupplier(retry, getIngredientSupplier);
        System.out.println(retryingGetIngredients.get());
        return retryingGetIngredients.get();
    }

    public List<Ingredient> getIngredientsIntervalFunction_Exponential() {
        //Fail request, exponential retrying time. Flow: request: fail-> Retrying 5 times (2^n time wait):fail -> 6th time-> get response
        RetryConfig config = RetryConfig.custom().
                maxAttempts(6).
                intervalFunction(IntervalFunction.ofExponentialBackoff(1000, 2)).
                build();

        RetryRegistry registry = RetryRegistry.of(config);

        io.github.resilience4j.retry.Retry retry = registry.retry("IngredientService", config);

        ingredientService.setPotentialFailure(new FailNTimes(5));
        Supplier<List<Ingredient>> getIngredientSupplier = () -> ingredientService.getIngredients();

        Supplier<List<Ingredient>> retryingGetIngredients = io.github.resilience4j.retry.Retry.decorateSupplier(retry, getIngredientSupplier);
        System.out.println(retryingGetIngredients.get());
        return retryingGetIngredients.get();
    }

    public List<Ingredient> getIngredientsIntervalFunction_Random() {
        // same with random time wait instead of exponential. random within 5 seconds.
        RetryConfig config = RetryConfig.custom().
                maxAttempts(4).
                intervalFunction(IntervalFunction.ofRandomized(5000)).
                build();
        RetryRegistry registry = RetryRegistry.of(config);

        io.github.resilience4j.retry.Retry retry = registry.retry("IngredientService", config);

        ingredientService.setPotentialFailure(new FailNTimes(3));
        Supplier<List<Ingredient>> getIngredientSupplier = () -> ingredientService.getIngredients();

        Supplier<List<Ingredient>> retryingGetIngredients = io.github.resilience4j.retry.Retry.decorateSupplier(retry, getIngredientSupplier);
        System.out.println(retryingGetIngredients.get());
        return retryingGetIngredients.get();
    }

    public List<Ingredient> getIngredientsAsyncRetryExample(){
        // async threading retry.
        RetryConfig config = RetryConfig.custom().maxAttempts(5).waitDuration(Duration.of(1, SECONDS)).build();
        RetryRegistry registry = RetryRegistry.of(config);

        ingredientService.setPotentialFailure(new FailNTimes(4));
        io.github.resilience4j.retry.Retry retry = registry.retry("IngredientService", config);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Supplier<CompletionStage<List<Ingredient>>> completionStageSupplier = () -> CompletableFuture.supplyAsync(() -> ingredientService.getIngredients());
        retry.executeCompletionStage(scheduler, completionStageSupplier).thenAccept(System.out::println);

        try {
            Thread.sleep(70000); //wait for the other thread to end
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scheduler.shutdown();
        return Collections.emptyList();
    }


// Import Errors in Gradle
//    public List<Ingredient> getIngredientsRetryMetrics() {
//        RetryConfig config = RetryConfig.custom().maxAttempts(3).build();
//        RetryRegistry retryRegistry = RetryRegistry.of(config);
//        io.github.resilience4j.retry.Retry retry = retryRegistry.retry("IngredientService", config);
//
//        MeterRegistry meterRegistry = new SimpleMeterRegistry();
//        TaggedRetryMetrics.ofRetryRegistry(retryRegistry).bindTo(meterRegistry);
//
//        Supplier<List<Ingredient>> ingredients = io.github.resilience4j.retry.Retry.decorateSupplier(retry, () -> ingredientService.getIngredients());
//
//        for (int i=0; i<10; i++) {
//            ingredientService.setPotentialFailure(new FailHalfTheTime(2));
//            System.out.println(ingredients.get());
//        }
//
//        Consumer<Meter> meterConsumer = meter -> {
//            String desc = meter.getId().getDescription();
//            String metricName = meter.getId().getTag("kind");
//            Double metricValue = StreamSupport.stream(meter.measure().spliterator(), false).
//                    filter(m -> m.getStatistic().name().equals("COUNT")).
//                    findFirst().
//                    map(m -> m.getValue()).
//                    orElse(0.0);
//            System.out.println(desc + " - " + metricName + ": " + metricValue);
//        };
//        meterRegistry.forEachMeter(meterConsumer);
//        return Collections.emptyList();
//    }

}

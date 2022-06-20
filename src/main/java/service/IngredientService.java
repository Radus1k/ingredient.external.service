package service;


import exception.NotFoundException;
import failures.NoDelay;
import failures.NoFailure;
import failures.PotentialDelay;
import failures.PotentialFailure;
import model.Ingredient;
import org.springframework.stereotype.Service;
import repository.IngredientRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Random;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    PotentialFailure potentialFailure= new NoFailure();

   PotentialDelay potentialDelay = new NoDelay();

    Random random = new Random();

    public void setPotentialDelay(PotentialDelay potentialDelay) {
        this.potentialDelay = potentialDelay;
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public Ingredient getIngredient(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No ingredient with id " + id + " found"));
    }

    public List<Ingredient> getIngredients() {
        this.potentialFailure.occur();
        this.potentialDelay.occur();
        return ingredientRepository.findAll();
    }

    public List<Ingredient> getIngredientsTakingRandomTime() {
        long delay = random.nextInt(3000);
        try {
            System.out.println(delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Getting ingredients taking random time within 3 secs...");

        return ingredientRepository.findAll();
    }




    public List<Ingredient> getIngredientsTimeoutError() {
        System.out.println("Get all ingredients in timeout Error function..."
                + "current time = " + LocalDateTime.now().format(formatter) +
                "; current thread = " + Thread.currentThread().getName());
        return Collections.emptyList();
    }

    public List<Ingredient> getIngredientsfromCache(){
        List<Ingredient> ingredients = new ArrayList<>();
        Ingredient ingredient = new Ingredient(1L,"water",null);
        Ingredient ingredient1 = new Ingredient(2L, "lemon",null);
        ingredients.add(ingredient);
        ingredients.add(ingredient1);
        System.out.println("Returning ingredients results from the cache...");
        return ingredients;

    }


    public List<Ingredient> getIngredientsThrowingException() throws Exception {
        System.out.println("Get all ingredients; "
                + "current time = " + LocalDateTime.now().format(formatter) +
                "; current thread = " + Thread.currentThread().getName());

        throw new Exception("Exception when getting all ingredients");
    }

    public Ingredient addIngredient(String ingredientName) {
        Optional<Ingredient> ingredient = ingredientRepository.findByName(ingredientName);
        if (ingredient.isEmpty()) {
            return ingredientRepository.save(Ingredient.builder().name(ingredientName).build());
        }

        return ingredient.get();
    }

    public Ingredient addIngredientThrowingException(String ingredientName) throws Exception {
        System.out.println("Adding ingredient... "
                + "current time = " + LocalDateTime.now().format(formatter) +
                "; current thread = " + Thread.currentThread().getName());

        throw new Exception("Exception when adding an ingredient...");
    }

    public List<Ingredient> getIngredientsInOneSecond() {
        // used for Bulkhead example
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Getting Ingredients; "
                + "current time = " + LocalDateTime.now().format(formatter) +
                "; current thread = " + Thread.currentThread().getName());


        System.out.println("Getting Ingredients successful at " + LocalDateTime.now().format(formatter));
        return ingredientRepository.findAll();
    }



    public List<Ingredient> addIngredients(List<String> ingredientNames) {
        List<Ingredient> recipeIngredients = new ArrayList<>();
        List<Ingredient> missingIngredients = new ArrayList<>();
        List<Ingredient> existingIngredients = ingredientRepository.findAll();

        ingredientNames.forEach(ingNames -> {
            Optional<Ingredient> existingIng = existingIngredients
                    .stream()
                    .filter(ing -> ing.getName().trim().toLowerCase().equals(ingNames))
                    .findFirst();
            if (existingIng.isPresent()) {
                recipeIngredients.add(existingIng.get());
            } else {
                missingIngredients.add(Ingredient.builder().name(ingNames).build());
            }
        });

        recipeIngredients.addAll(ingredientRepository.saveAll(missingIngredients));
        return recipeIngredients;
    }

    public void setPotentialFailure(PotentialFailure potentialFailure) {
        System.out.println("Setting potential failure..");
        this.potentialFailure = potentialFailure;
    }

}

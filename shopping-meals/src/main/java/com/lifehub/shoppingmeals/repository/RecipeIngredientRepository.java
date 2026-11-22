package com.lifehub.shoppingmeals.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lifehub.shoppingmeals.model.entity.RecipeIngredient;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {

    /** The ingredients of these recipes, in list order, in one query. */
    List<RecipeIngredient> findByRecipeIdInOrderByRecipeIdAscPositionAsc(Collection<UUID> recipeIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from RecipeIngredient i where i.recipeId = :recipeId")
    /** Removes a recipe's ingredients, before they are replaced or the recipe is deleted. */
    void deleteByRecipeId(UUID recipeId);
}

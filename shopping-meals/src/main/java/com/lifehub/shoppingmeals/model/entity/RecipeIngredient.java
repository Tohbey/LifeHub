package com.lifehub.shoppingmeals.model.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.lifehub.starter.persistence.AssignedIdEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One line of a recipe's ingredient list. */
@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient extends AssignedIdEntity {

    /** The recipe it belongs to. */
    @Column(name = "recipe_id", nullable = false, updatable = false)
    private UUID recipeId;

    /** Its place in the list, from 0. */
    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    /** What it is, for example "Long grain rice". */
    @Column(name = "name", nullable = false, updatable = false)
    private String name;

    /** How much, as free text, or null. */
    @Column(name = "quantity", updatable = false)
    private String quantity;

    /** Creates one ingredient line. */
    public RecipeIngredient(UUID id, UUID recipeId, int position, String name, String quantity) {
        super(id);
        this.recipeId = recipeId;
        this.position = position;
        this.name = name;
        this.quantity = quantity;
    }
}

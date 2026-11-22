package com.lifehub.planner.model.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.lifehub.planner.model.entity.Frequency;

/**
 * How an event repeats: every {@code interval} days, weeks, months or years, in the workspace's timezone.
 *
 * @param until the last day an occurrence may start on, or null to repeat with no end
 */
public record RecurrenceDTO(@NotNull Frequency frequency, @NotNull @Min(1) @Max(99) Integer interval, LocalDate until) {
}

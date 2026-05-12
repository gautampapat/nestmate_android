package com.nestmate.app.utils.roommate

import com.nestmate.app.data.model.HabitPreference
import com.nestmate.app.data.model.RoommateProfile

data class DimensionMatch(
    val dimension: String,
    val selfValue: String,
    val otherValue: String,
    val matched: Boolean,
    val pointsAwarded: Double,
    val pointsMax: Double,
)

data class CompatibilityResult(
    val score: Int,
    val breakdown: List<DimensionMatch>,
)

object CompatibilityScorer {

    private const val W_BUDGET = 20.0
    private const val W_LOCATION = 15.0
    private const val W_ROOM = 15.0
    private const val W_FOOD = 15.0
    private const val W_SLEEP = 10.0
    private const val W_STUDY = 10.0
    private const val W_SMOKE = 2.5
    private const val W_DRINK = 2.5
    private const val W_CLEAN = 10.0

    init {
        check(W_BUDGET + W_LOCATION + W_ROOM + W_FOOD + W_SLEEP + W_STUDY + W_SMOKE + W_DRINK + W_CLEAN == 100.0)
    }

    fun score(self: RoommateProfile, other: RoommateProfile): CompatibilityResult {
        val rows = buildList {
            val overlap = maxOf(0L, minOf(self.maxBudget, other.maxBudget) - maxOf(self.minBudget, other.minBudget))
            val budgetPoints = (minOf(overlap, 5000) / 5000.0) * W_BUDGET
            add(
                DimensionMatch(
                    "Budget overlap",
                    "Rs ${self.minBudget}–${self.maxBudget}",
                    "Rs ${other.minBudget}–${other.maxBudget}",
                    matched = budgetPoints > 0.0,
                    pointsAwarded = budgetPoints,
                    pointsMax = W_BUDGET,
                )
            )

            val locSelf = self.preferredLocation.trim()
            val locOther = other.preferredLocation.trim()
            val locPoints = when {
                locSelf.isBlank() || locOther.isBlank() -> 0.0
                locSelf.equals(locOther, ignoreCase = true) -> W_LOCATION
                locSelf.contains(locOther, ignoreCase = true) || locOther.contains(locSelf, ignoreCase = true) -> W_LOCATION / 2.0
                levenshtein(locSelf.lowercase(), locOther.lowercase()) <= 3 -> W_LOCATION / 2.0
                else -> 0.0
            }
            add(
                DimensionMatch(
                    "Location",
                    self.preferredLocation.ifBlank { "–" },
                    other.preferredLocation.ifBlank { "–" },
                    matched = locPoints > 0.0,
                    pointsAwarded = locPoints,
                    pointsMax = W_LOCATION,
                )
            )

            add(
                dimension(
                    "Room type",
                    self.roomTypePreference.label,
                    other.roomTypePreference.label,
                    matched = self.roomTypePreference == other.roomTypePreference,
                    max = W_ROOM,
                )
            )
            add(
                dimension(
                    "Food",
                    self.foodPreference.label,
                    other.foodPreference.label,
                    matched = self.foodPreference == other.foodPreference,
                    max = W_FOOD,
                )
            )
            add(
                dimension(
                    "Sleep schedule",
                    self.sleepingSchedule.label,
                    other.sleepingSchedule.label,
                    matched = self.sleepingSchedule == other.sleepingSchedule,
                    max = W_SLEEP,
                )
            )
            add(
                dimension(
                    "Study habits",
                    self.studyHabits.label,
                    other.studyHabits.label,
                    matched = self.studyHabits == other.studyHabits,
                    max = W_STUDY,
                )
            )
            add(
                dimension(
                    "Smoking",
                    self.smokingHabit.label,
                    other.smokingHabit.label,
                    matched = habitsCompatible(self.smokingHabit, other.smokingHabit),
                    max = W_SMOKE,
                )
            )
            add(
                dimension(
                    "Drinking",
                    self.drinkingHabit.label,
                    other.drinkingHabit.label,
                    matched = habitsCompatible(self.drinkingHabit, other.drinkingHabit),
                    max = W_DRINK,
                )
            )
            
            val cleanDiff = kotlin.math.abs(self.cleanlinessLevel - other.cleanlinessLevel)
            val cleanPoints = (1.0 - cleanDiff / 4.0) * W_CLEAN
            add(
                DimensionMatch(
                    "Cleanliness",
                    "${self.cleanlinessLevel}/5",
                    "${other.cleanlinessLevel}/5",
                    matched = cleanPoints > 0.0,
                    pointsAwarded = cleanPoints,
                    pointsMax = W_CLEAN,
                )
            )
        }
        val total = rows.sumOf { it.pointsAwarded }
        return CompatibilityResult(
            score = total.toInt().coerceIn(0, 100),
            breakdown = rows,
        )
    }

    private fun dimension(
        dimension: String,
        self: String,
        other: String,
        matched: Boolean,
        max: Double,
    ): DimensionMatch = DimensionMatch(
        dimension = dimension,
        selfValue = self,
        otherValue = other,
        matched = matched,
        pointsAwarded = if (matched) max else 0.0,
        pointsMax = max,
    )

    private fun habitsCompatible(a: HabitPreference, b: HabitPreference): Boolean {
        val yesNoConflict = (a == HabitPreference.YES && b == HabitPreference.NO) ||
            (a == HabitPreference.NO && b == HabitPreference.YES)
        if (yesNoConflict) return false
        return true
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}

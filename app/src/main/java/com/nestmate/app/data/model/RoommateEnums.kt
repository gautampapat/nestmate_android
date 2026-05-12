package com.nestmate.app.data.model

enum class Gender(val label: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
}

enum class RoomType(val label: String) {
    SHARED_ROOM("Shared room"),
    PRIVATE_ROOM("Private room"),
    FULL_FLAT("Full flat"),
}

enum class SleepSchedule(val label: String) {
    EARLY_BIRD("Early bird"),
    NIGHT_OWL("Night owl"),
}

enum class StudyHabit(val label: String) {
    QUIET_STUDIER("Quiet studier"),
    SOCIAL("Social"),
}

enum class FoodPreference(val label: String) {
    VEG("Veg"),
    NON_VEG("Non-veg"),
}

enum class HabitPreference(val label: String) {
    YES("Yes"),
    NO("No"),
    OKAY_WITH_IT("Okay with it"),
}

enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED,
}

enum class GroupStatus {
    OPEN,
    FULL,
    CLOSED,
}

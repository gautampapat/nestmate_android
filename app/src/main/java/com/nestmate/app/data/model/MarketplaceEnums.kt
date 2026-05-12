package com.nestmate.app.data.model

enum class ItemCondition(val label: String) {
    NEW("New"),
    USED("Used"),
}

enum class ItemCategory(val label: String) {
    FURNITURE("Furniture"),
    ELECTRONICS("Electronics"),
    BOOKS("Books"),
    APPLIANCES("Appliances"),
    CLOTHING("Clothing"),
    CYCLES_AND_VEHICLES("Cycles & Vehicles"),
    MISCELLANEOUS("Miscellaneous"),
}

enum class ItemTag(val label: String) {
    MOVING_OUT_SALE("Moving Out Sale"),
    URGENT_SALE("Urgent Sale"),
}

enum class ItemStatus {
    ACTIVE,
    SOLD,
}

enum class WantedStatus {
    OPEN,
    FULFILLED,
}

enum class ChatContextType {
    ITEM,
    WANTED,
}

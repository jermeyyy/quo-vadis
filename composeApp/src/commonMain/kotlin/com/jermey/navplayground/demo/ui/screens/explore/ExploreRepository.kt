package com.jermey.navplayground.demo.ui.screens.explore

import org.koin.core.annotation.Factory

/**
 * Repository for explore items data.
 * 
 * This is a simple in-memory repository for demo purposes.
 * Data is generated once and cached for the lifetime of the repository.
 * 
 * In a real app, this would:
 * - Call network APIs
 * - Cache data locally
 * - Handle pagination
 */
@Factory
class ExploreRepository {

    // Cache generated items to avoid regenerating on each call
    private val cachedItems: List<ExploreItem> by lazy { generateItems() }

    /**
     * Get all explore items.
     */
    fun getItems(): List<ExploreItem> = cachedItems

    /**
     * Get a single explore item by ID.
     */
    fun getItemById(itemId: String): ExploreItem? {
        return cachedItems.find { it.id == itemId }
    }

    /**
     * Get all available categories.
     */
    fun getCategories(): List<ExploreCategory> = ExploreCategory.entries

    /**
     * Generate the full list of sample explore items.
     */
    private fun generateItems(): List<ExploreItem> = listOf(
        // Technology items
        ExploreItem(
            id = "tech_1",
            title = "AI Revolution",
            subtitle = "How artificial intelligence is transforming industries",
            category = ExploreCategory.TECHNOLOGY,
            imageUrl = ExploreItem.imageUrlForId("tech_1"),
            rating = 4.8f,
            isFeatured = true
        ),
        ExploreItem(
            id = "tech_2",
            title = "Quantum Computing",
            subtitle = "The next frontier in computational power",
            category = ExploreCategory.TECHNOLOGY,
            imageUrl = ExploreItem.imageUrlForId("tech_2"),
            rating = 4.5f
        ),
        ExploreItem(
            id = "tech_3",
            title = "Blockchain Basics",
            subtitle = "Understanding decentralized technology",
            category = ExploreCategory.TECHNOLOGY,
            imageUrl = ExploreItem.imageUrlForId("tech_3"),
            rating = 4.2f
        ),
        ExploreItem(
            id = "tech_4",
            title = "Kotlin Multiplatform",
            subtitle = "Cross-platform development made easy",
            category = ExploreCategory.TECHNOLOGY,
            imageUrl = ExploreItem.imageUrlForId("tech_4"),
            rating = 4.9f,
            isFeatured = true
        ),
        ExploreItem(
            id = "tech_5",
            title = "Cloud Architecture",
            subtitle = "Building scalable cloud solutions",
            category = ExploreCategory.TECHNOLOGY,
            imageUrl = ExploreItem.imageUrlForId("tech_5"),
            rating = 4.3f
        ),

        // Design items
        ExploreItem(
            id = "design_1",
            title = "Material Design 3",
            subtitle = "The evolution of Google's design system",
            category = ExploreCategory.DESIGN,
            imageUrl = ExploreItem.imageUrlForId("design_1"),
            rating = 4.7f,
            isFeatured = true
        ),
        ExploreItem(
            id = "design_2",
            title = "Color Theory",
            subtitle = "Understanding color psychology in UI",
            category = ExploreCategory.DESIGN,
            imageUrl = ExploreItem.imageUrlForId("design_2"),
            rating = 4.4f
        ),
        ExploreItem(
            id = "design_3",
            title = "Typography Essentials",
            subtitle = "Choosing the right fonts for your app",
            category = ExploreCategory.DESIGN,
            imageUrl = ExploreItem.imageUrlForId("design_3"),
            rating = 4.1f
        ),
        ExploreItem(
            id = "design_4",
            title = "Glassmorphism Trends",
            subtitle = "Modern frosted glass UI patterns",
            category = ExploreCategory.DESIGN,
            imageUrl = ExploreItem.imageUrlForId("design_4"),
            rating = 4.6f
        ),

        // Travel items
        ExploreItem(
            id = "travel_1",
            title = "Hidden Gems of Japan",
            subtitle = "Discover off-the-beaten-path destinations",
            category = ExploreCategory.TRAVEL,
            imageUrl = ExploreItem.imageUrlForId("travel_1"),
            rating = 4.9f,
            isFeatured = true
        ),
        ExploreItem(
            id = "travel_2",
            title = "European Capitals",
            subtitle = "A guide to the best cities in Europe",
            category = ExploreCategory.TRAVEL,
            imageUrl = ExploreItem.imageUrlForId("travel_2"),
            rating = 4.5f
        ),
        ExploreItem(
            id = "travel_3",
            title = "Tropical Paradise",
            subtitle = "Best beaches for your next vacation",
            category = ExploreCategory.TRAVEL,
            imageUrl = ExploreItem.imageUrlForId("travel_3"),
            rating = 4.7f
        ),
        ExploreItem(
            id = "travel_4",
            title = "Mountain Adventures",
            subtitle = "Hiking trails around the world",
            category = ExploreCategory.TRAVEL,
            imageUrl = ExploreItem.imageUrlForId("travel_4"),
            rating = 4.3f
        ),
        ExploreItem(
            id = "travel_5",
            title = "Safari Experience",
            subtitle = "Wildlife adventures in Africa",
            category = ExploreCategory.TRAVEL,
            imageUrl = ExploreItem.imageUrlForId("travel_5"),
            rating = 4.8f
        ),

        // Food items
        ExploreItem(
            id = "food_1",
            title = "Italian Cuisine",
            subtitle = "Authentic recipes from Italy",
            category = ExploreCategory.FOOD,
            imageUrl = ExploreItem.imageUrlForId("food_1"),
            rating = 4.8f,
            isFeatured = true
        ),
        ExploreItem(
            id = "food_2",
            title = "Sushi Mastery",
            subtitle = "The art of Japanese sushi",
            category = ExploreCategory.FOOD,
            imageUrl = ExploreItem.imageUrlForId("food_2"),
            rating = 4.6f
        ),
        ExploreItem(
            id = "food_3",
            title = "Street Food Guide",
            subtitle = "Best street food around the world",
            category = ExploreCategory.FOOD,
            imageUrl = ExploreItem.imageUrlForId("food_3"),
            rating = 4.4f
        ),
        ExploreItem(
            id = "food_4",
            title = "Vegan Delights",
            subtitle = "Delicious plant-based recipes",
            category = ExploreCategory.FOOD,
            imageUrl = ExploreItem.imageUrlForId("food_4"),
            rating = 4.2f
        ),
        ExploreItem(
            id = "food_5",
            title = "Dessert Paradise",
            subtitle = "Sweet treats from every continent",
            category = ExploreCategory.FOOD,
            imageUrl = ExploreItem.imageUrlForId("food_5"),
            rating = 4.7f
        ),

        // Nature items
        ExploreItem(
            id = "nature_1",
            title = "Northern Lights",
            subtitle = "Best places to see the aurora",
            category = ExploreCategory.NATURE,
            imageUrl = ExploreItem.imageUrlForId("nature_1"),
            rating = 4.9f,
            isFeatured = true
        ),
        ExploreItem(
            id = "nature_2",
            title = "Rainforest Wonders",
            subtitle = "Exploring tropical ecosystems",
            category = ExploreCategory.NATURE,
            imageUrl = ExploreItem.imageUrlForId("nature_2"),
            rating = 4.5f
        ),
        ExploreItem(
            id = "nature_3",
            title = "Ocean Depths",
            subtitle = "Mysteries of the deep sea",
            category = ExploreCategory.NATURE,
            imageUrl = ExploreItem.imageUrlForId("nature_3"),
            rating = 4.6f
        ),
        ExploreItem(
            id = "nature_4",
            title = "Desert Landscapes",
            subtitle = "Beauty in arid environments",
            category = ExploreCategory.NATURE,
            imageUrl = ExploreItem.imageUrlForId("nature_4"),
            rating = 4.3f
        ),

        // Music items
        ExploreItem(
            id = "music_1",
            title = "Jazz Legends",
            subtitle = "The greatest jazz musicians of all time",
            category = ExploreCategory.MUSIC,
            imageUrl = ExploreItem.imageUrlForId("music_1"),
            rating = 4.7f,
            isFeatured = true
        ),
        ExploreItem(
            id = "music_2",
            title = "Electronic Evolution",
            subtitle = "The rise of electronic music",
            category = ExploreCategory.MUSIC,
            imageUrl = ExploreItem.imageUrlForId("music_2"),
            rating = 4.4f
        ),
        ExploreItem(
            id = "music_3",
            title = "Classical Masterpieces",
            subtitle = "Timeless orchestral compositions",
            category = ExploreCategory.MUSIC,
            imageUrl = ExploreItem.imageUrlForId("music_3"),
            rating = 4.8f
        ),
        ExploreItem(
            id = "music_4",
            title = "Rock Anthems",
            subtitle = "Iconic rock songs through the decades",
            category = ExploreCategory.MUSIC,
            imageUrl = ExploreItem.imageUrlForId("music_4"),
            rating = 4.5f
        ),
        ExploreItem(
            id = "music_5",
            title = "World Music",
            subtitle = "Traditional sounds from every culture",
            category = ExploreCategory.MUSIC,
            imageUrl = ExploreItem.imageUrlForId("music_5"),
            rating = 4.2f
        ),

        // Sports items
        ExploreItem(
            id = "sports_1",
            title = "Football Legends",
            subtitle = "Greatest players in football history",
            category = ExploreCategory.SPORTS,
            imageUrl = ExploreItem.imageUrlForId("sports_1"),
            rating = 4.8f,
            isFeatured = true
        ),
        ExploreItem(
            id = "sports_2",
            title = "Olympic Heroes",
            subtitle = "Inspiring stories from the Olympics",
            category = ExploreCategory.SPORTS,
            imageUrl = ExploreItem.imageUrlForId("sports_2"),
            rating = 4.6f
        ),
        ExploreItem(
            id = "sports_3",
            title = "Extreme Sports",
            subtitle = "Adrenaline-pumping adventures",
            category = ExploreCategory.SPORTS,
            imageUrl = ExploreItem.imageUrlForId("sports_3"),
            rating = 4.4f
        ),
        ExploreItem(
            id = "sports_4",
            title = "Tennis Champions",
            subtitle = "Grand Slam winners and their journeys",
            category = ExploreCategory.SPORTS,
            imageUrl = ExploreItem.imageUrlForId("sports_4"),
            rating = 4.3f
        ),
        ExploreItem(
            id = "sports_5",
            title = "Basketball Dynasties",
            subtitle = "The most dominant teams in NBA history",
            category = ExploreCategory.SPORTS,
            imageUrl = ExploreItem.imageUrlForId("sports_5"),
            rating = 4.7f
        )
    )
}

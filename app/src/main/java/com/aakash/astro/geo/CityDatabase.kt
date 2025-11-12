package com.aakash.astro.geo

import java.util.Locale

data class City(val name: String, val latitude: Double, val longitude: Double)

object CityDatabase {
    val cities: List<City> = listOf(
        City("Delhi", 28.6139, 77.2090),
        City("New Delhi", 28.6139, 77.2090),
        City("Mumbai", 19.0760, 72.8777),
        City("Kolkata", 22.5726, 88.3639),
        City("Chennai", 13.0827, 80.2707),
        City("Bengaluru", 12.9716, 77.5946),
        City("Bangalore", 12.9716, 77.5946),
        City("Hyderabad", 17.3850, 78.4867),
        City("Ahmedabad", 23.0225, 72.5714),
        City("Pune", 18.5204, 73.8567),
        City("Jaipur", 26.9124, 75.7873),
        City("Surat", 21.1702, 72.8311),
        City("Lucknow", 26.8467, 80.9462),
        City("Kanpur", 26.4499, 80.3319),
        City("Nagpur", 21.1458, 79.0882),
        City("Indore", 22.7196, 75.8577),
        City("Thane", 19.2183, 72.9781),
        City("Bhopal", 23.2599, 77.4126),
        City("Visakhapatnam", 17.6868, 83.2185),
        City("Patna", 25.5941, 85.1376),
        City("Vadodara", 22.3072, 73.1812),
        City("Ghaziabad", 28.6692, 77.4538),
        City("Ludhiana", 30.9010, 75.8573),
        City("Agra", 27.1767, 78.0081),
        City("Nashik", 19.9975, 73.7898),
        City("Faridabad", 28.4089, 77.3178),
        City("Meerut", 28.9845, 77.7064),
        City("Rajkot", 22.3039, 70.8022),
        City("Varanasi", 25.3176, 82.9739),
        City("Srinagar", 34.0837, 74.7973),
        City("Aurangabad", 19.8762, 75.3433),
        City("Dhanbad", 23.7957, 86.4304),
        City("Amritsar", 31.6340, 74.8723),
        City("Navi Mumbai", 19.0330, 73.0297),
        City("Prayagraj", 25.4358, 81.8463),
        City("Ranchi", 23.3441, 85.3096),
        City("Coimbatore", 11.0168, 76.9558),
        City("Jabalpur", 23.1815, 79.9864),
        City("Gwalior", 26.2183, 78.1828),
        City("Vijayawada", 16.5062, 80.6480),
        City("Jodhpur", 26.2389, 73.0243),
        City("Madurai", 9.9252, 78.1198),
        City("Raipur", 21.2514, 81.6296),
        City("Kota", 25.2138, 75.8648),
        City("Chandigarh", 30.7333, 76.7794),
        City("Guwahati", 26.1445, 91.7362),
        City("Solapur", 17.6599, 75.9064),
        City("Hubballi", 15.3647, 75.1240),
        City("Dharwad", 15.4589, 75.0078),
        City("Bareilly", 28.3670, 79.4304),
        City("Moradabad", 28.8386, 78.7733),
        City("Mysuru", 12.2958, 76.6394),
        City("Tiruchirappalli", 10.7905, 78.7047),
        City("Jalandhar", 31.3260, 75.5762),
        City("Bhubaneswar", 20.2961, 85.8245),
        City("Salem", 11.6643, 78.1460),
        City("Warangal", 17.9689, 79.5941),
        City("Thiruvananthapuram", 8.5241, 76.9366),
        City("Dehradun", 30.3165, 78.0322),
        City("Noida", 28.5355, 77.3910),
        City("Gurugram", 28.4595, 77.0266),
        City("Puducherry", 11.9416, 79.8083),
        City("Panaji", 15.4909, 73.8278),
        City("Shillong", 25.5788, 91.8933),
        City("Aizawl", 23.7271, 92.7176),
        City("Imphal", 24.8170, 93.9368),
        City("Kohima", 25.6701, 94.1077),
        City("Itanagar", 27.0844, 93.6053),
        City("Gangtok", 27.3314, 88.6138),
        City("Port Blair", 11.6234, 92.7265)
    )

    private val cityIndex: Map<String, City> = cities.associateBy { it.name.lowercase(Locale.ROOT) }

    private val cityNames: List<String> = cities.map { it.name }

    fun names(): List<String> = cityNames

    fun findByName(name: String): City? {
        val normalized = name.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) return null
        return cityIndex[normalized]
    }
}


package com.swastik.hobby.data.dao

import androidx.room.*
import com.swastik.hobby.data.entity.Material
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY name")
    fun getAllMaterials(): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE category = :category ORDER BY name")
    fun getMaterialsByCategory(category: String): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE currentQuantity <= minQuantity")
    fun getLowStockMaterials(): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchMaterials(query: String): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getMaterialById(id: Long): Material?

    @Query("SELECT DISTINCT category FROM materials ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: Material): Long

    @Update
    suspend fun updateMaterial(material: Material)

    @Delete
    suspend fun deleteMaterial(material: Material)

    @Query("UPDATE materials SET currentQuantity = currentQuantity - :quantity WHERE id = :materialId")
    suspend fun consumeMaterial(materialId: Long, quantity: Double)

    @Query("UPDATE materials SET currentQuantity = currentQuantity + :quantity WHERE id = :materialId")
    suspend fun addMaterial(materialId: Long, quantity: Double)
}
package com.example.stylemate.repository

import android.util.Log
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitClothingCrossRef
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitItemWithPosition
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.network.ClothingItemDto
import com.example.stylemate.network.OutfitDto
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * 🏪 OUTFIT REPOSITORY — API version (replaces Room)
 * ═══════════════════════════════════════════════════════════════
 *
 * ⚡ PERFORMANCE IMPROVEMENTS (06/2026):
 *   - Added shared ObjectCache to cache clothes list for 60s
 *   - getAllOutfitsWithItems() only calls getAllClothes() once
 *   - getOutfitItemsWithPosition() uses cache instead of separate API call
 *   - Avoids N+1 API call when displaying multiple Outfit Cards
 *
 * 🔄 Backend returns OutfitDto (items already nested in clothingItems).
 *    Repository maps DTO → Entity (OutfitWithClothingItems, OutfitItemWithPosition...)
 *    to keep return types unchanged for ViewModel.
 * ───────────────────────────────────────────────────────────────
 */
class OutfitRepository(private val apiService: StylemateApiService) {

    companion object {
        private const val TAG = "OutfitRepository"
        private const val CACHE_TTL_MS = 60_000L // 60 seconds

        /**
         * Convert OutfitDto (from API) to OutfitWithClothingItems (for ViewModel).
         */
        fun OutfitDto.toOutfitWithClothingItems(): OutfitWithClothingItems {
            val outfitEntity = OutfitEntity(
                id = this.id,
                name = this.name,
                createdAt = this.createdAt
            )
            val clothingItems = this.clothingItems.map { ref ->
                ClothingItemEntity(
                    id = ref.clothingItemId,
                    imageOriginal = "",
                    imageNoBg = "",
                    category = "",
                    color = "",
                    name = "",
                    season = "",
                    occasion = "",
                    brand = "",
                    purchaseDate = 0L,
                    price = 0.0,
                    canvasPosX = ref.posX,
                    canvasPosY = ref.posY,
                    createdAt = 0L
                )
            }
            return OutfitWithClothingItems(
                outfit = outfitEntity,
                clothingItems = clothingItems
            )
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 🗃️ Shared Object Cache (thread-safe)
    // ═════════════════════════════════════════════════════════════

    private val clothesCacheMutex = Mutex()
    private var cachedAllClothes: Map<String, ClothingItemEntity>? = null
    private var clothesCacheTimestamp: Long = 0L

    /**
     * Get cached clothes list (60s TTL).
     * Uses map<String, ClothingItemEntity> for fast ID lookup.
     */
    private suspend fun getCachedClothesMap(): Map<String, ClothingItemEntity> = clothesCacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedAllClothes != null && (now - clothesCacheTimestamp) < CACHE_TTL_MS) {
            return@withLock cachedAllClothes!!
        }
        try {
            Log.d(TAG, "📦 Cache clothes miss, calling API getAllClothes()...")
            val response = apiService.getAllClothes()
            val clothesMap: Map<String, ClothingItemEntity> = if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                items.associate { dto ->
                    dto.id to ClothingItemEntity(
                        id = dto.id,
                        imageOriginal = dto.imageOriginal,
                        imageNoBg = dto.imageNoBg,
                        category = dto.category,
                        color = dto.color,
                        name = dto.name,
                        season = dto.season,
                        occasion = dto.occasion,
                        brand = dto.brand,
                        purchaseDate = dto.purchaseDate,
                        price = dto.price,
                        canvasPosX = dto.canvasPosX,
                        canvasPosY = dto.canvasPosY,
                        createdAt = dto.createdAt
                    )
                }
            } else {
                cachedAllClothes ?: emptyMap()
            }
            cachedAllClothes = clothesMap
            clothesCacheTimestamp = now
            clothesMap
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ API getAllClothes() exception: ${e.message}")
            cachedAllClothes ?: emptyMap()
        }
    }

    /**
     * Invalidate cache so next call fetches fresh data.
     */
    fun invalidateClothesCache() {
        cachedAllClothes = null
        clothesCacheTimestamp = 0L
    }

    /**
     * Map OutfitDto to OutfitWithClothingItems with full item details.
     * Uses clothes cache to avoid separate API calls per outfit.
     */
    private suspend fun OutfitDto.toFullOutfitWithItems(clothesMap: Map<String, ClothingItemEntity>): OutfitWithClothingItems {
        val outfitEntity = OutfitEntity(
            id = this.id,
            name = this.name,
            createdAt = this.createdAt
        )
        val clothingItems = this.clothingItems.mapNotNull { ref ->
            val fullItem = clothesMap[ref.clothingItemId]
            if (fullItem != null) {
                ClothingItemEntity(
                    id = fullItem.id,
                    imageOriginal = fullItem.imageOriginal,
                    imageNoBg = fullItem.imageNoBg,
                    category = fullItem.category,
                    color = fullItem.color,
                    name = fullItem.name,
                    season = fullItem.season,
                    occasion = fullItem.occasion,
                    brand = fullItem.brand,
                    purchaseDate = fullItem.purchaseDate,
                    price = fullItem.price,
                    canvasPosX = ref.posX,
                    canvasPosY = ref.posY,
                    canvasScale = ref.scale,
                    canvasRotation = ref.rotation,
                    canvasFlipX = ref.flipX,
                    createdAt = fullItem.createdAt
                )
            } else null
        }
        return OutfitWithClothingItems(
            outfit = outfitEntity,
            clothingItems = clothingItems
        )
    }

    /**
     * Map OutfitDto to OutfitItemWithPosition (including position).
     */
    private suspend fun OutfitDto.toOutfitItemsWithPosition(clothesMap: Map<String, ClothingItemEntity>): List<OutfitItemWithPosition> {
        return this.clothingItems.mapNotNull { ref ->
            val fullItem = clothesMap[ref.clothingItemId]
            if (fullItem != null) {
                OutfitItemWithPosition(
                    item = ClothingItemEntity(
                        id = fullItem.id,
                        imageOriginal = fullItem.imageOriginal,
                        imageNoBg = fullItem.imageNoBg,
                        category = fullItem.category,
                        color = fullItem.color,
                        name = fullItem.name,
                        season = fullItem.season,
                        occasion = fullItem.occasion,
                        brand = fullItem.brand,
                        purchaseDate = fullItem.purchaseDate,
                        price = fullItem.price,
                        canvasPosX = ref.posX,
                        canvasPosY = ref.posY,
                        createdAt = fullItem.createdAt
                    ),
                    posX = ref.posX,
                    posY = ref.posY,
                    scale = ref.scale
                )
            } else null
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 📋 Read — Reactive with Flow
    // ═════════════════════════════════════════════════════════════

    /**
     * Get all Outfits (with full ClothingItem details) as Flow.
     * 
     * ⚡ Optimized: Only calls API getAllClothes() once shared for all outfits.
     * Uses 60s cache to avoid refetching when switching tabs.
     */
    fun getAllOutfitsWithItems(nameQuery: String? = null): Flow<List<OutfitWithClothingItems>> = flow {
        try {
            val outfitsResponse = apiService.getAllOutfits(
                name = nameQuery?.trim()?.takeIf { it.isNotBlank() }
            )

            if (outfitsResponse.isSuccessful) {
                val outfits = outfitsResponse.body()?.data ?: emptyList()
                if (outfits.isEmpty()) {
                    emit(emptyList())
                    return@flow
                }

                // ⚡ Only calls getAllClothes() once (uses 60s cache)
                val clothesMap = getCachedClothesMap()
                val result = outfits.map { outfitDto ->
                    outfitDto.toFullOutfitWithItems(clothesMap)
                }
                emit(result)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "getAllOutfitsWithItems() error: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get outfit details by ID.
     */
    suspend fun getOutfitWithItemsById(outfitId: String): OutfitWithClothingItems? =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOutfitById(outfitId)
                if (response.isSuccessful) {
                    val outfitDto = response.body()?.data ?: return@withContext null
                    val clothesMap = getCachedClothesMap()
                    outfitDto.toFullOutfitWithItems(clothesMap)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitWithItemsById() error: ${e.message}")
                null
            }
        }

    /**
     * Get list of Outfits containing a specific ClothingItem.
     */
    fun getOutfitsContainingItem(clothingItemId: String): Flow<List<OutfitWithClothingItems>> =
        flow {
            try {
                val response = apiService.getOutfitsContainingItem(clothingItemId)
                if (response.isSuccessful) {
                    val body = response.body()?.data ?: emptyList()
                    val clothesMap = getCachedClothesMap()
                    emit(body.map { it.toFullOutfitWithItems(clothesMap) })
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitsContainingItem() error: ${e.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Get items in outfit with saved positions.
     * 
     * ⚡ Optimized: Uses clothes cache instead of calling API getAllClothes() again.
     */
    suspend fun getOutfitItemsWithPosition(outfitId: String): List<OutfitItemWithPosition> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOutfitById(outfitId)
                if (response.isSuccessful) {
                    val outfitDto = response.body()?.data ?: return@withContext emptyList()
                    val clothesMap = getCachedClothesMap()
                    outfitDto.toOutfitItemsWithPosition(clothesMap)
                } else emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "getOutfitItemsWithPosition() error: ${e.message}")
                emptyList()
            }
        }

    // ═════════════════════════════════════════════════════════════
    // ➕ Write — Suspend + IO
    // ═════════════════════════════════════════════════════════════

    suspend fun insertOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            val dto = OutfitDto(
                id = outfit.id,
                name = outfit.name,
                clothingItems = emptyList(),
                createdAt = outfit.createdAt
            )
            apiService.createOutfit(dto)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ insertOutfit() error: ${e.message}")
        }
    }

    suspend fun insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>) =
        withContext(Dispatchers.IO) {
            try {
                val outfitId = crossRefs.firstOrNull()?.outfitId ?: return@withContext
                val itemsPayload = crossRefs.map { ref ->
                    mapOf(
                        "clothingItemId" to ref.clothingItemId,
                        "posX" to ref.posX,
                        "posY" to ref.posY,
                        "scale" to ref.scale,
                        "rotation" to ref.rotation,
                        "flipX" to ref.flipX
                    )
                }
                val updateMap = mapOf<String, Any>("clothingItems" to itemsPayload)
                apiService.updateOutfit(outfitId, updateMap)
                invalidateClothesCache()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ insertOutfitClothingCrossRefs() error: ${e.message}")
            }
        }

    suspend fun clearOutfitItems(outfitId: String) = withContext(Dispatchers.IO) {
        try {
            val updateMap = mapOf<String, Any>("clothingItems" to emptyList<Map<String, Any>>())
            apiService.updateOutfit(outfitId, updateMap)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ clearOutfitItems() error: ${e.message}")
        }
    }

    suspend fun deleteOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteOutfit(outfit.id)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ deleteOutfit() error: ${e.message}")
        }
    }
}
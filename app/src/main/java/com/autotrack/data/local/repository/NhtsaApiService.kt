package com.autotrack.data.local.repository

import retrofit2.http.GET
import retrofit2.http.Path

data class NhtsaMakesResponse(val Results: List<NhtsaMake>)
data class NhtsaModelsResponse(val Results: List<NhtsaModel>)
data class NhtsaMake(val MakeId: Int, val MakeName: String?)
data class NhtsaModel(val ModelId: Int, val ModelName: String?)

interface NhtsaApiService {
    @GET("vehicles/GetAllMakes?format=json")
    suspend fun getAllMakes(): NhtsaMakesResponse

    @GET("vehicles/GetModelsForMake/{make}?format=json")
    suspend fun getModelsForMake(@Path("make") make: String): NhtsaModelsResponse
}
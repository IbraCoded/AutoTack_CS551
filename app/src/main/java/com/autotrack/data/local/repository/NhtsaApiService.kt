package com.autotrack.data.local.repository

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

data class NhtsaMakesResponse(
    @SerializedName("Results") val Results: List<NhtsaMake>
)

data class NhtsaModelsResponse(
    @SerializedName("Results") val Results: List<NhtsaModel>
)

data class NhtsaMake(
    @SerializedName("Make_ID") val MakeId: Int,
    @SerializedName("Make_Name") val MakeName: String?
)

data class NhtsaModel(
    @SerializedName("Model_ID") val ModelId: Int,
    @SerializedName("Model_Name") val ModelName: String?
)

interface NhtsaApiService {
    @GET("vehicles/GetAllMakes?format=json")
    suspend fun getAllMakes(): NhtsaMakesResponse

    @GET("vehicles/GetModelsForMake/{make}?format=json")
    suspend fun getModelsForMake(@Path("make") make: String): NhtsaModelsResponse
}
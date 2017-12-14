package com.lyranetwork.demo.payapp.retrofit

import retrofit2.Call
import retrofit2.http.*


/**
 * Retrofit available services interface
 */
internal interface APIInterface {

    @FormUrlEncoded
    @POST("performInitPHP/")
    fun doGetPerformInitPHP(
            @Field("email") email: String,
            @Field("amount") amount: String
    ): Call<PerformInitPHP>
}
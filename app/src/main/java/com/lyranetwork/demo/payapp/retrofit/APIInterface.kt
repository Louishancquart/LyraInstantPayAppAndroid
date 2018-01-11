package com.lyranetwork.demo.payapp.retrofit

import retrofit2.Call
import retrofit2.http.*


/**
 * Retrofit available services interface
 */
internal interface APIInterface {

//    @FormUrlEncoded
//    @POST("performInitPHP/")
//    fun doGetPerformInitPHP(
//            @Field("order_ID") orderID: String,
//            @Field("amount") amount: String
//    ): Call<PerformInitPHP>


    @FormUrlEncoded
    @POST("raw_payment_test2.php")
    fun doGetPerformInitPHP(
            @Field("order_ID") orderID: String,
            @Field("amount") amount: String
    ): Call<PerformInitPHP>
}
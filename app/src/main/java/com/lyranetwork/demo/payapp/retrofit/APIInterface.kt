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
    @POST("raw_payment_test2.php") //for tests purpose
    fun doGetPerformInitPHP(
            @Field("order_ID") orderID: String,
            @Field("amount") amount: String
    ): Call<PerformInitPHP>



    @FormUrlEncoded
    @POST("vads-payment/entry.silentInit.a")
    fun doPostPayzenParams(
            @Field("vads_site_id") vads_site_id: String, //Merchant Shop ID
            @Field("vads_amount") vads_amount: String,
            @Field("vads_cust_email") email: String,
            @Field("vads_order_id") vads_order_id: String,
            @Field("vads_currency") vads_currency: String,
            @Field("vads_ctx_mode") vads_ctx_mode: String,
            @Field("vads_page_action") vads_page_action: String,
            @Field("vads_action_mode") vads_action_mode: String,
            @Field("vads_payment_config") vads_payment_config: String,
            @Field("vads_version") vads_version: String,
            @Field("vads_trans_date") vads_trans_date: String,
            @Field("vads_trans_id") vads_trans_id: String,
            @Field("vads_url_return") vads_url_return: String,
            @Field("vads_language") vads_language: String,
            @Field("signature") signature: String
    ): Call<PayzenParams>
}
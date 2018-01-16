package com.lyranetwork.demo.payapp.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Retrofit initialization
 */
internal object APIClient {

    val BASE_URL = "https://payzenindia-q08.lyra-labs.fr/"//"YOUR_URL" //direct Call app

//    val BASE_URL = "http://10.91.115.5/flipkart/"//"YOUR_URL"  //server call
//    val BASE_URL = "http://192.168.56.1/flipkart/"//"YOUR_URL"
//    val BASE_URL = "http://192.168.56.1:9090/"//"YOUR_URL"

    private lateinit var retrofit: Retrofit

    val client: Retrofit
        get() {

            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
            retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            return retrofit
        }

}
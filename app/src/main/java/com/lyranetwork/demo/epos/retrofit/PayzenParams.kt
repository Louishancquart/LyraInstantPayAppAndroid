package com.lyranetwork.demo.epos.retrofit


import com.google.gson.annotations.SerializedName


/**
 * Retrofit performInit response
 */
class PayzenParams {
    @SerializedName("redirect_url")
    var redirectUrl: String? = null
}
package com.lyranetwork.demo.payapp.retrofit

import com.google.gson.annotations.SerializedName


/**
 * Retrofit performInit response
 */
class PerformInitPHP {
    @SerializedName("redirect_url")
    var redirectUrl: String? = null
}
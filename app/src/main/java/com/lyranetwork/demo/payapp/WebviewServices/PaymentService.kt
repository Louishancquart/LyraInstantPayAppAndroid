package com.lyranetwork.demo.payapp.WebviewServices

import android.content.ContentValues.TAG
import android.util.Log
import android.webkit.URLUtil
import com.lyranetwork.demo.payapp.retrofit.APIClient
import com.lyranetwork.demo.payapp.retrofit.APIInterface
import com.lyranetwork.demo.payapp.retrofit.PerformInitPHP
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by asoler on 16/10/2017.
 */

class PaymentService(_orderID: String, _amount: Int) {
    var orderID = _orderID
    val amount = _amount

    private lateinit var apiInterface: APIInterface

    /**
     * Get PaymentContext
     * Call PerformInit WS (GET) with retrofit
     **/
    fun getPaymentContext(complete: (Boolean, String?) -> Unit) {
        Log.d(TAG, "orderID = " + orderID)
        Log.d(TAG, "amount = " + amount)

        if (orderID.isEmpty()) {
            orderID = "noorder"
        }

        // Init Retrofit
        apiInterface = APIClient.client.create(APIInterface::class.java)

        val call = apiInterface.doGetPerformInitPHP(orderID, amount.toString())


        call.enqueue(object : Callback<PerformInitPHP> {
            override fun onResponse(call: Call<PerformInitPHP>?, response: Response<PerformInitPHP>?) {
                Log.d("TAG", response?.code().toString() + "")

                if (response?.code() == 200) {
                    var redirectUrl = response.body()?.redirectUrl as String
                    if (URLUtil.isValidUrl(redirectUrl)) {
                        return complete(true, redirectUrl)
                    }
                } else {
                    return complete(false, null)
                }
            }

            override fun onFailure(call: Call<PerformInitPHP>?, t: Throwable?) {
                call?.cancel()
                return complete(false, null)
            }
        })
    }
}
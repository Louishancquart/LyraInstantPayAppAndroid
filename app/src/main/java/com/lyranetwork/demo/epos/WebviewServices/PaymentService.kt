package com.lyranetwork.demo.epos.WebviewServices

import android.util.Log
import android.webkit.URLUtil
import com.lyranetwork.demo.epos.retrofit.APIClient
import com.lyranetwork.demo.epos.retrofit.APIInterface
import com.lyranetwork.demo.epos.retrofit.PayzenParams
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
//import java.util.TimeZone
//import java.time.ZonedDateTime
//import java.time.format.DateTimeFormatter
import java.util.*


class PaymentService(_orderID: String, _amount: Double) {
    var orderID = _orderID
    var amount = (_amount*100).toInt() //convert to decimal as PayZen need it

    val merchantProdKey = "1365613330086542" //"1365613330086542"; 2503256198873034

    val vads_site_id = "22322173" //"22322173" 23475679
    val vads_amount = amount.toString()
    val vads_cust_email = "hancquartlouis@gmail.com" // _vads_cust_email
    val vads_order_id = orderID
    val vads_currency = "356"
    val vads_ctx_mode = "PRODUCTION"
    val vads_page_action = "PAYMENT"
    val vads_action_mode = "INTERACTIVE"
    val vads_payment_config = "SINGLE"
    val vads_version = "V2"
    var vads_trans_date:String = "" // "NOTransactionDate" //gmdate("YmdHis");
    var vads_trans_id = ""//"123213123" // gmdate("His");
    var vads_url_return = "https://payzenindia-q08.lyra-labs.fr/vads-payment/exec.cancel.a"
    val vads_language = "en"


        var signature: String = ""

    private lateinit var apiInterface: APIInterface

    /**
     * Get PaymentContext
     * Call PerformInit WS (GET) with retrofit
     **/
    fun getPaymentContext(complete: (Boolean, String?) -> Unit) {

        val dNow = Date()
        val transactionDate = SimpleDateFormat("yyyyMMddHHmmss")
        transactionDate.timeZone = TimeZone.getTimeZone("GMT")

        val transactionId = SimpleDateFormat("hhmmss")
        transactionId.timeZone = TimeZone.getTimeZone("GMT")


        vads_trans_date = transactionDate.format(dNow)
        vads_trans_id = transactionId.format(dNow)
//        vads_url_return += "\\&cacheId="+vads_site_id+vads_trans_date.substring(2)+"1"
        signature = getParamSignature()


        if (orderID.isEmpty()) {
            orderID = "noorder"
        }

        // Init Retrofit
        apiInterface = APIClient.client.create(APIInterface::class.java)

//        val call = apiInterface.doGetPerformInitPHP(orderID, amount.toString())

        val call = apiInterface.doPostPayzenParams(
                vads_site_id,
                vads_amount,
                vads_cust_email,
                vads_order_id,
                vads_currency,
                vads_ctx_mode,
                vads_page_action,
                vads_action_mode,
                vads_payment_config,
                vads_version,
                vads_trans_date,
                vads_trans_id,
                vads_url_return,
                vads_language,
                signature
        )


        call.enqueue(object : Callback<PayzenParams> {
            override fun onResponse(call: Call<PayzenParams>?, response: Response<PayzenParams>?) {
                Log.d("TAG", response?.code().toString() + "")

                if (response?.code() == 200) {
                    val redirectUrl = response.body()?.redirectUrl as String
                    if (URLUtil.isValidUrl(redirectUrl)) {
                        return complete(true, redirectUrl)
                    }
                } else {
                    return complete(false, null)
                }
            }

            override fun onFailure(call: Call<PayzenParams>?, t: Throwable?) {
                call?.cancel()
                return complete(false, null)
            }
        })
    }


    // Generate a map which represent a form
    @Throws(NoSuchAlgorithmException::class)
    fun getParamSignature(): String {
        val mapParams = TreeMap<String, String>()


        mapParams.put("vads_site_id", vads_site_id)
        mapParams.put("vads_amount", vads_amount)
        mapParams.put("vads_action_mode", vads_action_mode)
        mapParams.put("vads_ctx_mode", vads_ctx_mode)
        mapParams.put("vads_currency", vads_currency)
        mapParams.put("vads_page_action", vads_page_action)
        mapParams.put("vads_order_id", vads_order_id)
        mapParams.put("vads_cust_email", vads_cust_email)
        mapParams.put("vads_payment_config", vads_payment_config)
        mapParams.put("vads_trans_date", this@PaymentService.vads_trans_date)
        mapParams.put("vads_trans_id", this@PaymentService.vads_trans_id)
        mapParams.put("vads_version", vads_version)
        mapParams.put("vads_url_return", vads_url_return)
        mapParams.put("vads_language", vads_language)

        /** //        mapParams.put("vads_return_mode","GET");
        //        mapParams.put("vads_url_return","http://webview_"+merchantSiteId+".return");
        //        mapParams.put("vads_url_success","http://webview_"+merchantSiteId+".success");
        //        mapParams.put("vads_url_refused","http://webview_"+merchantSiteId+".refused");
        //        mapParams.put("vads_url_cancel","http://webview_"+merchantSiteId+".cancel");
        //        mapParams.put("vads_url_error","http://webview_"+merchantSiteId+".error");

        //        if (!email.equals("noemail"))
        //            mapParams.put("vads_cust_email",email);
         **/

        var concatenateMapParams = ""
        for ((key, value) in mapParams) {
            concatenateMapParams += value + "+"
            Log.d("PARAMS", "entry = " + key + "value :" + value)

        }


        concatenateMapParams += merchantProdKey

        println("concatenateMapParams: " + concatenateMapParams)
        println("signature Kotlin: " + sha1(concatenateMapParams))
        mapParams.put("signature", sha1(concatenateMapParams))

        println("signature: " + mapParams["signature"])

        return mapParams.getValue("signature")
    }

    private fun sha1(input: String) =
            MessageDigest
                    .getInstance("SHA-1")
                    .digest(input.toByteArray())
                    .map { String.format("%02X", it) }
                    .joinToString(separator = "")
}
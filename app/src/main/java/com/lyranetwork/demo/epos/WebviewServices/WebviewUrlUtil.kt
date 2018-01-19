package com.lyranetwork.demo.epos.WebviewServices

import android.util.Log
import java.io.Serializable
import java.util.*

/**
 * Parse the URL and convert it to an HashMap
 */
class WebviewUrlUtil(data: String) : Serializable {

    private val TAG = "WebviewUrlUtil"

    private val additionalData = HashMap<String, String>()

    init {
        val attributs = data.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (att in attributs) {
            val prop = att.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (prop.size == 2) {
                additionalData.put(prop[0], prop[1])
            } else if (prop.size == 1) {
                additionalData.put(prop[0], "")
            } else {
                println()
            }
        }
    }

    /**
     * Is URL contains success or return, else return false
     */
    val isSuccess: Boolean
        get() {
            val status = additionalData.get("vads_trans_status")
            Log.d(TAG, "vads_trans_status = " + status)
            return status.equals("CAPTURED")
        }


    /**
     * Get value extracted from the URL
     * @param key the parameter name
     * @return the value associated with the key
     */
    fun getParameter(key: String): String? {
        val value = additionalData.get(key)
        Log.d(TAG, "key = " + key + " and value = " + value)
        return value
    }

    /**
     * @return the HashMap Key/Value as a String
     */
    override fun toString(): String {
        return additionalData.toString()
    }
}

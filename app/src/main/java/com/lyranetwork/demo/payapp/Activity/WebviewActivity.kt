package com.lyranetwork.demo.payapp.Activity

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.webkit.*
import com.lyranetwork.demo.payapp.R
import com.lyranetwork.demo.payapp.Util.MyContextWrapper
import com.lyranetwork.demo.payapp.WebviewServices.PaymentService
import com.lyranetwork.demo.payapp.WebviewServices.WebviewUrlUtil
import kotlinx.android.synthetic.main.activity_payment.*
import java.net.URLDecoder

const val BUNDLE_RESULT = "WebviewUrlUtil"
const val KEY_EXTRA_EMAIL = "KEY_EXTRA_EMAIL"
const val KEY_EXTRA_REASON = "KEY_EXTRA_REASON"

/**
 * Display the fullscreen WebView used to make a payment
 */
class WebviewActivity : AppCompatActivity() {

    val TAG = "WebviewActivity"
    private val CALLBACK_URL_PREFIX = "http://webview"
    private val PDF_URL = "%2Fpdf"
    private val PP_URL = "payzen.eu/mentions-paiement"
    private val PP2_URL = "payzen.eu/paiement-securise"
    private val PP3_URL = "https://payzen.eu/"
    private val EXPIRATION_URL = "http://www.demo.lyra.mobile/"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Open WebviewActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Get the Intent that started this activity and extract the string
        val intent = intent
        val email = intent.getStringExtra("email")
        val amount = intent.getIntExtra("amount", 0)
        val lang = intent.getStringExtra("lang")

        // Call PaymentService to get in return payment url
        PaymentService(email, amount).getPaymentContext(
                { status: Boolean, urlPayment: String? ->
                    // Get an error, show error activity
                    if (!status) {
                        val intent = Intent(applicationContext, PaymentFailureActivity::class.java)
                        intent.putExtra(KEY_EXTRA_REASON,"NETWORK")
                        startActivity(intent, "")
                    // Fine, we get a payment url
                    } else {
                        Log.d(TAG, "open url in webview = " + urlPayment)

                        //force javascript
                        webview.getSettings().setJavaScriptEnabled(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                                WebView.setWebContentsDebuggingEnabled(true)
                            }
                        }
                        webview.setWebChromeClient(object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                                return super.onConsoleMessage(consoleMessage)
                            }
                        })
                        //Load the given URL
                        webview.loadUrl(urlPayment)

                        // Time to set custom callback
                        webview.setWebViewClient(object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("WebView", "your current url when webpage loading.. : " + url)
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                Log.d("WebView", "your current url when webpage loading.. finish : " + url)
                                loadingPanelPayment.visibility = View.GONE
                                super.onPageFinished(view, url)
                            }

                            override fun onLoadResource(view: WebView, url: String) {
                                super.onLoadResource(view, url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                println("when you click on any interlink on webview that time you got url : " + url)
                                return checkCurrentURL(view, url)
                            }

                            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                return checkCurrentURL(view, request.url.toString())
                            }
                        })
                        webview.canGoForward()

                        //Force progress bar color
                        progressBarPayment.getIndeterminateDrawable().setColorFilter(resources.getColor(R.color.bluelyradark),
                                PorterDuff.Mode.MULTIPLY)
                    }
                }
        )

    }

    override fun onResume() {
        super.onResume()
        loadingPanelPayment.visibility = View.VISIBLE
    }

    /**
     * Check if URL is the end of the payment or not
     */
    private fun checkCurrentURL(view: WebView, url: String): Boolean {
        val isCallBack = isCallbackUrl(url)
        Log.w(TAG, "Handle callback ? $isCallBack :: $url")
        // payment is finish
        if (isCallBack!!) {
            view.stopLoading()
            goToFinalActivity(url)
        // Pdf, Custom link found in page
        } else if (isUrlToOpenedSeparately(url)!!) {
            view.stopLoading()
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            openIntent.`package` = "com.android.chrome"

            try {
                startActivity(openIntent)
            } catch (e: ActivityNotFoundException) {
                openIntent.`package` = null
                startActivity(openIntent)
            }
         // User haven't finished his payment, payment platform expire his payment page
        } else if (isExpirationUrl(url)!!) {
            val intent = Intent(applicationContext, PaymentFailureActivity::class.java)
            intent.putExtra(KEY_EXTRA_REASON,"EXPIRATION")
            startActivity(intent, "")
        // Well we accept to follow url
        } else {
            view.loadUrl(url)
        }
        return (!isCallBack)
    }

    /**
     * Go to final activity depending on the success result
     */
    private fun goToFinalActivity(result: String) {
        Log.w(TAG, "Handle mUrl and redirect to MainActivity")
        val webviewUrlUtil = getResultFromUrl(result)
        Log.d(TAG, if (webviewUrlUtil == null) "" else webviewUrlUtil.toString())
        if (webviewUrlUtil?.isSuccess!!) {
            val intent = Intent(applicationContext, PaymentSuccessActivity::class.java)
            if (webviewUrlUtil.getParameter(
                    "vads_cust_email") != null && !webviewUrlUtil.getParameter(
                    "vads_cust_email")?.isEmpty()!!) {
                intent.putExtra(KEY_EXTRA_EMAIL,
                                URLDecoder.decode(webviewUrlUtil.getParameter("vads_cust_email"), "UTF-8"))
            }
            startActivity(intent, result)
        } else {
            val intent = Intent(applicationContext, PaymentFailureActivity::class.java)
            intent.putExtra(KEY_EXTRA_REASON,
                            URLDecoder.decode(webviewUrlUtil.getParameter("vads_trans_status"), "UTF-8"))
            startActivity(intent, result)
        }
    }

    /**
     * Start the activity given in parameter
     */
    private fun startActivity(intent: Intent, result: String) {
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in,
                                                                android.R.anim.fade_out)
        intent.putExtra(BUNDLE_RESULT, result)
        ActivityCompat.startActivity(this@WebviewActivity, intent, options.toBundle())
        finish()
    }

    /**
     * Convert URL to a WebviewUrlUtil object
     */
    fun getResultFromUrl(url: String): WebviewUrlUtil? {
        var myUrl = url
        if (myUrl.startsWith(CALLBACK_URL_PREFIX)) {
            myUrl = myUrl.replace(CALLBACK_URL_PREFIX, "")
            val data = myUrl.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val addData = data[1].substring(1, data[1].length)
            return WebviewUrlUtil(addData)
        }
        return null
    }

    /**
     * Is URL starts with the right prefix
     */
    fun isExpirationUrl(url: String): Boolean? {
        return url.contains(EXPIRATION_URL)
    }

    /**
     * Is URL starts with the right prefix
     */
    fun isCallbackUrl(url: String): Boolean? {
        return url.startsWith(CALLBACK_URL_PREFIX)
    }

    /**
     * Is URL contains PDF or to open in chrome
     */
    fun isUrlToOpenedSeparately(url: String): Boolean? {
        return url.contains(PDF_URL) or url.contains(PP_URL) or url.contains(PP2_URL) or url.contains(PP3_URL)
    }



    /**
     * Force selected language
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, MainActivity.LanguagesEnum.Companion.identifier(
                MainActivity.Companion.getLang(newBase))))
        MainActivity.Companion.setLanguageForApp(MainActivity.LanguagesEnum.identifier(MainActivity.getLang(newBase)),
                                                 newBase)
    }
}

This repo is intended to demonstrate how mobile payment is working; based on following components :

* Activity to collect payment information (orderID, amount) and submit payment (Casual payment & Apple Pay if enabled)
* Activity to indicate payment result
* Few class utils to perform payment workflow (communication with your backend, detect end of payment, detect expiration)

## Table of contents

* [Quick start](#quick-start)
* [Status](#status)
* [What's included](#whats-included)
* [Bugs and feature requests](#bugs-and-feature-requests)
* [Payment throught webviews](#payment-throught-webviews)

## Quick start

Several quick start options are available:

* Download [WebviewServices] & [WebviewActivity], and import it your android project.
* Clone the repo: `git clone https://github.com/payzen/Android-lyraInAppDemo.git`, and start hacking.

## Status

Tested in Android studio 3, written in Kotlin, Demo app require minSdkVersion: 19, or more.

## What's included

```swift
LyraInAppDemo
|---Activity
|   |-- MainActivity.kt
|   |-- WebviewActivity.kt
|   |-- PaymentFailureActivity.kt
|   |-- PaymentSuccessActivity.kt
|---WebviewServices
|   |-- WebviewUrlUtil.kt
|   |-- PaymentService.kt
|---Util
|   |-- Strings.java
|   |-- MyContextWrapper.kt
|---adapter
|   |-- SpinnerAdapter.kt
|---retrofit
|   |-- APIClient.kt
|   |-- APIInterface.kt
|   |-- PerformInit.kt
```

We used this app as a demo for our sales; Payment integration is separated from rest of the app.

## Bugs and feature requests

Have a bug or a feature request? [please open a new issue](https://github.com/payzen/Android-lyraInAppDemo/issues).

## General information

You will find whole documentation in official website :

## Payment throught webviews

Payment throught webview is realized by the WebviewViewActivity, and utils classes include in WebviewServices.

**WebviewServices**

*WebviewServices* is a directory containing two helpers classes.

*PaymentService* is responsible to communicate with your backend to get redirected payment url.

It offer a completion handler, to use in **WebviewViewController**, returning two values :

* Status of the request (Boolean)
* Payment url (null if status is false)

```kotlin
fun getPaymentContext(complete: (Boolean, String?) -> Unit) {
    if (orderID.isEmpty()) {
        orderID = "noemail"
    }

    // Init Retrofit
    apiInterface = APIClient.client.create(APIInterface::class.java)

    val call = apiInterface.doGetPerformInit(orderID, amount.toString(), mode, lang, card)
    call.enqueue(object : Callback<PerformInit> {
        override fun onResponse(call: Call<PerformInit>?, response: Response<PerformInit>?) {
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

        override fun onFailure(call: Call<PerformInit>?, t: Throwable?) {
            call?.cancel();
            return complete(false, null)
        }
    })
}
```

> Dont forget to modify BASE_URL located in `APIClient` according your backend


*WebviewUrlUtil* is small helper class which stored get params returned by the *payment platform* (payment status, transaction number, authorization status etc.)

```kotlin
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
        return status.equals("AUTHORISED")
    }
```

**WebviewActivity**

WebviewActivity is an activity dealing with payment workflow :

* Run a PaymentService, wait for payment Url

```kotlin
// Call PaymentService to get in return payment url
PaymentService(orderID, amount, mode, lang, card).getPaymentContext(
        { status: Boolean, urlPayment: String? ->
            // Get an error, show error activity
            if (!status) {
                val intent = Intent(applicationContext, PaymentFailureActivity::class.java)
                intent.putExtra(KEY_EXTRA_REASON,"NETWORK")
                startActivity(intent, "")
            // Fine, we get a payment url
            } else {
                Log.d(TAG, "open url in webview = " + urlPayment)
            }
        }
)
```

*Open up a WKWebview*

```kotlin
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

```

*Analyse every Url's running inside the webview to detect payment status*

```kotlin
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
```


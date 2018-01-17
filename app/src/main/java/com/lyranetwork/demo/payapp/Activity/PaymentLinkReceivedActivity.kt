package com.lyranetwork.demo.payapp.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.View
import com.lyranetwork.demo.payapp.BuildConfig
import com.lyranetwork.demo.payapp.R
import com.lyranetwork.demo.payapp.Util.MyContextWrapper
import com.lyranetwork.demo.payapp.WebviewServices.PaymentService
import com.mcxiaoke.koi.ext.getActivity
import com.mcxiaoke.koi.ext.getActivityManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.paymentlinkreceived.*

import net.glxn.qrgen.android.QRCode
import okhttp3.*
import java.io.IOException
import java.util.*

/**
 * Display the failed payment
 */
class PaymentLinkReceivedActivity : AppCompatActivity() {

    lateinit var body: String
    var payment_paid: Boolean = false //payment paid status : true: paid, false: not paid
    var paymentUrl: String =""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymentlinkreceived)


        // Get the Intent that started this activity and extract the string
        val intent = intent

        val orderID = intent.getStringExtra("orderID")
        val amount = intent.getIntExtra("amount", 0)
//        val lang = intent.getStringExtra("lang")


        amountTextView2.setText(amount.toString() + " INR")
        orderIDTextView2.setText(orderID)


        // Call PaymentService to get in return payment url
        PaymentService(orderID, amount).getPaymentContext(
                { status: Boolean, urlPayment: String? ->
                    // Get an error, show error activity
                    if (!status) {
//                        println("status: " + status.toString())
//                        Intent(applicationContext, PaymentFailureActivity::class.java)
//                                .putExtra(KEY_EXTRA_REASON, "NETWORK")
//                        startActivity(intent)

                        textViewPoweredByLinkReceived.setText("NETWORK ERROR!!!!")
                        textViewPoweredByLinkReceived.setTextColor(resources.getColor(R.color.red))
//                        goToMainActivity()

                        // Fine, we get a payment url
                    } else {

                        this@PaymentLinkReceivedActivity.paymentUrl = urlPayment!!
                        Log.d("PaymentLink", "Payment link gathered: @2nd activity = " + urlPayment)

                        /*
                       * Generate bitmap from the text provided,
                       * The QR code can be saved using other methods such as stream(), file(), to() etc.
                       * */
                        val bitmap = QRCode.from(urlPayment).withSize(layoutQRCode.height-10, layoutQRCode.height-10).bitmap()
                        imageViewQRCode.setImageBitmap(bitmap)
                        imageViewQRCode.setPadding(5,5,5,5)



                        imageViewQRCode.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(p0: View?) {

                                val sendIntent = Intent()
                                sendIntent.action = Intent.ACTION_SEND
                                sendIntent.putExtra(Intent.EXTRA_TEXT, urlPayment)
                                sendIntent.type = "text/plain"
                                startActivity(sendIntent)

//                                goToMainActivity()
                            }
                        })


//                        //payment status
//                        buttonCheckStatus.setOnClickListener(object : View.OnClickListener {
//                            override fun onClick(p0: View?) {
//
//                                if (!payment_paid) {
//                                    Log.d("Payment Status", "check status")
//                                    checkPaymentStatus(urlPayment)
//                                }
////                                goToMainActivity()
//                            }
//                        })

//                        //check for payment
//                        for(i in 1..100){
//                            if (!payment_paid) {
//                                    Thread.sleep(3000)
//                                    Log.d("Payment Status", "check status")
//                                    checkPaymentStatus(urlPayment)
//                                }
//                        }

                        //Force progress bar color
                        progressBarPaymentReceived.getIndeterminateDrawable().setColorFilter(resources.getColor(R.color.bluelyradark),
                                PorterDuff.Mode.MULTIPLY)
                        textViewPoweredByLinkReceived.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)

                        val waitingTimer = Timer()
                        waitingTimer.schedule(object : TimerTask() {
                            override fun run() {
                                runOnUiThread {
                                    Log.d("Timer", "payment paid? :"+ payment_paid)
                                    if (!payment_paid) {

                                            satusImageView.visibility = View.GONE
                                            loadingPanelPaymentReceived.visibility = View.VISIBLE
                                        checkPaymentStatus(urlPayment)

                                    }
                                }
                            }
                        }, 3000, 3000) // mention time interval after which your xml will be hit.

                    }
                }
        )


    }


    override fun onBackPressed() {
        AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(
                        "<font color='#293c7a'>" + resources.getString(R.string.areyousurecancel) + "</font>"))
                .setCancelable(false)
                .setPositiveButton(
                        Html.fromHtml("<font color='#293c7a'>" + resources.getString(R.string.yes) + "</font>"),
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                MainActivity.storeAmount("0,00", applicationContext)
                                MainActivity.storeOrderID("", applicationContext)
                                cancelPayment(paymentUrl)

                                finish()
                            }
                        })
                .setNegativeButton(
                        Html.fromHtml("<font color='#293c7a'>" + resources.getString(R.string.no) + "</font>"), null)
                .show()
    }


    @Throws(IOException::class)
    private fun checkPaymentStatus(urlPayment: String?) {

        Log.d("Payment Status", "Request Http")
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(urlPayment)
                .build()




        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                /**For further implementation, check following:
                 * https://medium.com/@macastiblancot/android-coroutines-getting-rid-of-runonuithread-and-callbacks-cleaner-thread-handling-and-more-234c0a9bd8eb
                 */

                body = response.body().string()
                this@PaymentLinkReceivedActivity.runOnUiThread({
                    loadingPanelPaymentReceived.visibility = View.GONE
                    satusImageView.visibility = View.VISIBLE
                    //check for payment
//                    satusImageView.visibility = View.VISIBLE

                    if (body.contains("PAYMENT_SUCCESS_TEMPLATE")) {
                        Log.d("Payment Status", "Contains SUCCESS")
                        titleQRTextView.setText(resources.getString(R.string.payment_success))
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.accepted, null))
                        payment_paid = true
                    } else if (body.contains("PAYMENT_REFUSED_TEMPLATE")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused, null))
                        titleQRTextView.setText(resources.getString(R.string.refused_payment))
                        payment_paid = false
                    } else if (body.contains("PAYMENT_CANCELED")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused, null))
                        titleQRTextView.setText(resources.getString(R.string.abandonned_payment))
                        payment_paid = false
                    } else {
                        Log.d("Payment Status", "NO STATUS YET")

                    }


//                    if (!payment_paid) {
//                        satusImageView.visibility = View.GONE
//                        loadingPanelPaymentReceived.visibility = View.VISIBLE
//                        Thread.sleep(3000)
//                        loadingPanelPaymentReceived.visibility = View.GONE
//                        satusImageView.visibility = View.VISIBLE
//
//                        Log.d("Payment Status", "check status")
////                            checkPaymentStatus(urlPayment)
//
//                    }

                })

            }
        })


    }

    @Throws(IOException::class)
    private fun cancelPayment(urlPayment: String?) {

        val cancelUrl: String = urlPayment!!.replace("exec.refresh.a", "exec.cancel.a")
        println("cancel URL : " + cancelUrl)

        Log.d("Payment Status", "Request Http")
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(cancelUrl)
                .build()




        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                Log.d("Payment Status", "Request NNNNNNNNOK")
            }

            override fun onResponse(call: Call, response: Response) {

                body = response.body().string()
//                println("payment cancelled: "+ body )
                Log.d("Payment Status", "Request OKKK")


            }
        })

    }

    override fun onPause() {
        super.onPause()
        MainActivity.storeAmount("0,00", applicationContext)
        MainActivity.storeOrderID("", applicationContext)
    }


    /**
     * Go to main activity
     */
    private fun goToMainActivity() {
        val refresh = Intent(this@PaymentLinkReceivedActivity, MainActivity::class.java)
        refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in, android.R.anim.fade_out)
        ActivityCompat.startActivity(this@PaymentLinkReceivedActivity, refresh, options.toBundle())
        finish()
    }

    /**
     * Force selected language
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, MainActivity.LanguagesEnum.Companion.identifier(MainActivity.Companion.getLang(newBase))))
        MainActivity.Companion.setLanguageForApp(MainActivity.LanguagesEnum.identifier(MainActivity.getLang(newBase)), newBase)
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
        ActivityCompat.startActivity(this@PaymentLinkReceivedActivity, intent, options.toBundle())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Erase values on destroy
//        MainActivity.storeAmount("0,00", applicationContext)
//        MainActivity.storeOrderID("", applicationContext)
//        Log.d("DESTROY","ondestroy called")

        if (isFinishing()) {
            //call some method
        }
    }

    override fun onStop() {
        super.onStop()
        //call some method
        MainActivity.storeAmount("0,00", applicationContext)
        MainActivity.storeOrderID("", applicationContext)
        Log.d("DESTROY", "ondestroy called")
//        cancelPayment(paymentUrl)


        if (isFinishing()) {
            //call some method
            MainActivity.storeAmount("0,00", applicationContext)
            MainActivity.storeOrderID("", applicationContext)
            Log.d("DESTROY", "ondestroy called in ISFINISHING!!!")

            if (!paymentUrl.isNullOrEmpty())
                cancelPayment(paymentUrl)
        }
    }
}
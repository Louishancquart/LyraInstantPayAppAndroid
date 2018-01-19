package com.lyranetwork.demo.epos.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.util.Log
import android.view.View
import com.lyranetwork.demo.epos.BuildConfig
import com.lyranetwork.demo.epos.R
import com.lyranetwork.demo.epos.Util.MyContextWrapper
import com.lyranetwork.demo.epos.WebviewServices.PaymentService
import kotlinx.android.synthetic.main.paymentlinkreceived.*

import net.glxn.qrgen.android.QRCode
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder
import java.util.*

/**
 * Display the failed payment
 */
class PaymentLinkReceivedActivity : AppCompatActivity() {

    lateinit var body: String
    var got_result: Boolean = false //payment paid status : true: paid, false: not paid
    var paymentUrl: String =""
    val client = OkHttpClient()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymentlinkreceived)


        // Get the Intent that started this activity and extract the string
        val intent = intent

        val orderID = intent.getStringExtra("orderID")
        val amount: Double = intent.getIntExtra("amount", 0).toDouble()/100
//        val lang = intent.getStringExtra("lang")


        amountTextView2.setText(amount.toString() + " INR")
        orderIDTextView2.setText(orderID)


        // Call PaymentService to get in return payment url
        PaymentService(orderID, amount).getPaymentContext(
                { status: Boolean, urlPayment: String? ->
                    // Get an error, show error activity
                    if (!status) {
                        this@PaymentLinkReceivedActivity.paymentUrl = "ERROR"
                        textViewPoweredByLinkReceived.setText("NETWORK ERROR!!!!")
                        textViewPoweredByLinkReceived.setTextColor(ContextCompat.getColor(applicationContext, R.color.red))

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
//                                sendIntent.putExtras(Intent.EXTRA_SUBJECT, "PayZenLink-"+orderID)
                                sendIntent.putExtra(Intent.EXTRA_SUBJECT, "PayZenLink-"+orderID)
//                                sendIntent.putExtra(Intent.EXTRA_TEXT, URLEncoder.encode(urlPayment.replace(";jsessionid=.*&cacheId=".toRegex(),"?cacheId="), "UTF-8"))
                                sendIntent.putExtra(Intent.EXTRA_TEXT,urlPayment.replace(";jsessionid=.*&cacheId=".toRegex(),"?cacheId="))
                                sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                sendIntent.setTypeAndNormalize("text/plain")
                                startActivity(Intent.createChooser(sendIntent, "Share Payment Link"))

                            }
                        })


                        //Force progress bar color
                        progressBarPaymentReceived.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(applicationContext, R.color.bluelyradark),
                                PorterDuff.Mode.MULTIPLY)
                        textViewPoweredByLinkReceived.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)

                        val waitingTimer = Timer()
                        waitingTimer.schedule(object : TimerTask() {
                            override fun run() {
                                runOnUiThread {
                                    Log.d("Timer", "payment paid? :"+ got_result)
                                    if (!got_result) {

                                            loadingPanelPaymentReceived.visibility = View.VISIBLE
                                            checkPaymentStatus(urlPayment)
                                    }else{
                                        waitingTimer.cancel()
                                        satusImageView2.setOnClickListener(object : View.OnClickListener {
                                            override fun onClick(p0: View?) {
                                                finish()
                                                goToMainActivity()
                                            }
                                        })
                                    }
                                }
                            }
                        }, 15000, 5000) // mention time interval after which your xml will be hit.

                    }
                }
        )


    }


    override fun onBackPressed() {
        if(got_result){
            goToMainActivity();
            finish()
            return
        }


        AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(
                        "<font color='#293c7a'>" + resources.getString(R.string.areyousurecancel) + "</font>" ))
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

                    //check for payment
                    if (body.contains("PAYMENT_SUCCESS_TEMPLATE")) {
                        Log.d("Payment Status", "Contains SUCCESS")
                        titleQRTextView.setText(resources.getString(R.string.payment_success))
                        satusImageView2.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.accepted_big, null))
                        imageViewQRCode.visibility = View.GONE
                        satusImageView2.visibility = View.VISIBLE
                        got_result = true
                    } else if (body.contains("PAYMENT_REFUSED_TEMPLATE")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView2.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused_big, null))
                        titleQRTextView.setText(resources.getString(R.string.refused_payment))
                        imageViewQRCode.visibility = View.GONE
                        satusImageView2.visibility = View.VISIBLE
                        got_result = true
                    } else if (body.contains("PAYMENT_CANCELED")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView2.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused_big, null))
                        titleQRTextView.setText(resources.getString(R.string.abandonned_payment))
                        got_result = false
                    } else {
                        Log.d("Payment Status", "NO STATUS YET")
                        imageViewQRCode.visibility = View.VISIBLE
                    }

                })

            }
        })
    }

    @Throws(IOException::class)
    private fun cancelPayment(urlPayment: String?) {

        if( urlPayment == "ERROR" ){
            return
        }
//https://payzenindia-q08.lyra-labs.fr:443/vads-payment/exec.refresh.a;jsessionid=4AE45f9c5c38DDbEAD3EacbA.?RemoteId=aa3e08be395e4ebdb9a3b2bea2885fdd&cacheId=223221731801190142361
        val cancelUrl: String = urlPayment!!.replace("exec.refresh.a", "exec.cancel.a")
        println("cancel URL : " + cancelUrl)

        Log.d("Payment Status", "Request Http")
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


//    /**
//     * Start the activity given in parameter
//     */
//    private fun startActivity(intent: Intent, result: String) {
//        intent.addFlags(
//                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
//        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in,
//                android.R.anim.fade_out)
//        intent.putExtra(BUNDLE_RESULT, result)
//        ActivityCompat.startActivity(this@PaymentLinkReceivedActivity, intent, options.toBundle())
//        finish()
//    }

    override fun onDestroy() {
        super.onDestroy()

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



        if (isFinishing()) {
            //call some method
            MainActivity.storeAmount("0,00", applicationContext)
            MainActivity.storeOrderID("", applicationContext)
            Log.d("DESTROY", "ondestroy called in ISFINISHING!!!")

            if (!paymentUrl.isEmpty())
                cancelPayment(paymentUrl)
        }
    }
}
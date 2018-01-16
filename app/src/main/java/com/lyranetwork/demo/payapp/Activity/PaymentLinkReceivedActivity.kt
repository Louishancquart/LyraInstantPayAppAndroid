package com.lyranetwork.demo.payapp.Activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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

/**
 * Display the failed payment
 */
class PaymentLinkReceivedActivity : AppCompatActivity() {

    lateinit var body: String
    var payment_paid: Boolean = false //payment paid status : true: paid, false: not paid
    lateinit var paymentUrl:String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymentlinkreceived)


        // Get the Intent that started this activity and extract the string
        val intent = intent

        val orderID = intent.getStringExtra("orderID")
        val amount = intent.getIntExtra("amount", 0)
//        val lang = intent.getStringExtra("lang")


        amountTextView2.setText( amount.toString() + " INR")
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

                        // Fine, we get a payment url
                    } else {

                        this@PaymentLinkReceivedActivity.paymentUrl = urlPayment!!
                        Log.d("PaymentLink", "Payment link gathered: @2nd activity = " + urlPayment)

                        /*
                       * Generate bitmap from the text provided,
                       * The QR code can be saved using other methods such as stream(), file(), to() etc.
                       * */
                        val bitmap = QRCode.from(urlPayment).bitmap()
                        imageViewQRCode.setImageBitmap(bitmap)



                        buttonShareLink.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(p0: View?) {

                                val sendIntent = Intent()
                                sendIntent.action = Intent.ACTION_SEND
                                sendIntent.putExtra(Intent.EXTRA_TEXT, urlPayment)
                                sendIntent.type = "text/plain"
                                startActivity(sendIntent)

//                                goToMainActivity()
                            }
                        })


                        //payment status
                        buttonCheckStatus.setOnClickListener(object : View.OnClickListener {
                            override fun onClick(p0: View?) {

                                if (!payment_paid) {
                                    Log.d("Payment Status", "check status")
                                    checkPaymentStatus(urlPayment)
                                }
//                                goToMainActivity()
                            }
                        })


                        textViewPoweredByLinkReceived.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)

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
                                MainActivity.storeAmount("0,00",applicationContext)
                                MainActivity.storeOrderID("",applicationContext)
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

                body = response.body().string()
                this@PaymentLinkReceivedActivity.runOnUiThread({
                    satusImageView.visibility = View.VISIBLE

                    if (body.contains("PAYMENT_SUCCESS_TEMPLATE")) {
                        Log.d("Payment Status", "Contains SUCCESS")
                        buttonCheckStatus.setText(resources.getString(R.string.payment_success))
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.accepted, null))
                        payment_paid = true
                    } else if (body.contains("PAYMENT_REFUSED_TEMPLATE")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused, null))
                        buttonCheckStatus.setText(resources.getString(R.string.refused_payment))
                        payment_paid = false
                    }
                    else if (body.contains("PAYMENT_CANCELED")) {
                        Log.d("Payment Status", "Contains REFUSE")
                        satusImageView.setImageDrawable(ResourcesCompat.getDrawable(resources,
                                R.drawable.refused, null))
                        buttonCheckStatus.setText(resources.getString(R.string.abandonned_payment))
                        payment_paid = false
                    } else {
                        Log.d("Payment Status", "NO STATUS YET")
                    }

                })

            }
        })

    }

    @Throws(IOException::class)
    private fun cancelPayment(urlPayment: String?) {

//        https://payzenindia-q08.lyra-labs.fr:443/vads-payment/exec.refresh.a;jsessionid=A82dFEcAB6caFDc2Cb4E3e24.?RemoteId=1437816e6f1c4138baf0219ffc8c92d9&cacheId=223221731801151234221
        val cancelUrl: String = urlPayment!!.replace("exec.refresh.a","exec.cancel.a")
        println("cancel URL : "+ cancelUrl)

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
                println("payment cancelled: "+ body )
                Log.d("Payment Status", "Request OKKK")


            }
        })

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
        MainActivity.storeAmount("0,00", applicationContext)
        MainActivity.storeOrderID("", applicationContext)
Log.d("DESTROY","ondestroy called")

        if (isFinishing()) {
            //call some method
            cancelPayment(paymentUrl)
        }
    }
}
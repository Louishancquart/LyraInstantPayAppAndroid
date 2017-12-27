package com.lyranetwork.demo.payapp.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.lyranetwork.demo.payapp.BuildConfig
import com.lyranetwork.demo.payapp.R
import com.lyranetwork.demo.payapp.Util.MyContextWrapper
import com.lyranetwork.demo.payapp.WebviewServices.PaymentService
import kotlinx.android.synthetic.main.paymentfailed.*
import kotlinx.android.synthetic.main.paymentlinkreceived.*

/**
 * Display the failed payment
 */
class PaymentLinkReceivedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymentlinkreceived)

        // Get the Intent that started this activity and extract the string
        val intent = intent
        val orderID = intent.getStringExtra("orderID")
        val amount = intent.getIntExtra("amount", 0)
        val lang = intent.getStringExtra("lang")


        // Call PaymentService to get in return payment url
        PaymentService(orderID, amount).getPaymentContext(
                { status: Boolean, urlPayment: String? ->
                    // Get an error, show error activity
                    if (!status) {
                        val intent = Intent(applicationContext, PaymentFailureActivity::class.java)
                        intent.putExtra(KEY_EXTRA_REASON,"NETWORK")
                        startActivity(intent, "")

                        // Fine, we get a payment url
                    } else {
                        Log.d("PaymentLink", "Payment link gathered: @2nd activity = " + urlPayment)





                        buttonShareLink.setOnClickListener(object : View.OnClickListener{
                            override fun onClick(p0: View?) {
//
//                                val sendIntent = Intent()
//                                sendIntent.action = Intent.ACTION_SEND
//                                sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.")
//                                sendIntent.type = "text/plain"
//                                startActivity(sendIntent)

                                goToMainActivity()
                            }
                        })






                    }
                }
                    )


        textViewPoweredByLinkReceived.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)
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
}
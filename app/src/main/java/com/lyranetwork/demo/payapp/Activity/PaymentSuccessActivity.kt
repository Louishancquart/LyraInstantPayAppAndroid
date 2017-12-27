package com.lyranetwork.demo.payapp.Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.lyranetwork.demo.payapp.BuildConfig
import com.lyranetwork.demo.payapp.R
import com.lyranetwork.demo.payapp.Util.MyContextWrapper
import kotlinx.android.synthetic.main.paymentsuccess.*

/**
 * Display the success payment
 */
class PaymentSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymentsuccess)

        //Update success wording with the orderID stored on response
        if (getIntent().hasExtra(KEY_EXTRA_EMAIL)) {
            val email = getIntent().getStringExtra(KEY_EXTRA_EMAIL)
            textViewEmailSentTo.visibility = View.VISIBLE
            textViewEmailSentTo.setText(getString(R.string.email_confirmation, email))
        }else{
            textViewEmailSentTo.visibility = View.GONE
        }

        buttonBackToHomeSuccess.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                goToMainActivity()
            }
        })

        textViewPoweredBySuccess.setText(resources.getString(R.string.powered_by_lyra) + " v" + BuildConfig.VERSION_NAME)
    }

    /**
     * Go to main activity
     */
    private fun goToMainActivity() {
        val refresh = Intent(this@PaymentSuccessActivity, MainActivity::class.java)
        refresh.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        val options = ActivityOptionsCompat.makeCustomAnimation(applicationContext, android.R.anim.fade_in, android.R.anim.fade_out)
        ActivityCompat.startActivity(this@PaymentSuccessActivity, refresh, options.toBundle())
        finish()
    }

    /**
     * Force selected language
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase, MainActivity.LanguagesEnum.Companion.identifier(MainActivity.Companion.getLang(newBase))))
        MainActivity.Companion.setLanguageForApp(MainActivity.LanguagesEnum.identifier(MainActivity.getLang(newBase)), newBase)
    }
}